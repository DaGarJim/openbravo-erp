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
package org.openbravo.client.application;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.personalization.PersonalizationHandler;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.StaticResourceComponent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * Computes different settings which may be user/role specific for a certain window.
 * 
 * @author mtaal
 * @see StaticResourceComponent
 */
@ApplicationScoped
public class WindowSettingsActionHandler extends BaseActionHandler {
  private static final Logger log4j = Logger.getLogger(WindowSettingsActionHandler.class);

  @Inject
  private PersonalizationHandler personalizationHandler;

  protected JSONObject execute(Map<String, Object> parameters, String data) {

    try {
      OBContext.setAdminMode();
      final String windowId = (String) parameters.get("windowId");
      final Window window = OBDal.getInstance().get(Window.class, windowId);
      final String roleId = OBContext.getOBContext().getRole().getId();
      final DalConnectionProvider dalConnectionProvider = new DalConnectionProvider();
      final JSONObject jsonUIPattern = new JSONObject();
      for (Tab tab : window.getADTabList()) {
        final boolean readOnlyAccess = org.openbravo.erpCommon.utility.WindowAccessData
            .hasReadOnlyAccess(dalConnectionProvider, roleId, tab.getId());
        String uiPattern = readOnlyAccess ? "RO" : tab.getUIPattern();
        jsonUIPattern.put(tab.getId(), uiPattern);
      }
      final JSONObject json = new JSONObject();
      json.put("uiPattern", jsonUIPattern);
      final String autoSaveStr = Preferences.getPreferenceValue("Autosave", false, OBContext
          .getOBContext().getCurrentClient(), OBContext.getOBContext().getCurrentOrganization(),
          OBContext.getOBContext().getUser(), OBContext.getOBContext().getRole(), window);
      json.put("autoSave", "Y".equals(autoSaveStr));

      try {
        json.put("personalization", personalizationHandler.getPersonalizationForWindow(window));
      } catch (Throwable t) {
        // be robust about errors in the personalization settings
        log4j.error("Error for window " + window, t);
      }

      final String showConfirmationStr = Preferences.getPreferenceValue("ShowConfirmationDefault",
          false, OBContext.getOBContext().getCurrentClient(), OBContext.getOBContext()
              .getCurrentOrganization(), OBContext.getOBContext().getUser(), OBContext
              .getOBContext().getRole(), window);
      json.put("showAutoSaveConfirmation", "Y".equals(showConfirmationStr));

      return json;
    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
