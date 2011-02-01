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
import org.openbravo.service.json.JsonConstants;

/**
 * Implementation of the foreign key ui definition which uses a combo box for its input/filter
 * types.
 * 
 * @author mtaal
 */
public class FKComboUIDefinition extends ForeignKeyUIDefinition {

  public String getGridEditorFieldProperties(Field field) {
    return "displayField: '" + JsonConstants.IDENTIFIER + "', valueField: '" + JsonConstants.ID
        + "'" + super.getGridEditorFieldProperties(field);
  }

  @Override
  public String getFieldProperties(Field field, boolean getValueFromSession) {
    JSONObject value;
    try {
      value = new JSONObject(super.getFieldProperties(field, getValueFromSession));
      if (!field.isDisplayed()) {
        return value.toString();
      }
      return getValueInComboReference(field, getValueFromSession, value.getString("classicValue"));
    } catch (JSONException e) {
      throw new OBException("Error while computing combo data", e);
    }
  }

}
