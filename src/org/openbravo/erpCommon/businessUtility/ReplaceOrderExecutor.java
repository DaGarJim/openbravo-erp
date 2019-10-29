/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.erpCommon.businessUtility;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.materialmgmt.ReservationUtils;
import org.openbravo.model.ad.access.OrderLineTax;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.order.OrderLineOffer;
import org.openbravo.model.common.order.OrderTax;
import org.openbravo.model.common.order.OrderlineServiceRelation;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.db.DbUtility;

/**
 * Process that cancels an existing order and creates another one inverse of the original. New
 * orders are created with received modifications that replace the original one.
 * 
 * This process will create a netting goods shipment to leave the original order and the inverse
 * order completely delivered, and if anything is delivered was delivered in the original order it
 * will be delivered so in the new one.
 * 
 * The same behavior of shipments will be implemented with payments.
 */
@Dependent
class ReplaceOrderExecutor extends CancelAndReplaceUtils {
  private Logger log4j = LogManager.getLogger();
  private Map<String, String> linesRelations = new HashMap<>();
  private String oldOrderId;
  private Set<String> newOrderIds;
  private String paymentOrganizationId;
  private JSONObject jsonOrder;
  private boolean useOrderDocumentNoForRelatedDocs;

  @SuppressWarnings("hiding")
  void init(String oldOrderId, Set<String> newOrderIds, String paymentOrganizationId,
      JSONObject jsonOrder, boolean useOrderDocumentNoForRelatedDocs) {
    this.oldOrderId = oldOrderId;
    this.newOrderIds = newOrderIds;
    this.paymentOrganizationId = paymentOrganizationId;
    this.jsonOrder = jsonOrder;
    this.useOrderDocumentNoForRelatedDocs = useOrderDocumentNoForRelatedDocs;
  }

  /**
   * Process that creates a replacement order in temporary status in order to Cancel and Replace an
   * original order
   * 
   * @param oldOrder
   *          Order that will be cancelled and replaced
   */
  List<Order> run() {
    return cancelAndReplaceOrder();
  }

