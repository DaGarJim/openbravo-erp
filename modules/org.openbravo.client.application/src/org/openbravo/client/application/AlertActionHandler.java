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
 * All portions are Copyright (C) 2009-2011 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.client.kernel.StaticResourceComponent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.UsedByLink;
import org.openbravo.model.ad.access.Session;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.service.json.DataResolvingMode;
import org.openbravo.service.json.DataToJsonConverter;
import org.openbravo.service.json.JsonConstants;

/**
 * Action handler checks if there are alerts and if so returns these as a json object.
 * 
 * Action handler also updates the session ping value in the database.
 * 
 * @author mtaal
 * @see StaticResourceComponent
 */
@ApplicationScoped
public class AlertActionHandler extends BaseActionHandler {
  private static final Logger log4j = Logger.getLogger(AlertActionHandler.class);

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.openbravo.client.kernel.BaseActionHandler#execute(javax.servlet.http.HttpServletRequest,
   * javax.servlet.http.HttpServletResponse)
   */
  @Override
  public void execute() {
    OBContext.setAdminMode();
    try {
      final HttpServletRequest request = RequestContext.get().getRequest();
      final HttpServletResponse response = RequestContext.get().getResponse();
      final HttpSession session = request.getSession(false);
      // update the session
      if (session != null) {
        final String dbSessionId = (String) session.getAttribute("#AD_Session_ID".toUpperCase());
        if (dbSessionId != null) {
          final Session dbSession = OBDal.getInstance().get(Session.class, dbSessionId);
          dbSession.setLastPing(new Date());
          // flush to force commit in admin mode
          OBDal.getInstance().flush();
        }
      }

      final VariablesSecureApp vars = new VariablesSecureApp(request);

      // select the alert rules
      final String hql = "select e.alertRule from  " + AlertRecipient.ENTITY_NAME
          + " e where e.alertRule.active = true and (e.userContact.id=? "
          + " or (e.userContact.id = null and e.role.id = ?))";
      final Query qry = OBDal.getInstance().getSession().createQuery(hql);
      qry.setParameter(0, OBContext.getOBContext().getUser().getId());
      qry.setParameter(1, OBContext.getOBContext().getRole().getId());
      final JSONObject result = new JSONObject();
      final List<JSONObject> alertJsonObjects = new ArrayList<JSONObject>();
      for (Object o : qry.list()) {
        final AlertRule alertRule = (AlertRule) o;
        final String whereClause = new UsedByLink().getWhereClause(vars, "",
            alertRule.getFilterClause() == null ? "" : alertRule.getFilterClause());
        final String sql = "select * from AD_ALERT where COALESCE(to_char(STATUS), 'NEW')='NEW'"
            + " AND AD_CLIENT_ID " + OBDal.getInstance().getReadableClientsInClause()
            + " AND AD_ORG_ID " + OBDal.getInstance().getReadableOrganizationsInClause()
            + " AND AD_ALERTRULE_ID = ? " + (whereClause == null ? "" : whereClause);
        final SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(sql)
            .addEntity(Alert.ENTITY_NAME);
        sqlQuery.setParameter(0, alertRule.getId());
        final DataToJsonConverter converter = new DataToJsonConverter();
        log4j.debug("Alert " + alertRule.getName() + " (" + alertRule.getId() + ") - SQL:'" + sql
            + "' - Rows: " + sqlQuery.list().size());
        for (Object alertObject : sqlQuery.list()) {
          final Alert alert = (Alert) alertObject;
          alertJsonObjects.add(converter.toJsonObject(alert, DataResolvingMode.FULL));
        }
      }
      result.put("cnt", alertJsonObjects.size());
      result.put("data", new JSONArray(alertJsonObjects));
      result.put("result", "success");

      response.setContentType(JsonConstants.JSON_CONTENT_TYPE);
      response.setHeader("Content-Type", JsonConstants.JSON_CONTENT_TYPE);
      response.getWriter().write(result.toString());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  protected JSONObject execute(Map<String, Object> parameters, String data) {
    throw new UnsupportedOperationException();
  }
}
