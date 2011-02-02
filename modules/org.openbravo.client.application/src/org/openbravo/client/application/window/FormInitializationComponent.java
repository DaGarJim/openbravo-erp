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

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Expression;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.structure.ClientEnabled;
import org.openbravo.base.structure.OrganizationEnabled;
import org.openbravo.client.application.DynamicExpressionParser;
import org.openbravo.client.application.window.servlet.CalloutHttpServletResponse;
import org.openbravo.client.application.window.servlet.CalloutServletConfig;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.client.kernel.reference.EnumUIDefinition;
import org.openbravo.client.kernel.reference.FKComboUIDefinition;
import org.openbravo.client.kernel.reference.UIDefinition;
import org.openbravo.client.kernel.reference.UIDefinitionController;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.Sqlc;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.ReferencedTable;
import org.openbravo.model.ad.ui.AuxiliaryInput;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.json.JsonToDataConverter;
import org.openbravo.service.json.JsonUtils;

/**
 * This class computes all the required information in Openbravo 3 forms. Basically, this can be
 * summarized in the following actions:
 * 
 * Computation of all required column information (including combo values)
 * 
 * Computation of auxiliary input values
 * 
 * Execution of callouts
 * 
 * Insertion of all relevant data in the session
 * 
 * Format: in the request and session the values are always formatted in classic mode. The ui
 * definition computes jsonobjects which contain a value as well as a classicValue, the latter is
 * placed in the request/session for subsequent callout computations.
 */
public class FormInitializationComponent extends BaseActionHandler {
  private static final Logger log = Logger.getLogger(FormInitializationComponent.class);

