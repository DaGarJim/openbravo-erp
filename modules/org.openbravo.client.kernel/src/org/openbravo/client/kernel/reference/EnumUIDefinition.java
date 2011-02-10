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
 * All portions are Copyright (C) 2010 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.kernel.reference;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.model.ad.ui.Field;

/**
 * Implementation of the enum ui definition.
 * 
 * @author mtaal
 */
public class EnumUIDefinition extends UIDefinition {

  @Override
  public String getParentType() {
    return "enum";
  }

  @Override
  public String getFormEditorType() {
    return "OBListItem";
  }

  @Override
  public String getFilterEditorType() {
    return "OBListFilterTextItem";
  }

  @Override
  public String getFieldProperties(Field field, boolean getValueFromSession) {
    JSONObject value;
    try {
      value = new JSONObject(super.getFieldProperties(field, getValueFromSession));
      if (!field.isDisplayed()) {
        return value.toString();
      }
      return getValueInComboReference(field, getValueFromSession, value.has("classicValue") ? value
          .getString("classicValue") : "");
    } catch (JSONException e) {
      throw new OBException("Error while computing combo data", e);
    }
  }

  @Override
  public String getFilterEditorProperties(Field field) {
    return ", filterOnKeypress: true" + super.getFilterEditorProperties(field);
  }

}
