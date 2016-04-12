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
 * All portions are Copyright (C) 2011-2016 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.attachment;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.util.Check;
import org.openbravo.client.application.Parameter;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.utility.Attachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttachmentAH extends BaseActionHandler {

  private static final Logger log = LoggerFactory.getLogger(AttachmentAH.class);

  @Inject
  private AttachImplementationManager aim;

  @Inject
  private ApplicationDictionaryCachedStructures adcs;

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    OBContext.setAdminMode();
    String tabId = (String) parameters.get("tabId");
    Check.isNotNull(tabId, OBMessageUtils.messageBD("OBUIAPP_Attachment_Tab_Mandatory"));
    Tab tab = adcs.getTab(tabId);

    String recordIds = "";
    try {
      final JSONObject request = new JSONObject(content);
      String command = (String) parameters.get("Command");

      if ("EDIT".equals(command)) {
        JSONObject params = request.getJSONObject("_params");
        recordIds = params.getString("inpKey");
        final String attachmentId = (String) parameters.get("attachmentId");
        String strAttMethodId = (String) parameters.get("attachmentMethod");
        if (StringUtils.isBlank(strAttMethodId)) {
          strAttMethodId = AttachmentUtils.DEFAULT_METHOD_ID;
        }
        Map<String, String> requestParams = fixRequestMap(parameters, request);
        for (Parameter param : adcs.getMethodMetadataParameters(strAttMethodId, tabId)) {
          if (param.isFixed()) {
            continue;
          }
          String value;
          if (params.has(param.getDBColumnName())
              && params.get(param.getDBColumnName()) != JSONObject.NULL) {
            value = URLDecoder.decode(params.getString(param.getDBColumnName()), "UTF-8");
          } else {
            value = null;
          }

          requestParams.put(param.getId(), value);
        }

        aim.update(requestParams, attachmentId, tabId);

        JSONObject obj = getAttachmentJSONObject(tab, recordIds);
        obj.put("buttonId", params.getString("buttonId"));
        return obj;
      } else if ("DELETE".equals(command)) {

        recordIds = parameters.get("recordIds").toString();
        String attachmentId = (String) parameters.get("attachId");

        String tableId = (String) DalUtil.getId(tab.getTable());

        OBCriteria<Attachment> attachmentFiles = OBDao.getFilteredCriteria(Attachment.class,
            Restrictions.eq("table.id", tableId), Restrictions.in("record", recordIds.split(",")));
        // do not filter by the attachment's organization
        // if the user has access to the record where the file its attached, it has access to all
        // its attachments
        attachmentFiles.setFilterOnReadableOrganization(false);
        if (attachmentId != null) {
          attachmentFiles.add(Restrictions.eq(Attachment.PROPERTY_ID, attachmentId));
        }
        for (Attachment attachment : attachmentFiles.list()) {
          aim.delete(attachment);

        }
        JSONObject obj = getAttachmentJSONObject(tab, recordIds);
        obj.put("buttonId", parameters.get("buttonId"));
        return obj;
      } else {
        return new JSONObject();
      }
    } catch (JSONException e) {
      throw new OBException("Error while removing file", e);
    } catch (UnsupportedEncodingException e) {
      throw new OBException("Error decoding parameter", e);
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(e.getMessage());
      JSONObject obj = getAttachmentJSONObject(tab, recordIds);
      try {
        obj.put("buttonId", parameters.get("buttonId"));
        obj.put("viewId", parameters.get("viewId"));
        obj.put("status", -1);
        obj.put("errorMessage", e.getMessage());
      } catch (Exception ex) {
        // do nothing
      }

      return obj;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static JSONObject getAttachmentJSONObject(Tab tab, String recordIds) {
    List<JSONObject> attachments = AttachmentUtils.getTabAttachmentsForRows(tab,
        recordIds.split(","));
    JSONObject jsonobj = new JSONObject();
    try {
      jsonobj.put("attachments", new JSONArray(attachments));
    } catch (JSONException e) {
      throw new OBException(e);
    }
    return jsonobj;

  }
}