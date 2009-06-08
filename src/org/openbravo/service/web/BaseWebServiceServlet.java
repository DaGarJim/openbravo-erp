/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SL 
 * All portions are Copyright (C) 2008 Openbravo SL 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.service.web;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.util.Check;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.model.ad.access.User;
import org.openbravo.utils.CryptoSHA1BASE64;

/**
 * This servlet has two main responsibilities: 1) authenticate, 2) set the correct {@link OBContext}
 * , and 3) translate Exceptions into the correct Http response code.
 * <p/>
 * In regard to authentication: there is support for basic-authentication as well as url parameter
 * based authentication.
 * 
 * @author mtaal
 */

public class BaseWebServiceServlet extends HttpServlet {

  public static final String LOGIN_PARAM = "l";
  public static final String PASSWORD_PARAM = "p";

  private static final long serialVersionUID = 1L;

  @Override
  protected void service(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    // already logged in?
    if (OBContext.getOBContext() != null) {
      doService(request, response);
      // do the login action
    } else if (isLoggedIn(request, response)) {
      doService(request, response);
    } else {
      response.setHeader("WWW-Authenticate", "Basic realm=\"Openbravo\"");
      response.setStatus(401);
    }
  }

  private void doService(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    try {
      super.service(request, response);
      response.setStatus(200);
    } catch (final InvalidRequestException e) {
      SessionHandler.getInstance().setDoRollback(true);
      e.printStackTrace(System.err);
      response.setStatus(400);
      final Writer w = response.getWriter();
      w.write(WebServiceUtil.getInstance().createErrorXML(e));
      w.close();
    } catch (final InvalidContentException e) {
      SessionHandler.getInstance().setDoRollback(true);
      e.printStackTrace(System.err);
      response.setStatus(409);
      final Writer w = response.getWriter();
      w.write(WebServiceUtil.getInstance().createErrorXML(e));
      w.close();
    } catch (final ResourceNotFoundException e) {
      SessionHandler.getInstance().setDoRollback(true);
      e.printStackTrace(System.err);
      response.setStatus(404);
      final Writer w = response.getWriter();
      w.write(WebServiceUtil.getInstance().createErrorXML(e));
      w.close();
    } catch (final OBSecurityException e) {
      SessionHandler.getInstance().setDoRollback(true);
      e.printStackTrace(System.err);
      response.setStatus(401);
      final Writer w = response.getWriter();
      w.write(WebServiceUtil.getInstance().createErrorXML(e));
      w.close();
    } catch (final Throwable t) {
      SessionHandler.getInstance().setDoRollback(true);
      t.printStackTrace(System.err);
      response.setStatus(500);
      final Writer w = response.getWriter();
      w.write(WebServiceUtil.getInstance().createErrorXML(t));
      w.close();
    }
  }

  private boolean isLoggedIn(HttpServletRequest request, HttpServletResponse response) {
    final String login = request.getParameter(LOGIN_PARAM);
    final String password = request.getParameter(PASSWORD_PARAM);
    String userId = null;
    if (login != null && password != null) {
      userId = getValidUserId(login, password);
    } else { // use basic authentication
      userId = doBasicAuthentication(request);
    }
    if (userId != null) {
      OBContext.setOBContext(UserContextCache.getInstance().getCreateOBContext(userId));
      OBContext.setOBContextInSession(request, OBContext.getOBContext());
      return true;
    } else {
      return false;
    }
  }

  private String doBasicAuthentication(HttpServletRequest request) {
    try {
      final String auth = request.getHeader("Authorization");
      if (auth == null) {
        return null;
      }
      if (!auth.toUpperCase().startsWith("BASIC ")) {
        return null; // only BASIC supported
      }

      // user and password come after BASIC
      final String userpassEncoded = auth.substring(6);

      // Decode it, using any base 64 decoder
      final String decodedUserPass = new String(Base64.decodeBase64(userpassEncoded.getBytes()));
      final int index = decodedUserPass.indexOf(":");
      if (index == -1) {
        return null;
      }
      final String login = decodedUserPass.substring(0, index);
      final String password = decodedUserPass.substring(index + 1);
      return getValidUserId(login, password);
    } catch (final Exception e) {
      throw new OBException(e);
    }
  }

  private String getValidUserId(String login, String password) {
    try {
      final String encodedPwd = CryptoSHA1BASE64.hash(password);
      // now search with the login and password
      final Query qry = SessionHandler.getInstance().createQuery(
          "select id from " + User.class.getName() + " where username=? and password=?");
      qry.setParameter(0, login);
      qry.setParameter(1, encodedPwd);
      final List<?> list = qry.list();
      Check.isTrue(list.size() == 0 || list.size() == 1, "Zero or one user expected for login "
          + login + " but found " + list.size() + " users ");
      if (list.size() == 0) {
        return null;
      }
      return (String) list.get(0);
    } catch (final Exception e) {
      throw new OBException(e);
    }
  }
}