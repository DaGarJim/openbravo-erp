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
 * All portions are Copyright (C) 2024 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.materialmgmt.refinventory;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;

/**
 * It calculates the new attribute set instance for a boxing. The calculated attribute set instance
 * must be included into a warehouse transaction document that, when booked, will perform the actual
 * boxing.
 * 
 * Note that, from the UI/UX perspective, a boxing is always performed in the outermost reference
 * inventory. However, in a programmatic way, it is also supported to box a stock into a inner
 * reference inventory. In this case the class assumes the Reference Inventory tree structure
 * already exists.
 *
 * This class can be used for any boxing activity, from a simple 1-level boxing to more complex
 * scenarios like a nested/cascade boxing of stock performed by an external warehouse that should be
 * received by Openbravo.
 * 
 */
public class BoxingAttributeSetInstanceToBuilder {

  // RI_OriginalAttributeSetInstanceId:NewAttributeSetInstanceId created by this object
  private Map<String, String> ri_OriginalAttributeIdVsNewAttributeIdMap = new HashMap<>();

  private ReferencedInventory innermostRefInventory;
  private AttributeSetInstance stockAttributeSetInstance;

  /**
   * Takes the innermost referenced inventory already included into a tree structure and the stock's
   * current attribute set instance to calculate the attribute set instance the stock should have to
   * perform a nested boxing.
   * 
   * It also works with loose referenced inventory without a tree structure.
   * 
   * @param innermostRefInventory
   *          this is the innermost referenced inventory (or the loose referenced inventory when
   *          outside a tree structure) where the stock will be stored into. If null, i.e. if not
   *          referenced inventory provided, then the calculator will return the current stock
   *          attribute set instance.
   * @param stockAttributeSetInstance
   *          this is the current stock attribute set instance before boxing it.
   */
  public BoxingAttributeSetInstanceToBuilder(final ReferencedInventory innermostRefInventory,
      final AttributeSetInstance stockAttributeSetInstance) {
    this.innermostRefInventory = innermostRefInventory;
    this.stockAttributeSetInstance = stockAttributeSetInstance;
  }

  /**
   * Optional Map with the relationship between the RI with the original attribute set instance and
   * the new attribute set instance. If provided, it speeds up the calculation.
   * 
   * This method is only useful when the caller performs the boxing of several items at the same
   * time.
   * 
   * @param ri_OriginalAttributeVsNewAttributeId
   *          map with key = "ReferencedInventoryID"_"OriginalAttributeSetInstanceID" and value =
   *          "new Attribute Set Instance ID".
   */
  public BoxingAttributeSetInstanceToBuilder withCache(
      final Map<String, String> ri_OriginalAttributeVsNewAttributeId) {
    this.ri_OriginalAttributeIdVsNewAttributeIdMap = ri_OriginalAttributeVsNewAttributeId;
    return this;
  }

  /**
   * Calculate the attribute set instance to perform the boxing. It automatically supports cascade
   * for nested referenced inventories.
   * 
   * No validation related to Content Restrictions or Storage Type is performed on purpose, because
   * these kind of validations should have been performed before calling this method.
   * 
   * @return the AttributeSetInstance ready to be included into the warehouse transaction document
   *         line which will perform the actual boxing when booked
   */
  public AttributeSetInstance build() {
    ReferencedInventory boxInReferencedInventory = innermostRefInventory;
    if (boxInReferencedInventory == null) {
      return stockAttributeSetInstance;
    }
    AttributeSetInstance attributeSetInstance = (AttributeSetInstance) ObjectUtils.defaultIfNull(
        stockAttributeSetInstance, OBDal.getInstance().getProxy(AttributeSetInstance.class, "0"));

    while (boxInReferencedInventory != null) {
      attributeSetInstance = calculateAttributeSetInstanceToForBoxing(attributeSetInstance,
          boxInReferencedInventory);
      boxInReferencedInventory = boxInReferencedInventory.getParentRefInventory();
    }

    return attributeSetInstance;
  }

  private String getMapKey(final ReferencedInventory boxInReferencedInventory,
      final AttributeSetInstance originalAttributeSetInstance) {
    return boxInReferencedInventory.getId() + "_" + originalAttributeSetInstance.getId();
  }

  private AttributeSetInstance calculateAttributeSetInstanceToForBoxing(
      final AttributeSetInstance originalAttributeSetInstance,
      final ReferencedInventory boxInReferencedInventory) {
    // Attribute previously created in this box execution
    if (ri_OriginalAttributeIdVsNewAttributeIdMap
        .containsKey(getMapKey(boxInReferencedInventory, originalAttributeSetInstance))) {
      return OBDal.getInstance()
          .getProxy(AttributeSetInstance.class, ri_OriginalAttributeIdVsNewAttributeIdMap
              .get(getMapKey(boxInReferencedInventory, originalAttributeSetInstance)));
    }

    // Attribute previously created in other box executions for this refInventory
    final AttributeSetInstance previouslyClonedAttributeSetInstance = ReferencedInventoryUtil
        .getAlreadyClonedAttributeSetInstance(originalAttributeSetInstance,
            boxInReferencedInventory);
    if (previouslyClonedAttributeSetInstance == null) {
      final AttributeSetInstance newAttributeSetInstance = ReferencedInventoryUtil
          .cloneAttributeSetInstance(originalAttributeSetInstance, boxInReferencedInventory);
      ri_OriginalAttributeIdVsNewAttributeIdMap.put(
          getMapKey(boxInReferencedInventory, originalAttributeSetInstance),
          newAttributeSetInstance.getId());
      return newAttributeSetInstance;
    } else {
      ri_OriginalAttributeIdVsNewAttributeIdMap.put(
          getMapKey(boxInReferencedInventory, originalAttributeSetInstance),
          previouslyClonedAttributeSetInstance.getId());
      return previouslyClonedAttributeSetInstance;
    }
  }

}
