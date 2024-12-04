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

package org.openbravo.authentication;

import java.io.IOException;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.Tuple;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.authentication.hashing.PasswordHash;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.service.password.PasswordStrengthChecker;

public class ResetPasswordWithTokenService extends HttpServlet {

  private static final long serialVersionUID = 1L;
  private static final Logger log = LogManager.getLogger();

  @Inject
  private PasswordStrengthChecker passwordStrengthChecker;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    JSONObject result = new JSONObject();
    try {
      OBContext.setAdminMode(true);
      JSONObject body = new JSONObject(
          request.getReader().lines().collect(Collectors.joining(System.lineSeparator())));

      String token = body.optString("token");
      String newPwd = body.optString("newPassword");
      if (!passwordStrengthChecker.isStrongPassword(newPwd)) {
        throw new ChangePasswordException(
            OBMessageUtils.getI18NMessage("CPPasswordNotStrongEnough"));
      }

      String hql_token = "select userContact.id, redeemed, creationDate from ADUserPwdResetToken where usertoken = :token";

      Tuple tokenEntry = OBDal.getInstance()
          .getSession()
          .createQuery(hql_token, Tuple.class)
          .setParameter("token", token)
          .uniqueResult();

      if (tokenEntry == null) {
        throw new ChangePasswordException(
            OBMessageUtils.getI18NMessage("NoSpecificEntryToken", new String[] { token }));
      }

      String userId = tokenEntry.get(0, String.class);
      Boolean isRedeemed = tokenEntry.get(1, Boolean.class);
      Timestamp creationDate = tokenEntry.get(2, Timestamp.class);

      if (!checkExpirationOfToken(creationDate, isRedeemed)) {
        throw new ChangePasswordException(OBMessageUtils.getI18NMessage("PasswordTokenExpired"));
      }

      updateIsRedeemedValue(token);
      User user = OBDal.getInstance().get(User.class, userId);
      user.setPassword(PasswordHash.generateHash(newPwd));
      OBDal.getInstance().flush();

    } catch (ChangePasswordException ex) {
      log.info("Error while validating the reset password request: " + ex.getMessage());
      try {
        result = new JSONObject(Map.of("error", generateError(ex.getMessage())));
      } catch (JSONException e) {
        // Should not happen
      }
    } catch (JSONException ex) {
      log.error("Error parsing JSON", ex);
      result = new JSONObject(Map.of("error", ex.getMessage()));
    } catch (Exception ex) {
      log.error("Error processing ", ex);
      result = new JSONObject(Map.of("error", ex.getMessage()));
    } finally {
      OBContext.restorePreviousMode();
    }
    writeResult(response, new JSONObject(Map.of("response", result)).toString());
  }

  private boolean checkExpirationOfToken(Timestamp creationDate, boolean isRedeemed) {
    Date tokenDate = new Date(creationDate.getTime());
    Date now = new Date();
    long differenceInSeconds = (now.getTime() - tokenDate.getTime()) / 1000;
    boolean isWithinFifteenMinutes = differenceInSeconds < 15 * 60;
    return !isRedeemed && isWithinFifteenMinutes;
  }

  private JSONObject generateError(String errorMsg) throws JSONException {
    JSONObject error = new JSONObject();
    JSONObject errorResponse = new JSONObject();
    errorResponse.put("messageTitle", OBMessageUtils.getI18NMessage("PasswordGenerationError"));
    errorResponse.put("messageText", errorMsg);
    error.put("response", errorResponse);
    return error;
  }

  private int updateIsRedeemedValue(String token) {
    String hql = "UPDATE ADUserPwdResetToken SET redeemed = 'Y' WHERE usertoken = :token ";

    return OBDal.getInstance()
        .getSession()
        .createQuery(hql)
        .setParameter("token", token)
        .executeUpdate();
  }

  private void writeResult(HttpServletResponse response, String result) throws IOException {
    response.setContentType("application/json;charset=UTF-8");
    response.setHeader("Content-Type", "application/json;charset=UTF-8");

    final Writer w = response.getWriter();
    w.write(result);
    w.close();
  }
}