  private List<Order> cancelAndReplaceOrder() {
    OBContext.setAdminMode(false);
    try {
      Order oldOrder = OBDal.getInstance().get(Order.class, oldOrderId);
      List<Order> newOrders = newOrderIds.stream()
          .map(newOrderId -> OBDal.getInstance().get(Order.class, newOrderId))
          .collect(Collectors.toList());
      oldOrder = lockOrder(oldOrder);

      // Added check in case Cancel and Replace button is hit more than once
      throwExceptionIfOrderIsCanceled(oldOrder);

      // Close old reservations
      closeOldReservations(oldOrder);
      oldOrder = OBDal.getInstance().get(Order.class, oldOrderId);

      Order inverseOrder = createInverseOrderAndNettingShipment();

      // Create or update the needed services relations
      updateServicesRelations();

      // Close inverse order
      closeOrder(inverseOrder);
      closeOrder(oldOrder);

      // Relate old and new orders using C_Order_Replacement table
      newOrders.stream().forEach(this::relateOldOrderAndNewOrder);

      // Complete new order and generate good shipment and sales invoice
      if (!areTriggersDisabled(jsonOrder)) {
        newOrders.stream().forEach(this::callCOrderPost);
      }

      // Only create new reservations for new orders if coming from Web POS. For backend workflow it
      // will attend to Order Line Reservation field.
      if (jsonOrder != null) {
        newOrders.stream().forEach(this::createNewReservations);
      }

      // Refresh documents
      oldOrder = OBDal.getInstance().get(Order.class, oldOrderId);
      inverseOrder = OBDal.getInstance().get(Order.class, inverseOrder.getId());

      OBDal.getInstance().flush();

      // Payment Creation only to orders with grand total different than ZERO
      // Get the payment schedule detail of the oldOrder
      final Organization paymentOrganization = OBDal.getInstance()
          .get(Organization.class, paymentOrganizationId);
      createNettingPayment(oldOrder, newOrders, inverseOrder, paymentOrganization);

      runCancelAndReplaceOrderHooks(oldOrder, inverseOrder, Optional.of(newOrders), jsonOrder);

      return newOrders;
    } catch (Exception e1) {
      Throwable e2 = DbUtility.getUnderlyingSQLException(e1);
      log4j.error("Error executing Cancel and Replace", e1);
      throw new OBException(e2.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void relateOldOrderAndNewOrder(Order newOrder) {
    // final ReplacementOrder orderReplacement =
    // OBProvider.getInstance().get(ReplacementOrder.class);
    // orderReplacement.setOrganization(oldOrder.getOrganization());
    // orderReplacement.setOrder(oldOrder);
    // orderReplacement.setReplacementOrder(newOrder);
    // OBDal.getInstance().save(orderReplacement);
    Order oldOrder = OBDal.getInstance().get(Order.class, oldOrderId);
    oldOrder.setReplacementorder(newOrder);
    OBDal.getInstance().save(oldOrder);
  }

  private void callCOrderPost(Order newOrder) {
    newOrder.setDocumentStatus("DR");
    OBDal.getInstance().save(newOrder);
    final List<Object> parameters = new ArrayList<>();
    parameters.add(null);
    parameters.add(newOrder.getId());
    final String procedureName = "c_order_post1";
    CallStoredProcedure.getInstance().call(procedureName, parameters, null, true, false);
  }

  private void callCOrderTaxAdjustment(Order order) {
    final List<Object> parameters = new ArrayList<>();
    parameters.add(order.getId());
    parameters.add(2);
    parameters.add("CO");
    final String procedureName = "C_ORDERTAX_ADJUSTMENT";
    CallStoredProcedure.getInstance().call(procedureName, parameters, null, true, false);
  }

  private Order createInverseOrder(Order oldOrder, String documentNo, boolean triggersDisabled)
      throws ParseException {
    Order inverseOrder = (Order) DalUtil.copy(oldOrder, false, true);
    // Change order values
    inverseOrder.setCreatedBy(OBContext.getOBContext().getUser());
    inverseOrder.setPosted("N");
    inverseOrder.setProcessed(false);
    inverseOrder.setDocumentStatus("DR");
    inverseOrder.setDocumentAction("CO");
    if (triggersDisabled) {
      inverseOrder.setGrandTotalAmount(oldOrder.getGrandTotalAmount().negate());
      inverseOrder.setSummedLineAmount(oldOrder.getSummedLineAmount().negate());
    } else {
      inverseOrder.setGrandTotalAmount(BigDecimal.ZERO);
      inverseOrder.setSummedLineAmount(BigDecimal.ZERO);
    }

    Date today = new Date();
    inverseOrder.setOrderDate(OBDateUtils.getDate(OBDateUtils.formatDate(today)));
    inverseOrder.setCreationDate(today);
    inverseOrder.setUpdated(today);
    inverseOrder.setScheduledDeliveryDate(today);
    String newDocumentNo = documentNo;
    if (newDocumentNo == null) {
      newDocumentNo = oldOrder.getDocumentNo() + REVERSE_PREFIX;
    }
    inverseOrder.setDocumentNo(newDocumentNo);
    inverseOrder.setCancelledorder(oldOrder);
    OBDal.getInstance().save(inverseOrder);

    // Copy old order taxes to inverse, it is done when is executed from Web POS because triggers
    // are disabled
    if (triggersDisabled) {
      createInverseOrderTaxes(oldOrder, inverseOrder);
    }

    return inverseOrder;
  }

  private Order createInverseOrderAndNettingShipment() throws ParseException, JSONException {
    Order oldOrder = OBDal.getInstance().get(Order.class, oldOrderId);

    // Get documentNo for the inverse Order Header coming from jsonOrder, if exists
    String negativeDocNo = jsonOrder != null && jsonOrder.has("negativeDocNo")
        ? jsonOrder.getString("negativeDocNo")
        : null;

    // Create inverse Order header
    Order inverseOrder = createInverseOrder(oldOrder, negativeDocNo,
        areTriggersDisabled(jsonOrder));

    // Define netting goods shipment and its lines
    ShipmentInOut nettingShipment = null;

    // Get preferences values
    boolean createNettingGoodsShipment = getCreateNettingGoodsShipmentPreferenceValue(oldOrder);
    boolean associateShipmentToNewReceipt = getAssociateGoodsShipmentToNewSalesOrderPreferenceValue(
        oldOrder);

    // Iterate old order lines
    ScrollableResults orderLines = getOrderLineList(oldOrder);
    long lineNoCounter = 1;
    long i = 0;
    while (orderLines.next()) {
      final OrderLine oldOrderLine = (OrderLine) orderLines.get(0);

      // Create inverse Order line
      final OrderLine inverseOrderLine = createInverseOrderLine(oldOrderLine, inverseOrder);

      // Netting goods shipment is created
      if (createNettingGoodsShipment) {
        // Create Netting goods shipment Header
        if (nettingShipment == null) {
          nettingShipment = createNettingGoodShipmentHeader(oldOrder);
          nettingShipment.setNettingshipment(true);
        }

        // Create Netting goods shipment Line for the old order line
        BigDecimal movementQty = oldOrderLine.getOrderedQuantity()
            .subtract(oldOrderLine.getDeliveredQuantity());
        BigDecimal oldOrderLineDeliveredQty = oldOrderLine.getDeliveredQuantity();
        oldOrderLine.setDeliveredQuantity(BigDecimal.ZERO);
        OBDal.getInstance().save(oldOrderLine);
        OBDal.getInstance().flush();
        if (movementQty.compareTo(BigDecimal.ZERO) != 0) {
          createNettingShipmentLine(nettingShipment, oldOrderLine, lineNoCounter++, movementQty);
        }
        // Create Netting goods shipment Line for the inverse order line
        movementQty = inverseOrderLine.getOrderedQuantity()
            .subtract(inverseOrderLine.getDeliveredQuantity());
        if (movementQty.compareTo(BigDecimal.ZERO) != 0) {
          createNettingShipmentLine(nettingShipment, inverseOrderLine, lineNoCounter++,
              movementQty);
        }

        // Get the the new order line that replaces the old order line, should be only one
        OrderLine newOrderLine = getReplacementOrderLine(oldOrderLine.getId());
        if (newOrderLine != null) {
          // Create Netting goods shipment Line for the new order line
          movementQty = oldOrderLineDeliveredQty;
          BigDecimal newOrderLineDeliveredQty = newOrderLine.getDeliveredQuantity();
          newOrderLine.setDeliveredQuantity(BigDecimal.ZERO);
          OBDal.getInstance().save(newOrderLine);
          OBDal.getInstance().flush();
          if (movementQty.compareTo(BigDecimal.ZERO) != 0) {
            createNettingShipmentLine(nettingShipment, newOrderLine, lineNoCounter++, movementQty);
          }
          if (newOrderLineDeliveredQty == null
              || newOrderLineDeliveredQty.compareTo(BigDecimal.ZERO) == 0) {
            // Set new order line delivered quantity to old order line ordered quantity, this
            // case coming from Backend (nothing is delivered)
            newOrderLine.setDeliveredQuantity(movementQty);
          } else {
            // Set new order line delivered quantity to previous delivery quantity, this case
            // coming from Web POS (everything is delivered)
            newOrderLine.setDeliveredQuantity(newOrderLineDeliveredQty);
          }
          OBDal.getInstance().save(newOrderLine);
        }
        // Shipment lines of original order lines are reassigned to the new order line
      } else if (associateShipmentToNewReceipt) {
        try (final ScrollableResults shipmentLines = getShipmentLineListOfOrderLine(oldOrderLine)) {
          long k = 0;
          List<ShipmentInOut> shipments = new ArrayList<>();
          List<ShipmentInOutLine> shipLines = new ArrayList<>();
          while (shipmentLines.next()) {
            ShipmentInOutLine shipLine = (ShipmentInOutLine) shipmentLines.get(0);
            // The netting shipment is flagged as unprocessed.
            ShipmentInOut shipment = shipLine.getShipmentReceipt();
            if (shipment.isProcessed().booleanValue()) {
              unprocessShipmentHeader(shipment);
              shipments.add(shipment);
            }
            // Get the the new order line that replaces the old order line, should be only one
            OrderLine newOrderLine = getReplacementOrderLine(oldOrderLine.getId());
            if (newOrderLine != null) {
              shipLine.setSalesOrderLine(newOrderLine);
              if (jsonOrder == null) {
                newOrderLine.setDeliveredQuantity(
                    newOrderLine.getDeliveredQuantity().add(shipLine.getMovementQuantity()));
                OBDal.getInstance().save(newOrderLine);
              }
              OBDal.getInstance().save(shipLine);
            }
            shipLines.add(shipLine);
            if ((++k % 100) == 0) {
              OBDal.getInstance().flush();
              for (ShipmentInOutLine shipLineToRemove : shipLines) {
                OBDal.getInstance().getSession().evict(shipLineToRemove);
              }
              shipLines.clear();
            }
          }
          OBDal.getInstance().flush();
          // The netting shipment is flagged as processed.
          for (ShipmentInOut ship : shipments) {
            OBDal.getInstance().refresh(ship);
            processShipmentHeader(ship);
          }
        }
        // Netting shipment is not created and original shipment lines are not associated to the
        // new order line. Set delivered quantity of the new order line to same as original
        // order
        // line. Do this only in backend workflow, as everything is always delivered in Web POS
      } else if (jsonOrder == null) {
        // Get the the new order line that replaces the old order line, should be only one
        OrderLine newOrderLine = getReplacementOrderLine(oldOrderLine.getId());
        if (newOrderLine != null) {
          newOrderLine.setDeliveredQuantity(oldOrderLine.getDeliveredQuantity());
        }
      }

      // Set old order delivered quantity to the ordered quantity
      oldOrderLine.setDeliveredQuantity(oldOrderLine.getOrderedQuantity());
      OBDal.getInstance().save(oldOrderLine);

      // Set inverse order delivered quantity to ordered quantity
      inverseOrderLine.setDeliveredQuantity(inverseOrderLine.getOrderedQuantity());
      OBDal.getInstance().save(inverseOrderLine);

      if ((++i % 100) == 0) {
        OBDal.getInstance().flush();
        OBDal.getInstance().getSession().clear();

        // Refresh documents
        if (nettingShipment != null) {
          nettingShipment = OBDal.getInstance().get(ShipmentInOut.class, nettingShipment.getId());
        }
        oldOrder = OBDal.getInstance().get(Order.class, oldOrderId);
        inverseOrder = OBDal.getInstance().get(Order.class, inverseOrder.getId());
      }
    }

    if (nettingShipment != null) {
      processShipmentHeader(nettingShipment);
    }

    // Adjust the taxes
    if (!areTriggersDisabled(jsonOrder)) {
      callCOrderTaxAdjustment(inverseOrder);
    }

    return inverseOrder;
  }

  private void createInverseOrderTaxes(Order oldOrder, Order inverseOrder) {
    for (OrderTax orderTax : oldOrder.getOrderTaxList()) {
      OrderTax inverseOrderTax = (OrderTax) DalUtil.copy(orderTax, false, true);
      BigDecimal inverseTaxAmount = orderTax.getTaxAmount().negate();
      BigDecimal inverseTaxableAmount = orderTax.getTaxableAmount().negate();
      inverseOrderTax.setTaxAmount(inverseTaxAmount);
      inverseOrderTax.setTaxableAmount(inverseTaxableAmount);
      inverseOrderTax.setSalesOrder(inverseOrder);
      inverseOrder.getOrderTaxList().add(inverseOrderTax);
      OBDal.getInstance().save(inverseOrderTax);
    }
    OBDal.getInstance().flush();
  }

  private OrderLine createInverseOrderLine(OrderLine oldOrderLine, Order inverseOrder) {
    OrderLine inverseOrderLine = (OrderLine) DalUtil.copy(oldOrderLine, false, true);
    inverseOrderLine.setSalesOrder(inverseOrder);
    inverseOrderLine.setOrderedQuantity(inverseOrderLine.getOrderedQuantity().negate());
    if (areTriggersDisabled(jsonOrder)) {
      inverseOrderLine.setLineGrossAmount(oldOrderLine.getLineGrossAmount().negate());
      inverseOrderLine.setLineNetAmount(oldOrderLine.getLineNetAmount().negate());
    }
    // Set inverse order delivered quantity zero
    inverseOrderLine.setDeliveredQuantity(BigDecimal.ZERO);
    inverseOrderLine.setReservedQuantity(BigDecimal.ZERO);
    inverseOrderLine.setInvoicedQuantity(BigDecimal.ZERO);

    inverseOrder.getOrderLineList().add(inverseOrderLine);
    OBDal.getInstance().save(inverseOrderLine);

    // Copy the discounts of the original line
    createInverseOrderLineDiscounts(oldOrderLine, inverseOrderLine);
    // Copy old order taxes to inverse, it is done when is executed from Web POS because triggers
    // are disabled
    if (areTriggersDisabled(jsonOrder)) {
      createInverseOrderLineTaxes(oldOrderLine, inverseOrderLine);
    }

    linesRelations.put(oldOrderLine.getId(), inverseOrderLine.getId());

    return inverseOrderLine;
  }

  private void createInverseOrderLineDiscounts(OrderLine oldOrderLine, OrderLine inverseOrderLine) {
    for (OrderLineOffer orderLineOffer : oldOrderLine.getOrderLineOfferList()) {
      final OrderLineOffer inverseOrderLineOffer = (OrderLineOffer) DalUtil.copy(orderLineOffer,
          false, true);
      inverseOrderLineOffer
          .setBaseGrossUnitPrice(inverseOrderLineOffer.getBaseGrossUnitPrice().negate());
      inverseOrderLineOffer
          .setDisplayedTotalAmount(inverseOrderLineOffer.getDisplayedTotalAmount().negate());
      inverseOrderLineOffer
          .setPriceAdjustmentAmt(inverseOrderLineOffer.getPriceAdjustmentAmt().negate());
      inverseOrderLineOffer.setTotalAmount(inverseOrderLineOffer.getTotalAmount().negate());
      inverseOrderLineOffer.setSalesOrderLine(inverseOrderLine);
      OBDal.getInstance().save(inverseOrderLineOffer);
    }
    OBDal.getInstance().flush();
  }

  private void createInverseOrderLineTaxes(OrderLine oldOrderLine, OrderLine inverseOrderLine) {
    for (OrderLineTax orderLineTax : oldOrderLine.getOrderLineTaxList()) {
      final OrderLineTax inverseOrderLineTax = (OrderLineTax) DalUtil.copy(orderLineTax, false,
          true);
      BigDecimal inverseTaxAmount = orderLineTax.getTaxAmount().negate();
      BigDecimal inverseTaxableAmount = orderLineTax.getTaxableAmount().negate();
      inverseOrderLineTax.setTaxAmount(inverseTaxAmount);
      inverseOrderLineTax.setTaxableAmount(inverseTaxableAmount);
      inverseOrderLineTax.setSalesOrder(inverseOrderLine.getSalesOrder());
      inverseOrderLineTax.setSalesOrderLine(inverseOrderLine);
      inverseOrderLine.getOrderLineTaxList().add(inverseOrderLineTax);
      inverseOrderLine.getSalesOrder().getOrderLineTaxList().add(inverseOrderLineTax);
      OBDal.getInstance().save(inverseOrderLineTax);
    }
    OBDal.getInstance().flush();
  }

  private ShipmentInOut createNettingGoodShipmentHeader(Order oldOrder) {
    ShipmentInOut nettingGoodsShipment = null;
    nettingGoodsShipment = OBProvider.getInstance().get(ShipmentInOut.class);
    nettingGoodsShipment.setOrganization(oldOrder.getOrganization());
    DocumentType goodsShipmentDocumentType = FIN_Utility.getDocumentType(oldOrder.getOrganization(),
        DOCTYPE_MatShipment);
    nettingGoodsShipment.setDocumentType(goodsShipmentDocumentType);
    nettingGoodsShipment.setWarehouse(oldOrder.getWarehouse());
    nettingGoodsShipment.setBusinessPartner(oldOrder.getBusinessPartner());
    nettingGoodsShipment.setPartnerAddress(oldOrder.getPartnerAddress());
    Date today = new Date();
    nettingGoodsShipment.setMovementDate(today);
    nettingGoodsShipment.setAccountingDate(today);
    nettingGoodsShipment.setSalesOrder(null);
    nettingGoodsShipment.setPosted("N");
    nettingGoodsShipment.setProcessed(false);
    nettingGoodsShipment.setDocumentStatus("DR");
    nettingGoodsShipment.setDocumentAction("CO");
    nettingGoodsShipment.setMovementType("C-");
    nettingGoodsShipment.setProcessGoodsJava("--");
    nettingGoodsShipment.setSalesTransaction(oldOrder.isSalesTransaction());
    String nettingGoodsShipmentDocumentNo = FIN_Utility
        .getDocumentNo(nettingGoodsShipment.getDocumentType(), ShipmentInOut.TABLE_NAME);
    nettingGoodsShipment.setDocumentNo(nettingGoodsShipmentDocumentNo);
    OBDal.getInstance().save(nettingGoodsShipment);
    return nettingGoodsShipment;
  }

  /**
   * Method that creates a netting goods shipment line for a netting shipment.
   * 
   * @param nettingGoodsShipment
   *          The header of the shipment.
   * @param orderLine
   *          OrderLine what the shipment line delivers
   * @param lineNoCounter
   *          Line number of the shipment line.
   * @param movementQty
   *          Movement quantity of the shipment line.
   * @param triggersDisabled
   *          Flag that tells if triggers are disabled or not while executing this method.
   */
  private ShipmentInOutLine createNettingShipmentLine(ShipmentInOut nettingGoodsShipment,
      OrderLine orderLine, long lineNoCounter, BigDecimal movementQty) {
    ShipmentInOutLine newGoodsShipmentLine = null;
    newGoodsShipmentLine = OBProvider.getInstance().get(ShipmentInOutLine.class);
    newGoodsShipmentLine.setOrganization(orderLine.getOrganization());
    newGoodsShipmentLine.setProduct(orderLine.getProduct());
    newGoodsShipmentLine.setUOM(orderLine.getUOM());
    // Get first storage bin
    Locator locator1 = nettingGoodsShipment.getWarehouse().getLocatorList().get(0);
    newGoodsShipmentLine.setStorageBin(locator1);
    newGoodsShipmentLine.setLineNo(10 * lineNoCounter);
    newGoodsShipmentLine.setSalesOrderLine(orderLine);
    newGoodsShipmentLine.setShipmentReceipt(nettingGoodsShipment);
    newGoodsShipmentLine.setMovementQuantity(movementQty);

    // Create Material Transaction record
    createMTransaction(newGoodsShipmentLine);

    OBDal.getInstance().save(newGoodsShipmentLine);
    return newGoodsShipmentLine;
  }

  /**
   * Method that creates the relation between products and services for the inverse order. Also, if
   * the old order has or has a relation with a deferred service, this relation must be moved to the
   * new tickets product.
   * 
   * @param oldOrder
   *          The order that has been canceled.
   * @param newOrderIdSet
   *          The orders that are replacing the old order.
   * @param jsonOrder
   *          Parameter with order information coming from Web POS.
   * @throws JSONException
   */
  private void updateServicesRelations() throws JSONException {
    final List<String> createdRelations = new ArrayList<>();
    final List<OrderlineServiceRelation> relationsToRemove = new ArrayList<>();
    final OBCriteria<OrderLine> oldOrderLineCriteria = OBDal.getInstance()
        .createCriteria(OrderLine.class);
    oldOrderLineCriteria.add(Restrictions.eq(OrderLine.PROPERTY_SALESORDER + ".id", oldOrderId));
    for (final OrderLine oldOrderLine : oldOrderLineCriteria.list()) {
      final StringBuilder where = new StringBuilder();
      where.append(
          " WHERE (" + OrderlineServiceRelation.PROPERTY_SALESORDERLINE + " = :salesorderline");
      where.append(
          " OR " + OrderlineServiceRelation.PROPERTY_ORDERLINERELATED + " = :salesorderline)");
      final OBQuery<OrderlineServiceRelation> serviceRelationQuery = OBDal.getInstance()
          .createQuery(OrderlineServiceRelation.class, where.toString());
      serviceRelationQuery.setNamedParameter("salesorderline", oldOrderLine);
      for (final OrderlineServiceRelation serviceRelation : serviceRelationQuery.list()) {
        if (!createdRelations.contains(serviceRelation.getId())) {
          createdRelations.add(serviceRelation.getId());
          if (linesRelations.containsKey(serviceRelation.getSalesOrderLine().getId())
              && linesRelations.containsKey(serviceRelation.getOrderlineRelated().getId())) {
            // Create a new relation if is not a deferred service or a product with a deferred
            // service
            final OrderlineServiceRelation inverseServiceRelation = (OrderlineServiceRelation) DalUtil
                .copy(serviceRelation, false, true);
            final OrderLine inverseServiceLine = OBDal.getInstance()
                .get(OrderLine.class,
                    linesRelations.get(serviceRelation.getSalesOrderLine().getId()));
            inverseServiceRelation.setSalesOrderLine(inverseServiceLine);
            final OrderLine inverseProductLine = OBDal.getInstance()
                .get(OrderLine.class,
                    linesRelations.get(serviceRelation.getOrderlineRelated().getId()));
            inverseServiceRelation.setOrderlineRelated(inverseProductLine);
            inverseServiceRelation.setAmount(inverseServiceRelation.getAmount().negate());
            inverseServiceRelation.setQuantity(inverseServiceRelation.getQuantity().negate());
            OBDal.getInstance().save(inverseServiceRelation);
          } else {
            // Is a deferred relation
            if (linesRelations.containsKey(serviceRelation.getOrderlineRelated().getId())) {
              // A product is being replaced, so the service relation must be removed (the new
              // relation is added in the new ticket synchronization)
              OrderLine newOrderLine = null;
              final OBCriteria<OrderLine> newOrderLineCriteria = OBDal.getInstance()
                  .createCriteria(OrderLine.class);
              if (jsonOrder != null) {
                final JSONArray lines = jsonOrder.getJSONArray("lines");
                for (int i = 0; i < lines.length(); i++) {
                  final JSONObject line = lines.getJSONObject(i);
                  if (line.has("linepos")
                      && (line.getInt("linepos") + 1) * 10 == oldOrderLine.getLineNo()) {
                    newOrderLineCriteria
                        .add(Restrictions.in(OrderLine.PROPERTY_SALESORDER + ".id", newOrderIds));
                    newOrderLineCriteria
                        .add(Restrictions.eq(OrderLine.PROPERTY_LINENO, (long) ((i + 1) * 10)));
                    newOrderLineCriteria.setMaxResults(1);
                    newOrderLine = (OrderLine) newOrderLineCriteria.uniqueResult();
                    break;
                  }
                }
              } else {
                newOrderLineCriteria
                    .add(Restrictions.eq(OrderLine.PROPERTY_REPLACEDORDERLINE, oldOrderLine));
                newOrderLineCriteria.setMaxResults(1);
                newOrderLine = (OrderLine) newOrderLineCriteria.uniqueResult();
              }
              if (newOrderLine != null) {
                // The product haven't been removed during the C&R process. The relation must be
                // moved from the original order to the new order.
                serviceRelation.setOrderlineRelated(newOrderLine);
                OBDal.getInstance().save(serviceRelation);
              } else {
                // The product have been removed during the C&R process. The service relation must
                // be removed.
                relationsToRemove.add(serviceRelation);
              }
            } else {
              // A deferred service has been replaced (or canceled), so the relation must also be
              // created for the inverse order
              final OrderLine inverseServiceLine = OBDal.getInstance()
                  .get(OrderLine.class,
                      linesRelations.get(serviceRelation.getSalesOrderLine().getId()));
              final OrderlineServiceRelation inverseServiceRelation = (OrderlineServiceRelation) DalUtil
                  .copy(serviceRelation, false, true);
              inverseServiceRelation.setSalesOrderLine(inverseServiceLine);
              inverseServiceRelation.setAmount(inverseServiceRelation.getAmount().negate());
              inverseServiceRelation.setQuantity(inverseServiceRelation.getQuantity().negate());
              OBDal.getInstance().save(inverseServiceRelation);
            }

          }
        }
      }
    }
    // Remove the services relation marked to remove
    for (final OrderlineServiceRelation serviceRelation : relationsToRemove) {
      OBDal.getInstance().remove(serviceRelation);
    }
    linesRelations.clear();
  }

  private void createNewReservations(Order newOrder) {
    if (getEnableStockReservationsPreferenceValue(newOrder.getOrganization())) {
      // Iterate old order lines
      try (final ScrollableResults newOrderLines = getOrderLineList(newOrder)) {
        int i = 0;
        while (newOrderLines.next()) {
          OrderLine newOrderLine = (OrderLine) newOrderLines.get(0);
          if (newOrderLine.getDeliveredQuantity() != null && newOrderLine.getOrderedQuantity()
              .subtract(newOrderLine.getDeliveredQuantity())
              .compareTo(BigDecimal.ZERO) == 0) {
            continue;
          }
          Reservation reservation = getReservationForOrderLine(newOrderLine.getReplacedorderline());
          if (reservation != null) {
            ReservationUtils.createReserveFromSalesOrderLine(newOrderLine, true);
          }
          if ((++i % 100) == 0) {
            OBDal.getInstance().flush();
            OBDal.getInstance().getSession().clear();
          }
        }
      } catch (Exception e) {
        log4j.error("Error in CancelAndReplaceUtils.createNewReservations", e);
        throw new OBException(e.getMessage(), e);
      }
    }
  }

  private ScrollableResults getShipmentLineListOfOrderLine(OrderLine line) {
    return OBDal.getInstance()
        .createCriteria(ShipmentInOutLine.class)
        .add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SALESORDERLINE, line))
        .setFilterOnReadableOrganization(false)
        .scroll(ScrollMode.FORWARD_ONLY);
  }

  /**
   * This method creates an M_TRANSACTION record for a given M_INOUT_LINE. This is done because
   * M_INOUT_POST is not executed for a Netting Shipment, so material transactions needs to be
   * created manually. If triggers are disabled, as it happens when the process is executed from Web
   * POS it is necessary to manually update the stock running M_UPDATE_INVENTORY stored procedure.
   * 
   * @param line
   *          Shipment Line related to the transaction.
   * @param updateStockStatement
   *          M_UPDATE_INVENTORY callable statement.
   * @param triggersDisabled
   *          Flag that tells if triggers are disabled or not while executing this method.
   */
  private void createMTransaction(ShipmentInOutLine line) {
    Product prod = line.getProduct();
    if (prod.getProductType().equals("I") && line.getProduct().isStocked().booleanValue()) {
      // Stock is changed only for stocked products of type "Item"
      MaterialTransaction transaction = OBProvider.getInstance().get(MaterialTransaction.class);
      transaction.setOrganization(line.getOrganization());
      transaction.setMovementType(line.getShipmentReceipt().getMovementType());
      transaction.setProduct(prod);
      transaction.setStorageBin(line.getStorageBin());
      transaction.setOrderUOM(line.getOrderUOM());
      transaction.setUOM(line.getUOM());
      transaction.setOrderQuantity(line.getOrderQuantity());
      transaction.setMovementQuantity(line.getMovementQuantity().multiply(NEGATIVE_ONE));
      transaction.setMovementDate(line.getShipmentReceipt().getMovementDate());
      transaction.setGoodsShipmentLine(line);
      if (line.getAttributeSetValue() != null) {
        transaction.setAttributeSetValue(line.getAttributeSetValue());
      } else if (prod.getAttributeSet() != null
          && (prod.getUseAttributeSetValueAs() == null
              || !"F".equals(prod.getUseAttributeSetValueAs()))
          && prod.getAttributeSet().isRequireAtLeastOneValue().booleanValue()) {
        // Set fake AttributeSetInstance to transaction line for netting shipment as otherwise it
        // will return an error when the product has an attribute set and
        // "Is Required at Least One Value" property of the attribute set is "Y"
        AttributeSetInstance attr = OBProvider.getInstance().get(AttributeSetInstance.class);
        attr.setAttributeSet(prod.getAttributeSet());
        attr.setDescription("1");
        OBDal.getInstance().save(attr);
        transaction.setAttributeSetValue(attr);
      }

      // Execute M_UPDATE_INVENTORY stored procedure, it is done when is executed from Web POS
      // because triggers are disabled
      if (areTriggersDisabled(jsonOrder)) {
        updateInventory(transaction);
      }

      OBDal.getInstance().save(transaction);
    }
  }

  /**
   * Method that updates the inventory based on an M_TRANSACTION record.
   * 
   * @param transaction
   *          The transaction that triggers the update of the inventory.
   * @param updateStockStatement
   *          The query to be executed.
   */
  private void updateInventory(MaterialTransaction transaction) {
    // Stock manipulation
    try (final CallableStatement updateStockStatement = new DalConnectionProvider(false)
        .getConnection()
        .prepareCall("{call M_UPDATE_INVENTORY (?,?,?,?,?,?,?,?,?,?,?,?,?)}")) {
      // client
      updateStockStatement.setString(1, OBContext.getOBContext().getCurrentClient().getId());
      // org
      updateStockStatement.setString(2, OBContext.getOBContext().getCurrentOrganization().getId());
      // user
      updateStockStatement.setString(3, OBContext.getOBContext().getUser().getId());
      // product
      updateStockStatement.setString(4, transaction.getProduct().getId());
      // locator
      updateStockStatement.setString(5, transaction.getStorageBin().getId());
      // attributesetinstance
      updateStockStatement.setString(6,
          transaction.getAttributeSetValue() != null ? transaction.getAttributeSetValue().getId()
              : null);
      // uom
      updateStockStatement.setString(7, transaction.getUOM().getId());
      // product uom
      updateStockStatement.setString(8, null);
      // p_qty
      updateStockStatement.setBigDecimal(9,
          transaction.getMovementQuantity() != null ? transaction.getMovementQuantity() : null);
      // p_qtyorder
      updateStockStatement.setBigDecimal(10,
          transaction.getOrderQuantity() != null ? transaction.getOrderQuantity() : null);
      // p_dateLastInventory --- **
      updateStockStatement.setDate(11, null);
      // p_preqty
      updateStockStatement.setBigDecimal(12, BigDecimal.ZERO);
      // p_preqtyorder
      updateStockStatement.setBigDecimal(13,
          transaction.getOrderQuantity() != null
              ? transaction.getOrderQuantity().multiply(NEGATIVE_ONE)
              : null);

      updateStockStatement.execute();

    } catch (Exception e) {
      log4j.error("Error in CancelAndReplaceUtils.updateInventory", e);
      throw new OBException(e.getMessage(), e);
    }
  }

  /**
   * Process that flags the shipment as processed and Completed. M_INOUT_POST is not used as
   * triggers are disabled.
   * 
   * @param shipment
   */
  private void processShipmentHeader(ShipmentInOut shipment) {
    shipment.setProcessed(true);
    shipment.setDocumentStatus("CO");
    shipment.setDocumentAction("--");
    OBDal.getInstance().save(shipment);
    OBDal.getInstance().flush();
  }

  /**
   * Process that flags the shipment as not processed and draft. M_INOUT_POST is not used as
   * triggers are disabled.
   * 
   * @param shipment
   */
  private void unprocessShipmentHeader(ShipmentInOut shipment) {
    shipment.setProcessed(false);
    shipment.setDocumentStatus("DR");
    shipment.setDocumentAction("CO");
    OBDal.getInstance().save(shipment);
    OBDal.getInstance().flush();
  }

  private void createNettingPayment(Order oldOrder, List<Order> newOrders, Order inverseOrder,
      Organization paymentOrganization) {
    try {
      if (oldOrder.getGrandTotalAmount().compareTo(BigDecimal.ZERO) == 0) {
        return;
      }

      final FIN_PaymentSchedule paymentSchedule = getPaymentScheduleOfOrder(oldOrder);
      if (paymentSchedule == null) {
        throw new OBException("There is no payment plan for the order: " + oldOrder.getId());
      }

      // Pay fully inverse order in C&R.
      final BigDecimal outstandingAmount = getPaymentScheduleOutstandingAmount(paymentSchedule);
      final BigDecimal paidAmount = paymentSchedule.getAmount().subtract(outstandingAmount);
      final BigDecimal negativeAmount = paymentSchedule.getAmount().negate();
      final FIN_Payment nettingPayment = payOriginalAndInverseOrder(jsonOrder, oldOrder,
          inverseOrder, paymentOrganization, outstandingAmount, negativeAmount,
          useOrderDocumentNoForRelatedDocs);

      newOrders.stream()
          .forEach(newOrder -> addNewPayments(newOrder, paymentOrganization, nettingPayment,
              paidAmount));
      processPayment(nettingPayment, jsonOrder);
    } catch (Exception e1) {
      log4j.error("Error in CancelAndReplaceUtils.createPayments", e1);
      try {
        OBDal.getInstance().getConnection().rollback();
      } catch (Exception e2) {
        throw new OBException(e2);
      }
      Throwable e3 = DbUtility.getUnderlyingSQLException(e1);
      throw new OBException(e3);
    }
  }

  private void addNewPayments(Order newOrder, Organization paymentOrganization,
      FIN_Payment nettingPayment, BigDecimal paidAmount) {
    try {
      // Only for BackEnd WorkFlow
      // Get the payment schedule of the new order to check the outstanding amount, could
      // have been automatically paid on C_ORDER_POST if is automatically invoiced and the
      // payment method of the financial account is configured as 'Automatic Receipt'
      final boolean createPayments = areTriggersDisabled(jsonOrder)
          || getPaymentScheduleOfOrder(newOrder).getOutstandingAmount()
              .compareTo(BigDecimal.ZERO) != 0;

      // Pay of the new order the amount already paid in original order
      if (createPayments && paidAmount.compareTo(BigDecimal.ZERO) != 0) {
        createOrUdpatePayment(nettingPayment, newOrder, paymentOrganization, null, paidAmount, null,
            null, null);

        String description = nettingPayment.getDescription() + ": " + newOrder.getDocumentNo();
        String truncatedDescription = (description.length() > 255)
            ? description.substring(0, 252).concat("...")
            : description;
        nettingPayment.setDescription(truncatedDescription);
      }

    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  /**
   * Method to check if a netting shipment must be generated during the C&amp;R and CL process.
   * 
   * @param order
   *          The order that is being canceled.
   * @return True if is necessary to create the netting shipment.
   */
  private boolean getCreateNettingGoodsShipmentPreferenceValue(Order order) {
    boolean createNettingGoodsShipment = false;
    try {
      createNettingGoodsShipment = Preferences
          .getPreferenceValue(CREATE_NETTING_SHIPMENT, true,
              OBContext.getOBContext().getCurrentClient(), order.getOrganization(),
              OBContext.getOBContext().getUser(), null, null)
          .equals("Y");
    } catch (PropertyException e1) {
      createNettingGoodsShipment = false;
    }
    return createNettingGoodsShipment;
  }

  /**
   * Method to check if during the C&amp;R process the shipment lines must be moved from the old
   * order to the new order.
   * 
   * @param order
   *          The order that is being canceled.
   * @return True if the shipment lines must be moved to the new order.
   */
  public boolean getAssociateGoodsShipmentToNewSalesOrderPreferenceValue(Order order) {
    boolean associateShipmentToNewReceipt = false;
    try {
      associateShipmentToNewReceipt = Preferences
          .getPreferenceValue(ASSOCIATE_SHIPMENT_TO_REPLACE_TICKET, true,
              OBContext.getOBContext().getCurrentClient(), order.getOrganization(),
              OBContext.getOBContext().getUser(), null, null)
          .equals("Y");
    } catch (PropertyException e1) {
      associateShipmentToNewReceipt = false;
    }
    return associateShipmentToNewReceipt;
  }

  private OrderLine getReplacementOrderLine(String oldOrderLineId) {
    return (OrderLine) OBDal.getInstance()
        .createCriteria(OrderLine.class)
        .add(Restrictions.eq(OrderLine.PROPERTY_REPLACEDORDERLINE + ".id", oldOrderLineId))
        .add(Restrictions.in(OrderLine.PROPERTY_SALESORDER + ".id", newOrderIds))
        .setFilterOnReadableOrganization(false)
        .setMaxResults(1)
        .uniqueResult();
  }

}
