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
 * All portions are Copyright (C) 2010-2011 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.window;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.application.ApplicationUtils;
import org.openbravo.client.kernel.BaseTemplateComponent;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.client.kernel.Template;
import org.openbravo.client.kernel.reference.StringUIDefinition;
import org.openbravo.client.kernel.reference.UIDefinition;
import org.openbravo.client.kernel.reference.UIDefinitionController;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.Sqlc;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.json.JsonConstants;

/**
 * The backing bean for generating the OBViewGrid client-side representation.
 * 
 * @author mtaal
 */
public class OBViewGridComponent extends BaseTemplateComponent {

  private static final String TEMPLATE_ID = "91DD63545B674BE8801E1FA4F48FF4C6";
  private static Long ZERO = new Long(0);

  private boolean applyTransactionalFilter = false;
  private Tab tab;
  private List<LocalField> fields = null;

  protected Template getComponentTemplate() {
    return OBDal.getInstance().get(Template.class, TEMPLATE_ID);
  }

  public Tab getTab() {
    return tab;
  }

  public void setTab(Tab tab) {
    this.tab = tab;
  }

  public String getWhereClause() {
    if (tab.getHqlwhereclause() != null) {
      return tab.getHqlwhereclause();
    }
    return "";
  }

  public String getOrderByClause() {
    if (tab.getHqlorderbyclause() != null) {
      return tab.getHqlorderbyclause();
    }
    return JsonConstants.IDENTIFIER;
  }

  public String getFilterClause() {
    if (tab.getHqlfilterclause() != null) {
      return addTransactionalFilter(tab.getHqlfilterclause());
    }
    return addTransactionalFilter("");
  }

  private String addTransactionalFilter(String filterClause) {
    if (!this.isApplyTransactionalFilter()) {
      return filterClause;
    }
    String transactionalFilter = " e.updated > " + JsonConstants.QUERY_PARAM_TRANSACTIONAL_RANGE
        + " ";
    final Entity entity = ModelProvider.getInstance().getEntityByTableId(
        (String) DalUtil.getId(tab.getTable()));
    if (entity.hasProperty(Order.PROPERTY_PROCESSED)) {
      transactionalFilter += " and e.processed = false ";
    }

    if (filterClause.length() > 0) {
      return " ((" + transactionalFilter + ") and (" + filterClause + ")) ";
    }
    return transactionalFilter;
  }

  public List<String> getForeignKeyFields() {
    final List<String> fkFields = new ArrayList<String>();
    for (LocalField field : getFields()) {
      if (!field.getProperty().isPrimitive()) {
        fkFields.add(field.getProperty().getName());
      }
    }
    return fkFields;
  }

  public List<LocalField> getFields() {
    if (fields != null) {
      return fields;
    }
    fields = new ArrayList<LocalField>();
    final List<String> windowEntities = getWindowEntities();
    final List<Field> sortedFields = new ArrayList<Field>(tab.getADFieldList());
    Collections.sort(sortedFields, new GridFieldComparator());

    // first add the grid fields
    for (Field fld : sortedFields) {
      if (fld.isActive() && fld.isShowInGridView()) {
        final Property prop = KernelUtils.getInstance().getPropertyFromColumn(fld.getColumn());
        if (prop.isParent() && windowEntities.contains(prop.getTargetEntity().getName())) {
          continue;
        }
        if (prop.isId()) {
          continue;
        }
        if (!fld.isDisplayed()) {
          continue;
        }
        if (ApplicationUtils.isUIButton(fld)) {
          continue;
        }
        // these are currently also ignored
        if (fld.getGridPosition() == null && fld.getSequenceNumber() == null) {
          continue;
        }

        fields.add(createLocalField(fld, prop, true));
      }
    }
    // and add the non grid fields
    for (Field fld : sortedFields) {
      if (fld.isActive() && !fld.isShowInGridView()) {
        final Property prop = KernelUtils.getInstance().getPropertyFromColumn(fld.getColumn());
        if (prop.isParent() && windowEntities.contains(prop.getTargetEntity().getName())) {
          continue;
        }
        if (prop.isId()) {
          continue;
        }
        if (!fld.isDisplayed()) {
          continue;
        }
        if (ApplicationUtils.isUIButton(fld)) {
          continue;
        }
        fields.add(createLocalField(fld, prop, false));
      }
    }
    return fields;
  }

