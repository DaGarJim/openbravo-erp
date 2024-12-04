/*
 ************************************************************************************
 * Copyright (C) 2024 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at https://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.authentication;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.hibernate.criterion.Restrictions;
import org.openbravo.client.kernel.BaseComponent;
import org.openbravo.client.kernel.TemplateProcessor;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.email.EmailEventContentGenerator;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.authentication.EmailType;
import org.openbravo.model.common.enterprise.EmailTemplate;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Convenience class that provides a set of common utilities for email templates to be sent
 * 
 * @see EmailEventContentGenerator
 * @author ander.flores
 */
public class ForgotPasswordEmailBody extends BaseComponent {

  @Inject
  private TemplateProcessor.Registry templateProcessRegistry;

  @Override
  public String generate() {
    OBContext.setAdminMode();
    try {
      final EmailTemplate template = getComponentEmailTemplate();
      processBody(template);
      final TemplateProcessor templateProcessor = templateProcessRegistry.get("OBCLFRE_Freemarker");
      return templateProcessor.process(template, getParameters());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void processBody(final EmailTemplate template) {
    String body = template.getBody();
    body = replaceContentWithParameters(template.getBody(), getParameters());
    template.setBody(body);
  }

  private String replaceContentWithParameters(String str, Map<String, Object> parameters) {
    if (str == null) {
      return str;
    }
    String newString = str;
    for (String key : parameters.keySet()) {
      newString = newString.replaceAll("@" + key + "@", String.valueOf(parameters.get(key)));
    }
    return newString;
  }

  public User getUser() {
    return (User) getParameters().get("user");
  }

  public String getChangePasswordURL() {
    return (String) getParameters().get("changePasswordURL");
  }

  public Organization getOrg() {
    return (Organization) getParameters().get("org");
  }

  public Client getClient() {
    return (Client) getParameters().get("client");
  }

  public EmailTemplate getComponentEmailTemplate() {
    List<EmailTemplate> emailTemplates = OBDal.getInstance()
        .createCriteria(EmailTemplate.class)
        .add(Restrictions.eq(EmailTemplate.PROPERTY_EMAILTYPE,
            OBDal.getInstance().get(EmailType.class, "5209BE52755B49C582F034E9B98B3F33")))
        .addOrderBy(EmailTemplate.PROPERTY_ID, true)
        .setFilterOnActive(true)
        .setFilterOnReadableClients(false)
        .setFilterOnReadableOrganization(false)
        .list();

    if (emailTemplates.isEmpty()) {
      throw new ForgotPasswordException(
          OBMessageUtils.getI18NMessage("NoForgottenPasswordEmailTemplatePresent"));
    }

    Optional<EmailTemplate> emailTemplate = filterEmailTemplates(emailTemplates,
        template -> template.getLanguage().getId().equals(getUser().getDefaultLanguage().getId()));
    if (emailTemplate.isPresent()) {
      return emailTemplate.get();
    }

    emailTemplate = filterEmailTemplates(emailTemplates,
        template -> template.getLanguage().getId().equals(getOrg().getLanguage().getId()));
    if (emailTemplate.isPresent()) {
      return emailTemplate.get();
    }

    emailTemplate = filterEmailTemplates(emailTemplates,
        template -> template.getLanguage().getId().equals(getClient().getLanguage().getId()));
    if (emailTemplate.isPresent()) {
      return emailTemplate.get();
    }

    emailTemplate = filterEmailTemplates(emailTemplates, EmailTemplate::isDefault);
    if (emailTemplate.isPresent()) {
      return emailTemplate.get();
    }

    return emailTemplates.get(0);
  }

  private Optional<EmailTemplate> filterEmailTemplates(List<EmailTemplate> emailTemplates,
      Predicate<? super EmailTemplate> predicate) {
    return emailTemplates.stream().filter(predicate).findAny();
  }

  @Override
  public Object getData() {
    return this;
  }

}
