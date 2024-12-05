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

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.openbravo.email.EmailEventContentGenerator;
import org.openbravo.model.common.enterprise.EmailTemplate;

/**
 * Email generator for {@link ForgotPasswordService#EVT_FORGOT_PASSWORD} event which is triggered
 * when the {@link ForgotPasswordService} is executed
 * 
 * @author ander.flores
 * 
 */
public class ForgotPasswordEmailGenerator implements EmailEventContentGenerator {

  @Inject
  private ForgotPasswordEmailBody body;

  @SuppressWarnings("unchecked")
  @Override
  public String getSubject(Object data, String event) {
    body.setParameters((Map<String, Object>) data);
    EmailTemplate emailTemplate = body.getComponentEmailTemplate();
    return emailTemplate.getSubject();
  }

  @SuppressWarnings("unchecked")
  @Override
  public String getBody(Object data, String event) {
    body.setParameters((Map<String, Object>) data);
    return body.generate();
  }

  @Override
  public String getContentType() { // check isHTML
    return "text/plain; charset=utf-8";
  }

  @Override
  public boolean isValidEvent(String event, Object data) {
    return ForgotPasswordService.EVT_FORGOT_PASSWORD.equals(event);
  }

  @Override
  public int getPriority() {
    return 100;
  }

  @Override
  public boolean preventsOthersExecution() {
    return false;
  }

  @Override
  public boolean isAsynchronous() {
    return false;
  }

  @Override
  public List<File> getAttachments(Object data, String event) {
    return null;
  }
}
