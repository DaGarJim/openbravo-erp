/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2011 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_actionButton;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.reference.PInstanceProcessData;
import org.openbravo.erpCommon.utility.AttributeSetInstanceValue;
import org.openbravo.erpCommon.utility.AttributeSetInstanceValueData;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.common.plm.AttributeInstance;
import org.openbravo.model.common.plm.AttributeSet;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.manufacturing.processplan.OperationProduct;
import org.openbravo.model.manufacturing.processplan.OperationProductAttribute;
import org.openbravo.model.manufacturing.transaction.WorkRequirementProduct;
import org.openbravo.model.materialmgmt.transaction.ProductionLine;
import org.openbravo.model.materialmgmt.transaction.ProductionPlan;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.utils.Replace;

public class CreateStandars implements org.openbravo.scheduling.Process {

  private static final String lotSearchKey = "LOT";
  private static final String serialNoSearchKey = "SNO";
  private static final String expirationDateSearchKey = "EXD";

  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    try {

      final String strMProductionPlanID = (String) bundle.getParams().get("M_ProductionPlan_ID");
      final ConnectionProvider conn = bundle.getConnection();
      final VariablesSecureApp vars = bundle.getContext().toVars();

      ProductionPlan productionPlan = OBDal.getInstance().get(ProductionPlan.class,
          strMProductionPlanID);

      createStandars(productionPlan, conn, vars);
      OBDal.getInstance().save(productionPlan);
      OBDal.getInstance().flush();

      copyAttributes(conn, vars, productionPlan);
      createInstanciableAttributes(conn, vars, productionPlan);

      final OBError msg = new OBError();

      msg.setType("Success");
      msg.setTitle(Utility.messageBD(conn, "Success", bundle.getContext().getLanguage()));
      msg.setMessage(Utility.messageBD(conn, "Success", bundle.getContext().getLanguage()));
      bundle.setResult(msg);
    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace(System.err);
      final OBError msg = new OBError();
      msg.setType("Error");
      if (e instanceof org.hibernate.exception.GenericJDBCException) {
        msg.setMessage(((org.hibernate.exception.GenericJDBCException) e).getSQLException()
            .getNextException().getMessage());
      } else if (e instanceof org.hibernate.exception.ConstraintViolationException) {
        msg.setMessage(((org.hibernate.exception.ConstraintViolationException) e).getSQLException()
            .getNextException().getMessage());
      } else {
        msg.setMessage(e.getMessage());
      }
      msg.setTitle("Error occurred");
      bundle.setResult(msg);
    }
  }

  private void createStandars(ProductionPlan productionplan, ConnectionProvider conn,
      VariablesSecureApp vars) throws Exception {

    OBContext.setAdminMode();

    org.openbravo.model.ad.ui.Process process = OBDal.getInstance().get(
        org.openbravo.model.ad.ui.Process.class, "800105");

    final ProcessInstance pInstance = OBProvider.getInstance().get(ProcessInstance.class);
    pInstance.setProcess(process);
    pInstance.setActive(true);
    pInstance.setRecordID(productionplan.getId());
    pInstance.setUserContact(OBContext.getOBContext().getUser());

    OBDal.getInstance().save(pInstance);
    OBDal.getInstance().flush();

    try {
      final Connection connection = OBDal.getInstance().getConnection();
      PreparedStatement ps = null;
      final Properties obProps = OBPropertiesProvider.getInstance().getOpenbravoProperties();
      if (obProps.getProperty("bbdd.rdbms") != null
          && obProps.getProperty("bbdd.rdbms").equals("POSTGRE")) {
        ps = connection.prepareStatement("SELECT * FROM ma_productionrun_standard(?)");
      } else {
        ps = connection.prepareStatement("CALL ma_productionrun_standard(?)");
      }
      ps.setString(1, pInstance.getId());
      ps.execute();

    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

    OBDal.getInstance().getSession().refresh(pInstance);

    if (pInstance.getResult() == 0) {
      // Error Processing
      OBError myMessage = Utility
          .getProcessInstanceMessage(conn, vars, getPInstanceData(pInstance));
      throw new OBException("ERROR: " + myMessage.getMessage());
    }
    OBContext.restorePreviousMode();
  }

  private PInstanceProcessData[] getPInstanceData(ProcessInstance pInstance) throws Exception {
    Vector<java.lang.Object> vector = new Vector<java.lang.Object>(0);
    PInstanceProcessData objectPInstanceProcessData = new PInstanceProcessData();
    objectPInstanceProcessData.result = pInstance.getResult().toString();
    objectPInstanceProcessData.errormsg = pInstance.getErrorMsg();
    objectPInstanceProcessData.pMsg = "";
    vector.addElement(objectPInstanceProcessData);
    PInstanceProcessData pinstanceData[] = new PInstanceProcessData[1];
    vector.copyInto(pinstanceData);
    return pinstanceData;
  }

  private void copyAttributes(ConnectionProvider conn, VariablesSecureApp vars,
      ProductionPlan productionPlan) throws Exception {

    // CHECK PHASE EXITS
    if (productionPlan.getWRPhase() != null && productionPlan.getWRPhase().getMASequence() != null) {

      // LOOP PRODUCTIONLINES
      for (OperationProduct opProduct : productionPlan.getWRPhase().getMASequence()
          .getManufacturingOperationProductList()) {
        // ONLY PRODUCTION TYPE + AND HAS ATTSET AND HAS ATTLIST
        if (opProduct.getProductionType() != null && opProduct.getProductionType().equals("+")
            && !opProduct.getManufacturingOperationProductAttributeList().isEmpty()
            && opProduct.getProduct() != null && opProduct.getProduct().getAttributeSet() != null) {

          // NEW ATTRIBUTE
          AttributeSetInstanceValue attSetInstanceTo = new AttributeSetInstanceValue();
          HashMap<String, String> attValues = new HashMap<String, String>();

          // LOOP ATTRIBUTES
          for (OperationProductAttribute opProductAtt : opProduct
              .getManufacturingOperationProductAttributeList()) {

            // CHECK ATTFROM EXISTS
            AttributeSetInstance attSetInstanceFrom = null;

            OBCriteria ProductionLineCriteria = OBDal.getInstance().createCriteria(
                ProductionLine.class);
            ProductionLineCriteria.add(Restrictions.eq(ProductionLine.PROPERTY_PRODUCTIONPLAN,
                productionPlan));
            ProductionLineCriteria.createAlias(ProductionLine.PROPERTY_WRPRODUCTPHASE, "wrpp");
            ProductionLineCriteria.add(Restrictions.eq("wrpp."
                + WorkRequirementProduct.PROPERTY_SEQUENCEPRODUCT, opProductAtt.getProductFrom()));

            List<ProductionLine> plinesToCopyFrom = ProductionLineCriteria.list();

            if (!plinesToCopyFrom.isEmpty()) {
              int i = 0;
              while (attSetInstanceFrom == null) {
                attSetInstanceFrom = plinesToCopyFrom.get(i).getAttributeSetValue();
                i++;
              }
            }

            OBContext.setAdminMode();

            if (attSetInstanceFrom != null && !attSetInstanceFrom.getId().equals("0")) {
              if (opProductAtt.isSpecialatt()) {
                // SPECIAL ATT
                // LOT
                if (opProductAtt.getSpecialatt().equals(lotSearchKey))
                  attSetInstanceTo.setLot(attSetInstanceFrom.getLotName());
                // SERNO
                if (opProductAtt.getSpecialatt().equals(serialNoSearchKey))
                  attSetInstanceTo.setSerialNumber(attSetInstanceFrom.getSerialNo());
                // GDate //
                if (opProductAtt.getSpecialatt().equals(expirationDateSearchKey)) {
                  attSetInstanceTo.setGuaranteeDate(dateToString(attSetInstanceFrom
                      .getExpirationDate()));
                }
              } else {
                // NORMAL ATT
                // CHECK ATT_TO EXISTS
                if (opProductAtt.getAttributeuseto() != null
                    && opProductAtt.getAttributeuseto().getAttribute() != null) {
                  // GetValue From
                  OBCriteria attributeInstanceCriteria = OBDal.getInstance().createCriteria(
                      AttributeInstance.class);
                  attributeInstanceCriteria.add(Restrictions.eq(
                      AttributeInstance.PROPERTY_ATTRIBUTESETVALUE, attSetInstanceFrom));
                  attributeInstanceCriteria.add(Restrictions.eq(
                      AttributeInstance.PROPERTY_ATTRIBUTE, opProductAtt.getAttributeUse()
                          .getAttribute()));
                  List<AttributeInstance> AttributeInstanceList = attributeInstanceCriteria.list();
                  // Add value
                  if (!AttributeInstanceList.isEmpty()) {
                    if (AttributeInstanceList.get(0).getAttributeValue() == null) {
                      attValues.put(replace(opProductAtt.getAttributeuseto().getAttribute()
                          .getName()), AttributeInstanceList.get(0).getSearchKey());
                    } else {
                      attValues.put(replace(opProductAtt.getAttributeuseto().getAttribute()
                          .getName()), AttributeInstanceList.get(0).getAttributeValue().getId());
                    }

                  }
                }
              }
            }
            OBContext.restorePreviousMode();
          } // END LOOP ATTRIBUTES

          // UPDATE LINES

          OBCriteria ProductionLineCriteria = OBDal.getInstance().createCriteria(
              ProductionLine.class);
          ProductionLineCriteria.add(Restrictions.eq(ProductionLine.PROPERTY_PRODUCTIONPLAN,
              productionPlan));
          ProductionLineCriteria.add(Restrictions.eq(ProductionLine.PROPERTY_PRODUCTIONTYPE, "+"));
          ProductionLineCriteria.createAlias(ProductionLine.PROPERTY_WRPRODUCTPHASE, "wrpp");
          ProductionLineCriteria.add(Restrictions.eq("wrpp."
              + WorkRequirementProduct.PROPERTY_SEQUENCEPRODUCT, opProduct));

          List<ProductionLine> plinesToCopyTo = ProductionLineCriteria.list();

          for (ProductionLine pline : plinesToCopyTo) {

            // CREATE ATRIBUTE
            if (pline.getProduct().getAttributeSet().isExpirationDate()
                && (attSetInstanceTo.getGuaranteeDate() == null || attSetInstanceTo
                    .getGuaranteeDate().equals(""))
                && pline.getProduct().getAttributeSet().getGuaranteedDays() != null
                && pline.getProduct().getAttributeSet().getGuaranteedDays() != 0L) {
              // Set GuaranteeDate if is not copied
              Date movementdate = ((productionPlan.getProductionplandate() != null) ? productionPlan
                  .getProductionplandate() : productionPlan.getProduction().getMovementDate());
              int days = pline.getProduct().getAttributeSet().getGuaranteedDays().intValue();
              attSetInstanceTo.setGuaranteeDate(dateToString(addDays(movementdate, days)));
            }
            AttributeSetInstanceValueData[] data = AttributeSetInstanceValueData.select(conn,
                opProduct.getProduct().getAttributeSet().getId());
            OBError createAttributeInstanceError = attSetInstanceTo.setAttributeInstance(conn,
                vars, data, opProduct.getProduct().getAttributeSet().getId(), "", "", "N",
                opProduct.getProduct().getId(), attValues);
            if (!createAttributeInstanceError.getType().equals("Success"))
              throw new OBException(createAttributeInstanceError.getMessage());

            OBDal.getInstance().flush();

            AttributeSetInstance newAttSetinstance = OBDal.getInstance().get(
                AttributeSetInstance.class, attSetInstanceTo.getAttSetInstanceId());

            pline.setAttributeSetValue(newAttSetinstance);
            OBDal.getInstance().save(pline);
          }

        }
      }
      OBDal.getInstance().flush();
    }
  }

  private void createInstanciableAttributes(ConnectionProvider conn, VariablesSecureApp vars,
      ProductionPlan productionPlan) throws Exception {
    OBCriteria ProductionLineCriteria = OBDal.getInstance().createCriteria(ProductionLine.class);
    ProductionLineCriteria.add(Restrictions.eq(ProductionLine.PROPERTY_PRODUCTIONPLAN,
        productionPlan));
    ProductionLineCriteria.add(Restrictions.eq(ProductionLine.PROPERTY_PRODUCTIONTYPE, "+"));
    List<ProductionLine> plines = ProductionLineCriteria.list();
    for (ProductionLine line : plines) {
      // Check has empty attribute
      if (line.getProduct().getAttributeSet() != null
          && line.getProduct().getAttributeSetValue() == null) {
        AttributeSet attSet = line.getProduct().getAttributeSet();
        // Check if has automatic attributes
        if ((attSet.isLot() && attSet.getLotControl() != null)
            || (attSet.isSerialNo() && attSet.getSerialNoControl() != null)
            || (attSet.isExpirationDate() && attSet.getGuaranteedDays() != null && attSet
                .getGuaranteedDays() != 0L)) {

          AttributeSetInstanceValue attSetInstance = new AttributeSetInstanceValue();
          HashMap<String, String> attValues = new HashMap<String, String>();

          if (attSet.isExpirationDate()) {
            Date movementdate = ((productionPlan.getProductionplandate() != null) ? productionPlan
                .getProductionplandate() : productionPlan.getProduction().getMovementDate());
            int days = attSet.getGuaranteedDays().intValue();
            attSetInstance.setGuaranteeDate(dateToString(addDays(movementdate, days)));
          }
          AttributeSetInstanceValueData[] data = AttributeSetInstanceValueData.select(conn,
              attSet.getId());
          OBError createAttributeInstanceError = attSetInstance.setAttributeInstance(conn, vars,
              data, attSet.getId(), "", "", "N", line.getProduct().getId(), attValues);
          if (!createAttributeInstanceError.getType().equals("Success"))
            throw new OBException(createAttributeInstanceError.getMessage());

          OBDal.getInstance().flush();

          AttributeSetInstance newAttSetinstance = OBDal.getInstance().get(
              AttributeSetInstance.class, attSetInstance.getAttSetInstanceId());

          line.setAttributeSetValue(newAttSetinstance);
          OBDal.getInstance().save(line);

        }
      }
    }
    OBDal.getInstance().flush();
  }

  private String replace(String strIni) {
    // delete characters: " ","&",","
    return Replace.replace(Replace.replace(Replace.replace(
        Replace.replace(Replace.replace(Replace.replace(strIni, "#", ""), " ", ""), "&", ""), ",",
        ""), "(", ""), ")", "");
  }

  private String dateToString(Date date) throws Exception {
    if (date == null)
      return "";
    String dateformat = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("dateFormat.java");
    SimpleDateFormat formater = new SimpleDateFormat(dateformat);
    return formater.format(date);
  }

  private Date addDays(Date date, int days) throws Exception {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.add(Calendar.DATE, days);
    Date gdate = calendar.getTime();
    return gdate;
  }
}
