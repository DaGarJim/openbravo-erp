/*
 ************************************************************************************
 * Copyright (C) 2024 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at https://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
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