  private static final int MAX_CALLOUT_CALLS = 50;

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    OBContext.setAdminMode(true);
    long iniTime = System.currentTimeMillis();
    try {
      // Execution mode. It can be:
      // - NEW: used when the user clicks on the "New record" button
      // - EDIT: used when the user opens a record in form view
      // - CHANGE: used when the user changes a field which should fire callouts or comboreloads
      // - SETSESSION: used when the user calls a process
      String mode = (String) parameters.get("MODE");
      // ID of the parent record
      String parentId = (String) parameters.get("PARENT_ID");
      // The ID of the tab
      String tabId = (String) parameters.get("TAB_ID");
      // The ID of the record. Only relevant on EDIT, CHANGE and SETSESSION modes
      String rowId = (String) parameters.get("ROW_ID");
      // The column changed by the user. Only relevant on CHANGE mode
      String changedColumn = (String) parameters.get("CHANGED_COLUMN");
      Tab tab = OBDal.getInstance().get(Tab.class, tabId);
      BaseOBObject row = null;
      BaseOBObject parentRecord = null;
      Map<String, JSONObject> columnValues = new HashMap<String, JSONObject>();
      List<String> allColumns = new ArrayList<String>();
      List<String> calloutsToCall = new ArrayList<String>();
      List<String> lastfieldChanged = new ArrayList<String>();
      List<String> changeEventCols = new ArrayList<String>();
      Map<String, List<String>> columnsInValidation = new HashMap<String, List<String>>();
      List<String> calloutMessages = new ArrayList<String>();

      log.debug("Form Initialization Component Execution. Tab Name: " + tab.getWindow().getName()
          + "." + tab.getName() + " Tab Id:" + tab.getId());
      log.debug("Execution mode: " + mode);
      if (rowId != null) {
        log.debug("Row id: " + rowId);
      }
      if (changedColumn != null) {
        log.debug("Changed field: " + changedColumn);
      }
      if (rowId != null) {
        row = OBDal.getInstance().get(tab.getTable().getName(), rowId);
      }
      JSONObject jsContent = new JSONObject();
      try {
        if (content != null) {
          jsContent = new JSONObject(content);
        }
      } catch (JSONException e) {
        throw new OBException("Error while parsing content", e);
      }

      // First the parent record is retrieved and the session variables for the parent records are
      // set
      parentRecord = setSessionVariablesInParent(mode, tab, row, parentId);

      // We also need to set the current record values in the request
      setValuesInRequest(mode, tab, row, jsContent);

      // Calculation of validation dependencies
      computeListOfColumnsSortedByValidationDependencies(tab, allColumns, columnsInValidation,
          changeEventCols);

      // Computation of the Auxiliary Input values
      computeAuxiliaryInputs(mode, tab, columnValues);

      // Computation of Column Values (using UIDefinition, so including combo values and all
      // relevant additional information)
      computeColumnValues(mode, tab, allColumns, columnValues, parentRecord, parentId,
          changedColumn, jsContent, changeEventCols, calloutsToCall, lastfieldChanged);

      // Execution of callouts
      executeCallouts(mode, tab, columnValues, changedColumn, calloutsToCall, lastfieldChanged,
          calloutMessages);

      // Construction of the final JSONObject
      JSONObject finalObject = buildJSONObject(mode, tab, columnValues, row, changeEventCols,
          calloutMessages);
      log.debug("Elapsed time: " + (System.currentTimeMillis() - iniTime));
      return finalObject;
    } catch (Throwable t) {
      final String jsonString = JsonUtils.convertExceptionToJson(t);
      try {
        return new JSONObject(jsonString);
      } catch (JSONException e) {
        log.error("Error while generating the error JSON object: " + jsonString, e);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return null;
  }

  private JSONObject buildJSONObject(String mode, Tab tab, Map<String, JSONObject> columnValues,
      BaseOBObject row, List<String> changeEventCols, List<String> calloutMessages) {
    JSONObject finalObject = new JSONObject();
    try {
      if (mode.equals("NEW") || mode.equals("CHANGE")) {
        JSONArray arrayMessages = new JSONArray(calloutMessages);
        finalObject.put("calloutMessages", arrayMessages);
      }
      if (mode.equals("NEW") || mode.equals("EDIT") || mode.equals("CHANGE")) {
        JSONObject jsonColumnValues = new JSONObject();
        for (Field field : tab.getADFieldList()) {
          jsonColumnValues.put(field.getColumn().getDBColumnName(), columnValues.get("inp"
              + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName())));
        }
        finalObject.put("columnValues", jsonColumnValues);
      }
      JSONObject jsonAuxiliaryInputValues = new JSONObject();
      for (AuxiliaryInput auxIn : tab.getADAuxiliaryInputList()) {
        jsonAuxiliaryInputValues.put(auxIn.getName(), columnValues.get("inp"
            + Sqlc.TransformaNombreColumna(auxIn.getName())));
      }

      finalObject.put("auxiliaryInputValues", jsonAuxiliaryInputValues);

      if (mode.equals("NEW") || mode.equals("EDIT")) {
        // We also include information related to validation dependencies
        // and we add the columns which have a callout

        final Map<String, String> sessionAttributesMap = new HashMap<String, String>();
        for (Field field : tab.getADFieldList()) {
          if (field.getColumn().getCallout() != null) {
            final String columnName = "inp"
                + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName());
            if (!changeEventCols.contains(columnName)) {
              changeEventCols.add(columnName);
            }
          }

          // Adding session attributes in a dynamic expression
          // This session attributes could be a preference
          if (field.getDisplayLogic() != null && field.isDisplayed() && field.isActive()) {
            final DynamicExpressionParser parser = new DynamicExpressionParser(field
                .getDisplayLogic(), tab);

            for (String attrName : parser.getSessionAttributes()) {
              if (!sessionAttributesMap.containsKey(attrName)) {
                final String attrValue = Utility
                    .getContext(new DalConnectionProvider(false), RequestContext.get()
                        .getVariablesSecureApp(), attrName, tab.getWindow().getId());
                sessionAttributesMap.put(attrName.startsWith("#") ? attrName.replace("#", "_")
                    : attrName, attrValue);
              }

            }
          }

        }

        final JSONObject sessionAttributes = new JSONObject();
        for (String attr : sessionAttributesMap.keySet()) {
          sessionAttributes.put(attr, sessionAttributesMap.get(attr));
        }

        finalObject.put("sessionAttributes", sessionAttributes);
        finalObject.put("dynamicCols", new JSONArray(changeEventCols));
      }

      if (mode.equals("EDIT") && row != null) {
        final String rowClientId = ((ClientEnabled) row).getClient().getId();
        final String currentClientId = OBContext.getOBContext().getCurrentClient().getId();
        if (!rowClientId.equals(currentClientId)) {
          finalObject.put("writable", false);
        } else {
          boolean writable = false;
          final String objectOrgId = ((OrganizationEnabled) row).getOrganization().getId();
          for (String orgId : OBContext.getOBContext().getWritableOrganizations()) {
            if (orgId.equals(objectOrgId)) {
              writable = true;
              break;
            }
          }
          finalObject.put("writable", writable);
        }
      } else {
        finalObject.put("writable", true);
      }

      log.debug(finalObject.toString(1));
      return finalObject;
    } catch (JSONException e) {
      log.error("Error while generating the final JSON object: ", e);
      return null;
    }
  }

  private void computeColumnValues(String mode, Tab tab, List<String> allColumns,
      Map<String, JSONObject> columnValues, BaseOBObject parentRecord, String parentId,
      String changedColumn, JSONObject jsContent, List<String> changeEventCols,
      List<String> calloutsToCall, List<String> lastfieldChanged) {
    boolean forceComboReload = (mode.equals("CHANGE") && changedColumn == null);
    HashMap<String, Field> columnsOfFields = new HashMap<String, Field>();
    for (Field field : tab.getADFieldList()) {
      columnsOfFields.put(field.getColumn().getDBColumnName(), field);
    }
    for (String col : allColumns) {
      Field field = columnsOfFields.get(col);
      try {
        String columnId = field.getColumn().getId();
        final Property prop = KernelUtils.getInstance().getPropertyFromColumn(field.getColumn());
        UIDefinition uiDef = UIDefinitionController.getInstance().getUIDefinition(columnId);
        String value = null;
        if (mode.equals("NEW")) {
          // On NEW mode, the values are computed through the UIDefinition (the defaults will be
          // used)
          if (field.getColumn().isLinkToParentColumn() && parentRecord != null
              && referencedEntityIsParent(parentRecord, field)) {
            // If the column is link to the parent tab, we set its value as the parent id
            RequestContext.get().setRequestParameter("inp" + Sqlc.TransformaNombreColumna(col),
                parentId);
            value = uiDef.getFieldProperties(field, true);
          } else if (field.getColumn().getDBColumnName().equalsIgnoreCase("IsActive")) {
            // The Active column is always set to 'true' on new records
            RequestContext.get()
                .setRequestParameter("inp" + Sqlc.TransformaNombreColumna(col), "Y");
            value = uiDef.getFieldProperties(field, true);
          } else {
            // Else, the default is used
            value = uiDef.getFieldProperties(field, false);
          }
        } else if (mode.equals("EDIT")
            || (mode.equals("CHANGE") && (forceComboReload || changeEventCols
                .contains(changedColumn)))) {
          // On EDIT mode, the values are computed through the UIDefinition (the values have been
          // previously set in the RequestContext)
          // This is also done this way on CHANGE mode where a combo reload is needed
          value = uiDef.getFieldProperties(field, true);
        } else if (mode.equals("CHANGE") || mode.equals("SETSESSION")) {
          // On CHANGE and SETSESSION mode, the values are read from the request
          JSONObject jsCol = new JSONObject();
          String colName = "inp" + Sqlc.TransformaNombreColumna(col);
          Object jsonValue = null;
          if (jsContent.has(colName)) {
            jsonValue = jsContent.get(colName);
          } else if (jsContent.has(field.getColumn().getDBColumnName())) {
            // Special case related to the primary key column, which is sent with its dbcolumnname
            // instead of the "inp" name
            jsonValue = jsContent.get(field.getColumn().getDBColumnName());
          }

          if (prop.isPrimitive()) {
            final Object propValue = JsonToDataConverter
                .convertJsonToPropertyValue(prop, jsonValue);
            final String classicStr = uiDef.convertToClassicString(propValue);
            jsCol.put("value", propValue);
            jsCol.put("classicValue", classicStr);
          } else {
            jsCol.put("value", jsonValue);
            jsCol.put("classicValue", jsonValue);
          }
        }
        JSONObject jsonobject = null;
        if (value != null) {
          jsonobject = new JSONObject(value);
          columnValues.put("inp"
              + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName()), jsonobject);
          // We need to fire callouts if the field is a combo
          // (due to how ComboReloads worked, callouts were always called)
          if (mode.equals("NEW")
              && (uiDef instanceof EnumUIDefinition || uiDef instanceof FKComboUIDefinition)) {
            if (field.getColumn().getCallout() != null) {
              addCalloutToList(field.getColumn(), calloutsToCall, lastfieldChanged);
            }
          }
          setRequestContextParameter(field, jsonobject);
          // We also set the session value for the column in Edit or SetSession mode
          if (mode.equals("NEW") || mode.equals("EDIT") || mode.equals("SETSESSION")) {
            if (field.getColumn().isStoredInSession() || field.getColumn().isKeyColumn()) {
              setSessionValue(tab.getWindow().getId() + "|"
                  + field.getColumn().getDBColumnName().toUpperCase(), jsonobject
                  .has("classicValue") ? jsonobject.get("classicValue") : null);
            }
          }

        }
      } catch (Exception e) {
        throw new OBException(
            "Couldn't get data for column " + field.getColumn().getDBColumnName(), e);
      }
    }
  }

  private void computeAuxiliaryInputs(String mode, Tab tab, Map<String, JSONObject> columnValues) {
    for (AuxiliaryInput auxIn : tab.getADAuxiliaryInputList()) {
      Object value = computeAuxiliaryInput(auxIn, tab.getWindow().getId());
      log.debug("Final Computed Value. Name: " + auxIn.getName() + " Value: " + value);
      JSONObject jsonObj = new JSONObject();
      try {
        jsonObj.put("value", value);
        jsonObj.put("classicValue", value);
      } catch (JSONException e) {
        log.error("Error while computing auxiliary input " + auxIn.getName(), e);
      }
      columnValues.put("inp" + Sqlc.TransformaNombreColumna(auxIn.getName()), jsonObj);
      RequestContext.get().setRequestParameter(
          "inp" + Sqlc.TransformaNombreColumna(auxIn.getName()),
          value == null ? null : value.toString());
      // Now we insert session values for auxiliary inputs
      if (mode.equals("NEW") || mode.equals("EDIT") || mode.equals("SETSESSION")) {
        setSessionValue(tab.getWindow().getId() + "|" + auxIn.getName(), value);
      }
    }
  }

  private BaseOBObject setSessionVariablesInParent(String mode, Tab tab, BaseOBObject row,
      String parentId) {
    BaseOBObject parentRecord = null;
    if (mode.equals("EDIT")) {
      parentRecord = KernelUtils.getInstance().getParentRecord(row, tab);
    }
    Tab parentTab = KernelUtils.getInstance().getParentTab(tab);
    if (parentId != null && parentTab != null) {
      parentRecord = OBDal.getInstance().get(
          ModelProvider.getInstance().getEntityByTableName(parentTab.getTable().getDBTableName())
              .getName(), parentId);
    }
    if (parentTab != null && parentRecord != null) {
      setSessionValues(parentRecord, parentTab);
    }
    return parentRecord;
  }

  private void setValuesInRequest(String mode, Tab tab, BaseOBObject row, JSONObject jsContent) {
    List<Field> fields = tab.getADFieldList();
    if (mode.equals("EDIT")) {
      // In EDIT mode we get them from the database
      for (Field field : fields) {
        setValueOfColumnInRequest(row, field.getColumn().getDBColumnName());
      }
    } else if (mode.equals("CHANGE") || mode.equals("SETSESSION")) {
      // In CHANGE and SETSESSION we get them from the request
      for (Field field : fields) {
        final Property prop = KernelUtils.getInstance().getPropertyFromColumn(field.getColumn());
        String inpColName = "inp"
            + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName());
        try {
          if (jsContent.has(inpColName)) {
            String value;
            if (jsContent.get(inpColName) == null
                || jsContent.get(inpColName).toString().equals("null")) {
              value = null;
            } else if (prop.isPrimitive()) {
              // convert to a dal value
              final Object propValue = JsonToDataConverter.convertJsonToPropertyValue(prop,
                  jsContent.get(inpColName));
              // convert to a valid classic string
              value = UIDefinitionController.getInstance().getUIDefinition(
                  field.getColumn().getId()).convertToClassicString(propValue);
            } else {
              value = (String) jsContent.get(inpColName);
            }
            RequestContext.get().setRequestParameter(inpColName, value);
          }
        } catch (Exception e) {
          log.error("Couldn't read column value from the request for column " + inpColName, e);
        }
      }
    }

    // We also add special parameters such as the one set by selectors to the request, so the
    // callouts can use them
    addSpecialParameters(tab, jsContent);
  }

  @SuppressWarnings("unchecked")
  private void addSpecialParameters(Tab tab, JSONObject jsContent) {
    Iterator it = jsContent.keys();
    while (it.hasNext()) {
      String key = it.next().toString();
      try {
        if (RequestContext.get().getRequestParameter(key) == null) {
          RequestContext.get().setRequestParameter(key, jsContent.getString(key));
        }
      } catch (JSONException e) {
        log.error("Couldn't read parameter from the request: " + key, e);
      }
    }

    RequestContext.get().setRequestParameter("inpwindowId", tab.getWindow().getId());
    RequestContext.get().setRequestParameter("inpTabId", tab.getId());
  }

  private boolean referencedEntityIsParent(BaseOBObject parentRecord, Field field) {
    Entity parentEntity = parentRecord.getEntity();
    Entity entity = ModelProvider.getInstance().getEntityByTableId(
        field.getTab().getTable().getId());
    Property property = entity.getPropertyByColumnName(field.getColumn().getDBColumnName());
    Entity referencedEntity = property.getReferencedProperty().getEntity();
    return referencedEntity.equals(parentEntity);
  }

  private void computeListOfColumnsSortedByValidationDependencies(Tab tab,
      List<String> sortedColumns, Map<String, List<String>> columnsInValidation,
      List<String> changeEventCols) {
    List<Field> fields = tab.getADFieldList();
    ArrayList<String> columns = new ArrayList<String>();
    List<String> columnsWithValidation = new ArrayList<String>();
    HashMap<String, String> validations = new HashMap<String, String>();
    for (Field field : fields) {
      String columnName = field.getColumn().getDBColumnName();
      columns.add(columnName.toUpperCase());
      if (field.getColumn().getValidation() != null) {
        columnsWithValidation.add(field.getColumn().getDBColumnName());
        validations.put(field.getColumn().getDBColumnName(), getValidation(field));
      }
    }
    for (String column : columnsWithValidation) {
      columnsInValidation.put(column, parseValidation(column, validations.get(column), columns));
      String cols = "";
      for (String col : columnsInValidation.get(column)) {
        cols += col + ",";
      }
      log.debug("Column: " + column);
      log.debug("Validation: '" + validations.get(column) + "'");
      log.debug("Columns in validation: '" + cols + "'");
    }

    // Add client and org first to compute dependencies correctly
    for (Field field : fields) {
      String colName = field.getColumn().getDBColumnName();
      if (colName.equalsIgnoreCase("Ad_Client_Id")) {
        sortedColumns.add(colName);
      }
    }
    for (Field field : fields) {
      String colName = field.getColumn().getDBColumnName();
      if (colName.equalsIgnoreCase("Ad_Org_Id")) {
        sortedColumns.add(colName);
      }
    }
    // we add the columns not included in the sortedColumns
    // (the ones which don't have validations)
    for (Field field : fields) {
      String colName = field.getColumn().getDBColumnName();
      if (field.getColumn().getValidation() == null && !sortedColumns.contains(colName)) {
        sortedColumns.add(colName);
      }
    }
    String nonDepColumn = pickNonDependantColumn(sortedColumns, columnsWithValidation,
        columnsInValidation);
    while (nonDepColumn != null) {
      sortedColumns.add(nonDepColumn);
      nonDepColumn = pickNonDependantColumn(sortedColumns, columnsWithValidation,
          columnsInValidation);
    }

    String cycleCols = "";
    for (String col : columnsWithValidation) {
      if (!sortedColumns.contains(col)) {
        cycleCols += "," + col;
      }
    }
    if (!cycleCols.equals("")) {
      throw new OBException("Error. The columns " + cycleCols.substring(1)
          + " have validations which form a cycle.");
    }
    String finalCols = "";
    for (String col : sortedColumns) {
      finalCols += col + ",";
    }
    log.debug("Final order of column computation: " + finalCols);

    // We also fill the changeEventCols
    // These are the columns which should trigger a CHANGE request to the FIC (because either they
    // require a combo reload because they are used in a validation, or there is a callout
    // associated with them)
    for (Field field : fields) {
      String column = field.getColumn().getDBColumnName();
      if (columnsInValidation.get(column) != null && columnsInValidation.get(column).size() > 0) {
        for (String colInVal : columnsInValidation.get(column)) {
          final String columnName = "inp" + Sqlc.TransformaNombreColumna(colInVal);
          if (!changeEventCols.contains(columnName)) {
            changeEventCols.add(columnName);
          }
        }
      }
    }
  }

  private void setValueOfColumnInRequest(BaseOBObject obj, String columnName) {
    Entity entity = obj.getEntity();
    Property prop = entity.getPropertyByColumnName(columnName);
    Object currentValue = obj.get(prop.getName());

    if (currentValue != null) {
      if (currentValue instanceof BaseOBObject) {
        if (prop.getReferencedProperty() != null) {
          currentValue = ((BaseOBObject) currentValue).get(prop.getReferencedProperty().getName());
        } else {
          currentValue = ((BaseOBObject) currentValue).getId();
        }
      } else {
        currentValue = UIDefinitionController.getInstance().getUIDefinition(prop.getColumnId())
            .convertToClassicString(currentValue);
      }
      RequestContext.get().setRequestParameter("inp" + Sqlc.TransformaNombreColumna(columnName),
          currentValue.toString());
    }
  }

  private void setSessionValues(BaseOBObject object, Tab tab) {
    for (Column col : tab.getTable().getADColumnList()) {
      if (col.isStoredInSession()) {
        Property prop = object.getEntity().getPropertyByColumnName(col.getDBColumnName());
        Object value = object.get(prop.getName());
        if (value != null) {
          if (value instanceof BaseOBObject) {
            if (prop.getReferencedProperty() != null) {
              value = ((BaseOBObject) value).get(prop.getReferencedProperty().getName());
            } else {
              value = ((BaseOBObject) value).getId();
            }
          } else {
            value = UIDefinitionController.getInstance().getUIDefinition(col.getId())
                .convertToClassicString(value);
          }
          setSessionValue(tab.getWindow().getId() + "|" + col.getDBColumnName(), value);
        }
        // We also set the value of every column in the RequestContext so that it is available for
        // the Auxiliary Input computation
        setValueOfColumnInRequest(object, col.getDBColumnName());
      }
    }
    OBCriteria<AuxiliaryInput> auxInC = OBDal.getInstance().createCriteria(AuxiliaryInput.class);
    auxInC.add(Expression.eq(AuxiliaryInput.PROPERTY_TAB, tab));
    List<AuxiliaryInput> auxInputs = auxInC.list();
    for (AuxiliaryInput auxIn : auxInputs) {
      Object value = computeAuxiliaryInput(auxIn, tab.getWindow().getId());
      setSessionValue(tab.getWindow().getId() + "|" + auxIn.getName(), value);
    }
    Tab parentTab = KernelUtils.getInstance().getParentTab(tab);
    BaseOBObject parentRecord = KernelUtils.getInstance().getParentRecord(object, tab);
    if (parentTab != null && parentRecord != null) {
      setSessionValues(parentRecord, parentTab);
    }
  }

  private void setSessionValue(String key, Object value) {
    log.debug("Setting session value. Key: " + key + "  Value:" + value);
    RequestContext.get().setSessionAttribute(key, value);
  }

  private void setRequestContextParameter(Field field, JSONObject jsonObj) {
    try {
      String fieldId = "inp" + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName());
      RequestContext.get().setRequestParameter(fieldId,
          jsonObj.has("classicValue") ? jsonObj.getString("classicValue") : null);
    } catch (JSONException e) {
      log.error("Couldn't read JSON parameter for column " + field.getColumn().getDBColumnName());
    }
  }

  private HashMap<String, Field> buildInpField(List<Field> fields) {
    HashMap<String, Field> inpFields = new HashMap<String, Field>();
    for (Field field : fields) {
      inpFields.put("inp" + Sqlc.TransformaNombreColumna(field.getColumn().getDBColumnName()),
          field);
    }
    return inpFields;
  }

  private void executeCallouts(String mode, Tab tab, Map<String, JSONObject> columnValues,
      String changedColumn, List<String> calloutsToCall, List<String> lastfieldChanged,
      List<String> messages) {

    // In CHANGE mode, we will add the initial callout call for the changed column, if there is
    // one
    if (mode.equals("CHANGE")) {
      if (changedColumn != null) {
        for (Column col : tab.getTable().getADColumnList()) {
          if (("inp" + Sqlc.TransformaNombreColumna(col.getDBColumnName())).equals(changedColumn)) {
            if (col.getCallout() != null) {
              // The column has a callout. We will add the callout to the callout list
              addCalloutToList(col, calloutsToCall, lastfieldChanged);
            }
          }
        }
      }
    }

    ArrayList<String> calledCallouts = new ArrayList<String>();
    runCallouts(columnValues, tab, calledCallouts, calloutsToCall, lastfieldChanged, messages);
  }

  private void runCallouts(Map<String, JSONObject> columnValues, Tab tab,
      List<String> calledCallouts, List<String> calloutsToCall, List<String> lastfieldChangedList,
      List<String> messages) {

    List<Field> fields = tab.getADFieldList();
    HashMap<String, Field> inpFields = buildInpField(fields);

    while (!calloutsToCall.isEmpty() && calledCallouts.size() < MAX_CALLOUT_CALLS) {
      String calloutClassName = calloutsToCall.get(0);
      String lastFieldChanged = lastfieldChangedList.get(0);
      log.debug("Calling callout " + calloutClassName);
      try {
        Class<?> calloutClass = Class.forName(calloutClassName);
        calloutsToCall.remove(calloutClassName);
        lastfieldChangedList.remove(lastFieldChanged);
        Object calloutInstance = calloutClass.newInstance();
        Method method = null;
        Method init = null;
        Method service = null;
        for (Method m : calloutClass.getMethods()) {
          if (m.getName().equals("doPost")) {
            method = m;
          }
          if (m.getName().equals("init") && m.getParameterTypes().length == 1) {
            init = m;
          }
          if (m.getName().equals("service")) {
            service = m;
          }
        }

        if (method == null || init == null || service == null) {
          log.error("Couldn't find method in Callout " + calloutClassName);
        } else {
          RequestContext rq = RequestContext.get();

          RequestContext.get().setRequestParameter("inpLastFieldChanged", lastFieldChanged);

          // We then execute the callout
          CalloutServletConfig config = new CalloutServletConfig(calloutClassName, RequestContext
              .getServletContext());
          Object[] initArgs = { config };
          init.invoke(calloutInstance, initArgs);
          CalloutHttpServletResponse fakeResponse = new CalloutHttpServletResponse(rq.getResponse());
          Object[] arguments = { rq.getRequest(), fakeResponse };
          service.invoke(calloutInstance, arguments);
          method.invoke(calloutInstance, arguments);
          String calloutResponse = fakeResponse.getOutputFromWriter();

          // Now we parse the callout response and modify the stored values of the columns modified
          // by the callout
          ArrayList<NativeArray> returnedArray = new ArrayList<NativeArray>();
          String calloutNameJS = parseCalloutResponse(calloutResponse, returnedArray);
          if (calloutNameJS != null && calloutNameJS != "") {
            calledCallouts.add(calloutNameJS);
          }
          if (returnedArray.size() > 0) {
            for (NativeArray element : returnedArray) {
              String name = (String) element.get(0, null);
              if (name.equals("MESSAGE")) {
                log.debug("Callout message: " + element.get(1, null));
                messages.add(element.get(1, null).toString());
              } else {
                if (name.startsWith("inp")) {
                  boolean changed = false;
                  if (inpFields.containsKey(name)) {
                    Column col = inpFields.get(name).getColumn();
                    String colId = "inp" + Sqlc.TransformaNombreColumna(col.getDBColumnName());
                    if (element.get(1, null) instanceof NativeArray) {
                      // Combo data
                      NativeArray subelements = (NativeArray) element.get(1, null);
                      for (int j = 0; j < subelements.getLength(); j++) {
                        NativeArray subelement = (NativeArray) subelements.get(j, null);
                        if (subelement.get(2, null).toString().equalsIgnoreCase("True")) {
                          String value = subelement.get(0, null).toString();
                          log.debug("Column: " + col.getDBColumnName() + "  Value: " + value);
                          String oldValue = rq.getRequestParameter(colId);
                          rq.setRequestParameter(colId, value);
                          UIDefinition uiDef = UIDefinitionController.getInstance()
                              .getUIDefinition(col.getId());
                          JSONObject jsonobject = new JSONObject(uiDef.getFieldProperties(inpFields
                              .get(name), true));
                          if (jsonobject.has("classicValue")) {
                            String newValue = jsonobject.getString("classicValue");
                            if ((oldValue == null && newValue != null)
                                || (oldValue != null && newValue == null)
                                || (oldValue != null && newValue != null && !oldValue
                                    .equals(newValue))) {
                              columnValues.put(colId, jsonobject);
                              changed = true;
                            } else {
                              log
                                  .debug("Column value didn't change. We do not attempt to execute any additional callout");
                            }
                          }
                        }
                      }
                    } else {
                      // Normal data
                      Object el = element.get(1, null);
                      String value;
                      if (el instanceof Number) {
                        value = ((Number) el).toString();
                      } else {
                        value = (String) el;
                      }
                      String oldValue = rq.getRequestParameter(colId);
                      // We set the new value in the request, so that the JSONObject is computed
                      // with the new value
                      rq.setRequestParameter(colId, value);
                      UIDefinition uiDef = UIDefinitionController.getInstance().getUIDefinition(
                          col.getId());
                      JSONObject jsonobj = new JSONObject(uiDef.getFieldProperties(inpFields
                          .get(name), true));
                      if (jsonobj.has("classicValue")) {
                        String newValue = jsonobj.getString("classicValue");
                        log
                            .debug("Modified column: " + col.getDBColumnName() + "  Value: "
                                + value);
                        if ((oldValue == null && newValue != null)
                            || (oldValue != null && newValue == null)
                            || (oldValue != null && newValue != null && !oldValue.equals(newValue))) {
                          columnValues.put("inp"
                              + Sqlc.TransformaNombreColumna(col.getDBColumnName()), jsonobj);
                          changed = true;

                          rq.setRequestParameter(colId, jsonobj.getString("classicValue"));
                        }
                      } else {
                        log
                            .debug("Column value didn't change. We do not attempt to execute any additional callout");
                      }
                    }
                    if (changed && col.getCallout() != null) {
                      // We need to fire this callout, as the column value was changed
                      addCalloutToList(col, calloutsToCall, lastfieldChangedList);
                    }
                  }
                }
              }
            }
          }
        }
      } catch (ClassNotFoundException e) {
        throw new OBException("Couldn't find class " + calloutClassName, e);
      } catch (Exception e) {
        throw new OBException("Couldn't execute callout (class " + calloutClassName + ")", e);
      }
    }
    if (calledCallouts.size() == MAX_CALLOUT_CALLS) {
      log.warn("Warning: maximum number of callout calls reached");
    }

  }

  private void addCalloutToList(Column col, List<String> listOfCallouts,
      List<String> lastFieldChangedList) {
    if (col.getCallout().getADModelImplementationList() == null
        || col.getCallout().getADModelImplementationList().size() == 0) {
      log.error("The callout of the column " + col.getDBColumnName()
          + " doesn't have a corresponding model object, and therefore cannot be executed.");
    } else {
      String calloutClassNameToCall = col.getCallout().getADModelImplementationList().get(0)
          .getJavaClassName();
      listOfCallouts.add(calloutClassNameToCall);
      lastFieldChangedList.add("inp" + Sqlc.TransformaNombreColumna(col.getDBColumnName()));
    }
  }

  private String parseCalloutResponse(String calloutResponse, List<NativeArray> returnedArray) {
    String initS = "id=\"paramArray\">";
    String resp = calloutResponse.substring(calloutResponse.indexOf(initS) + initS.length());
    resp = resp.substring(0, resp.indexOf("</")).trim();
    if (!resp.contains("new Array(")) {
      return null;
    }
    Context cx = Context.enter();
    Scriptable scope = cx.initStandardObjects();
    cx.evaluateString(scope, resp, "<cmd>", 1, null);
    try {
      NativeArray array = (NativeArray) scope.get("respuesta", scope);
      Object calloutName = scope.get("calloutName", scope);
      String calloutNameS = calloutName == null ? null : calloutName.toString();
      log.debug("Callout Name: " + calloutNameS);
      for (int i = 0; i < array.getLength(); i++) {
        returnedArray.add((NativeArray) array.get(i, null));
      }
      return calloutNameS;
    } catch (Exception e) {
      log.error("Couldn't parse callout response. The parsed response was: " + resp, e);
    }
    return null;
  }

  private String pickNonDependantColumn(List<String> sortedColumns, List<String> columns,
      Map<String, List<String>> columnsInValidation) {
    for (String col : columns) {
      if (sortedColumns.contains(col)) {
        continue;
      }
      if (columnsInValidation.get(col) == null || columnsInValidation.get(col).isEmpty()) {
        return col;
      }
      boolean allColsSorted = true;
      for (String depCol : columnsInValidation.get(col)) {
        if (!containsIgnoreCase(sortedColumns, depCol))
          allColsSorted = false;
      }
      if (allColsSorted)
        return col;
    }

    return null;
  }

  private boolean containsIgnoreCase(List<String> list, String element) {
    for (String e : list) {
      if (e.equalsIgnoreCase(element)) {
        return true;
      }
    }
    return false;
  }

  private String getValidation(Field field) {
    Column c = field.getColumn();
    String val = c.getValidation().getValidationCode();
    if (c.getReference().getId().equals("18")) {
      if (c.getReferenceSearchKey() != null) {
        for (ReferencedTable t : c.getReferenceSearchKey().getADReferencedTableList()) {
          val += " AND " + t.getSQLWhereClause();
        }
      }
    }
    return val;

  }

  private ArrayList<String> parseValidation(String column, String validation,
      List<String> possibleColumns) {
    String token = validation;
    ArrayList<String> columns = new ArrayList<String>();
    int i = token.indexOf("@");
    while (i != -1) {
      token = token.substring(i + 1);
      if (!token.startsWith("SQL")) {
        i = token.indexOf("@");
        if (i != -1) {
          String strAux = token.substring(0, i);
          token = token.substring(i + 1);
          if (!columns.contains(strAux)) {
            if (!strAux.equalsIgnoreCase(column) && possibleColumns.contains(strAux.toUpperCase())) {
              columns.add(strAux);
            }
          }
        }
      }
      i = token.indexOf("@");
    }
    return columns;
  }

  private Object computeAuxiliaryInput(AuxiliaryInput auxIn, String windowId) {
    try {
      String code = auxIn.getValidationCode();
      log.debug("Auxiliary Input: " + auxIn.getName() + " Code:" + code);
      String fvalue = null;
      if (code.startsWith("@SQL=")) {
        ArrayList<String> params = new ArrayList<String>();
        String sql = UIDefinition.parseSQL(code, params);
        // final StringBuffer parametros = new StringBuffer();
        // for (final Enumeration<String> e = params.elements(); e.hasMoreElements();) {
        // String paramsElement = WadUtility.getWhereParameter(e.nextElement(), true);
        // parametros.append("\n" + paramsElement);
        // }
        log.debug("Transformed SQL code: " + sql);
        int indP = 1;
        PreparedStatement ps = OBDal.getInstance().getConnection().prepareStatement(sql);
        for (String parameter : params) {
          String value = "";
          if (parameter.substring(0, 1).equals("#")) {
            value = Utility.getContext(new DalConnectionProvider(false), RequestContext.get()
                .getVariablesSecureApp(), parameter, windowId);
          } else {
            String fieldId = "inp" + Sqlc.TransformaNombreColumna(parameter);
            value = RequestContext.get().getRequestParameter(fieldId);
          }
          log.debug("Parameter: " + parameter + ": Value " + value);
          ps.setObject(indP++, value);
        }
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
          fvalue = rs.getString(1);
        }
      } else if (code.contains("@")) {
        fvalue = Utility.getContext(new DalConnectionProvider(false), RequestContext.get()
            .getVariablesSecureApp(), code, windowId);
      } else {
        fvalue = code;
      }
      return fvalue;
    } catch (Exception e) {
      log.error("Error while computing auxiliary input parameter: " + auxIn.getName()
          + " from tab: " + auxIn.getTab().getName(), e);
    }
    return null;
  }
}