  private LocalField createLocalField(Field fld, Property prop, boolean showInitially) {
    final LocalField localField = new LocalField();
    localField.setField(fld);
    localField.setProperty(prop);
    localField.setUIDefinition(UIDefinitionController.getInstance().getUIDefinition(
        prop.getColumnId()));
    if (!prop.isPrimitive()) {
      localField.setName(prop.getName() + "." + JsonConstants.IDENTIFIER);
    } else {
      localField.setName(prop.getName());
    }
    localField.setTitle(OBViewUtil.getLabel(fld));
    localField.setInitialShow(showInitially);
    return localField;
  }

  public class LocalField {
    private String name;
    private Field field;
    private String title;
    private Property property;
    private UIDefinition uiDefinition;
    private boolean initialShow;

    public String getInpColumnName() {
      return "inp" + Sqlc.TransformaNombreColumna(property.getColumnName());
    }

    public String getReferencedKeyColumnName() {
      if (property.isOneToMany() || property.isPrimitive()) {
        return "";
      }
      Property prop;
      if (property.getReferencedProperty() == null) {
        prop = property.getTargetEntity().getIdProperties().get(0);
      } else {
        prop = property.getReferencedProperty();
      }
      return prop.getColumnName();
    }

    /**
     * @deprecated use {@link #getGridFieldProperties()}
     */
    @Deprecated
    public String getFieldProperties() {
      return getGridFieldProperties();
    }

    public String getGridFieldProperties() {
      return uiDefinition.getGridFieldProperties(field);
    }

    public String getTargetEntity() {
      if (property.getTargetEntity() == null) {
        return "";
      }
      return property.getTargetEntity().getName();
    }

    public String getGridEditorFieldProperties() {
      String props = uiDefinition.getGridEditorFieldProperties(field).trim();
      if (props.startsWith("{")) {
        props = props.substring(1, props.length() - 1);
      }
      if (props.trim().endsWith(",")) {
        return props.trim().substring(0, props.trim().length() - 1);
      }
      return props.trim();
    }

    public String getFilterEditorProperties() {
      return uiDefinition.getFilterEditorProperties(field);
    }

    // can the column be auto expanded to fill any remaining space
    public String getAutoExpand() {
      return Boolean
          .toString(uiDefinition instanceof StringUIDefinition || !property.isPrimitive());
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public UIDefinition getUIDefinition() {
      return uiDefinition;
    }

    public void setUIDefinition(UIDefinition typeDefinition) {
      this.uiDefinition = typeDefinition;
    }

    public String getType() {
      return uiDefinition.getName();
    }

    public Field getField() {
      return field;
    }

    public void setField(Field field) {
      this.field = field;
    }

    public Property getProperty() {
      return property;
    }

    public void setProperty(Property property) {
      this.property = property;
    }

    public boolean isInitialShow() {
      return initialShow;
    }

    public void setInitialShow(boolean initialShow) {
      this.initialShow = initialShow;
    }
  }

  private class GridFieldComparator implements Comparator<Field> {

    @Override
    public int compare(Field arg0, Field arg1) {
      Long arg0Position = (arg0.getGridPosition() != null ? arg0.getGridPosition() : arg0
          .getSequenceNumber());
      Long arg1Position = (arg1.getGridPosition() != null ? arg1.getGridPosition() : arg1
          .getSequenceNumber());
      if (arg0Position == null) {
        arg0Position = ZERO;
      }
      if (arg1Position == null) {
        arg1Position = ZERO;
      }
      return (int) (arg0Position - arg1Position);
    }

  }

  private List<String> getWindowEntities() {
    final List<String> windowEntities = new ArrayList<String>();
    for (Tab localTab : tab.getWindow().getADTabList()) {
      windowEntities.add(localTab.getTable().getName());
    }
    return windowEntities;
  }

  public boolean isApplyTransactionalFilter() {
    return applyTransactionalFilter;
  }

  public void setApplyTransactionalFilter(boolean applyTransactionalFilter) {
    this.applyTransactionalFilter = applyTransactionalFilter;
  }

}
