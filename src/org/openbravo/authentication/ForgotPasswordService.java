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
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.email.EmailEventException;
import org.openbravo.email.EmailUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.poc.EmailInfo;
import org.openbravo.erpCommon.utility.poc.EmailManager;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserPwdResetToken;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.authentication.EmailType;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.model.common.enterprise.EmailTemplate;
import org.openbravo.model.common.enterprise.Organization;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateException;

/**
 * First servlet created for the "Forgot Password" functionality, designed to be used when a
 * password reset has been requested for a specific user
 * <p>
 * Initializes the parameters and performs the initial checks necessary to ensure that the password
 * reset can be performed safely.
 */
public class ForgotPasswordService extends HttpServlet {

  // 15 minutes
  public static final long EXPIRATION_TIME = 15;
  private static final long serialVersionUID = 1L;
  private static final Logger log = LogManager.getLogger();

  private static final SecureRandom secureRandom = new SecureRandom();
  private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

  @Inject
  @Any
  private Instance<ForgotPasswordServiceValidator> validateInstances;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

    try {
      OBContext.setAdminMode(true);
      JSONObject body = new JSONObject(IOUtils.toString(request.getReader()));

      String strAppName = body.optString("appName");
      if (strAppName.isEmpty()) {
        log.warn("Parameter appName not defined in the request");
        throw new ForgotPasswordException("Parameter appName not defined in the request");
      }

      String strOrgId = body.optString("organization");
      if (strOrgId.isEmpty()) {
        log.warn("Parameter organization not defined in the request");
        throw new ForgotPasswordException("Parameter organization not defined in the request");
      }
      Organization org = OBDal.getInstance().get(Organization.class, strOrgId);

      String strClientId = body.optString("client");
      if (strClientId.isEmpty()) {
        log.warn("Parameter client not defined in the request");
        throw new ForgotPasswordException("Parameter client not defined in the request");
      }
      Client client = OBDal.getInstance().get(Client.class, strClientId);

      ForgotPasswordServiceValidator validator = validateInstances
          .select(new ForgotPasswordServiceValidator.Selector(strAppName))
          .get();

      validator.validate(client, org, body);

      EmailServerConfiguration emailConfig = getEmailConfiguration(org, client);

      if (emailConfig == null) {
        log.warn("Email configuration not found for client/organization: {}/{}",
            client.getIdentifier(), org.getIdentifier());
        throw new ForgotPasswordException("Email configuration not found for client/organization: "
            + client.getIdentifier() + "/" + org.getIdentifier());
      }

      String userOrEmail = body.getString("userOrEmail");
      User user = getValidUser(userOrEmail);
      if (user != null) {

        String token = generateAndPersistToken(user, client, org);
        String tokenURL = generateChangePasswordURL(request, token);

        Runnable r = () -> {
          try {
            OBContext.setAdminMode(true);
            EmailTemplate emailTemplate = getEmailTemplate(user, org, client);
            sendChangePasswordEmail(org, client, emailConfig, user, tokenURL, emailTemplate);
          } catch (EmailEventException ex) {
            log.error("Error sending the email", ex);
          } catch (ForgotPasswordException ex) {
            log.error("Error getting email template", ex);
          } catch (Exception ex) {
            log.error("Error with forgot password service", ex);
          } finally {
            OBContext.restorePreviousMode();
          }
        };
        new Thread(r).start();
      }
      writeResult(response, new JSONObject(Map.of("result", "SUCCESS")));
    } catch (ForgotPasswordException ex) {
      JSONObject result = new JSONObject(Map.of("result", ex.getResult(), "clientMsg",
          ex.getClientMsg(), "message", ex.getMessage()));
      writeResult(response, result);
    } catch (JSONException | URISyntaxException ex) {
      JSONObject result = new JSONObject(Map.of("result", "ERROR", "message", ex.getMessage()));
      writeResult(response, result);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private EmailServerConfiguration getEmailConfiguration(Organization org, Client client)
      throws JSONException {
    OBContext.getOBContext().setCurrentClient(client);
    EmailServerConfiguration emailConfig = EmailUtils.getEmailConfiguration(org);
    if (emailConfig == null) {
      OBContext.getOBContext().setCurrentClient(OBDal.getInstance().get(Client.class, "0"));
      emailConfig = EmailUtils.getEmailConfiguration(org);
    }
    return emailConfig;
  }

  private User getValidUser(String userOrEmail) {
    List<User> users = OBDal.getInstance()
        .createCriteria(User.class)
        .add(Restrictions.or(Restrictions.eq(User.PROPERTY_USERNAME, userOrEmail),
            Restrictions.eq(User.PROPERTY_EMAIL, userOrEmail)))
        .setFilterOnActive(true)
        .setFilterOnReadableClients(false)
        .setFilterOnReadableOrganization(false)
        .list();

    if (users.size() == 0) {
      log.warn("User or email not found: {}", userOrEmail);
      return null;
    }
    if (users.size() > 1) {
      log.warn("More than one email configured for: {}", userOrEmail);
      return null;
    }

    User user = users.get(0);

    boolean userCompliesWithRules = checkUser(user);
    if (!userCompliesWithRules) {
      log.warn("User does not comply with the rules: {}", userOrEmail);
      return null;
    }
    return user;
  }

  private boolean checkUser(User user) {
    return user.isActive() && user.getEmail() != null && !user.getEmail().isEmpty()
        && !user.isLocked() && !user.isSsoonly();
  }

  private String generateAndPersistToken(User user, Client client, Organization org) {
    String token = generateNewToken();
    UserPwdResetToken resetToken = OBProvider.getInstance().get(UserPwdResetToken.class);
    resetToken.setClient(client);
    resetToken.setOrganization(org);
    resetToken.setUsertoken(token);
    resetToken.setUserContact(user);

    OBDal.getInstance().save(resetToken);
    return token;
  }

  private String generateChangePasswordURL(HttpServletRequest request, String token)
      throws URISyntaxException {

    URI referer = new URI(request.getHeader("Referer"));
    String query = referer.getQuery();

    StringBuilder newQuery = new StringBuilder();
    if (query != null && !query.isEmpty()) {
      newQuery.append(query);
      newQuery.append("&");
    }

    newQuery.append("changePassword=");
    newQuery.append(token);

    return new URI(referer.getScheme(), referer.getAuthority(), referer.getPath(),
        newQuery.toString(), referer.getFragment()).toString();

  }

  private EmailTemplate getEmailTemplate(User user, Organization org, Client client)
      throws ForgotPasswordException {
    if (org == null) {
      throw new ForgotPasswordException(
          OBMessageUtils.getI18NMessage("NoForgottenPasswordEmailTemplatePresent"));
    }

    List<EmailTemplate> emailTemplates = OBDal.getInstance()
        .createCriteria(EmailTemplate.class)
        .add(Restrictions.eq(EmailTemplate.PROPERTY_EMAILTYPE,
            OBDal.getInstance().get(EmailType.class, "5209BE52755B49C582F034E9B98B3F33")))
        .add(Restrictions.eq(EmailTemplate.PROPERTY_ORGANIZATION, org))
        .setFilterOnActive(true)
        .setFilterOnReadableClients(false)
        .setFilterOnReadableOrganization(false)
        .list();

    if (emailTemplates.isEmpty()) {
      OrganizationStructureProvider orgStructure = new OrganizationStructureProvider();
      return getEmailTemplate(user, orgStructure.getParentOrg(org), client);
    }

    Optional<EmailTemplate> emailTemplate = filterEmailTemplates(emailTemplates,
        template -> template.getLanguage() != null && user.getDefaultLanguage() != null
            && template.getLanguage().getId().equals(user.getDefaultLanguage().getId()));
    if (emailTemplate.isPresent()) {
      return emailTemplate.get();
    }

    emailTemplate = filterEmailTemplates(emailTemplates,
        template -> template.getLanguage() != null && org.getLanguage() != null
            && template.getLanguage().getId().equals(org.getLanguage().getId()));
    if (emailTemplate.isPresent()) {
      return emailTemplate.get();
    }

    emailTemplate = filterEmailTemplates(emailTemplates,
        template -> template.getLanguage() != null && client.getLanguage() != null
            && template.getLanguage().getId().equals(client.getLanguage().getId()));
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

  private void sendChangePasswordEmail(Organization org, Client client,
      EmailServerConfiguration emailConfig, User user, String url, EmailTemplate emailTemplate)
      throws Exception {
    String emailTemplateBody = emailTemplate.getBody();
    String emailBody = processBodyWithFreemarker(user, url, emailTemplateBody);

    final EmailInfo email = new EmailInfo.Builder() //
        .setSubject(emailTemplate.getSubject()) //
        .setRecipientTO(user.getEmail()) //
        .setContent(emailBody) //
        .setContentType(isTemplateHTML(emailTemplate) ? "text/html; charset=utf-8"
            : "text/plain; charset=utf-8")
        .build();

    EmailManager.sendEmail(emailConfig, email);
  }

  private boolean isTemplateHTML(EmailTemplate emailTemplate) {

    // checks for POS2 isHTML property in case POS2 is installed
    if (emailTemplate.getEntity().getProperty("obpos2Ishtml", false) == null) {
      return false;
    }
    return (Boolean) emailTemplate.get("obpos2Ishtml");
  }

  private String processBodyWithFreemarker(User user, String url, String emailTemplateBody)
      throws IOException, TemplateException {
    final Configuration configuration = new Configuration();
    configuration.setObjectWrapper(new DefaultObjectWrapper());

    freemarker.template.Template templateImplementation = new freemarker.template.Template(
        "template", new StringReader(emailTemplateBody), configuration);

    Map<String, Object> emailData = new HashMap<String, Object>();
    emailData.put("user", user);
    emailData.put("change_password_url", url);
    emailData.put("reset_password_timeout", EXPIRATION_TIME);

    final StringWriter output = new StringWriter();
    templateImplementation.process(emailData, output);
    return output.toString();
  }

  private void writeResult(HttpServletResponse response, JSONObject result) throws IOException {
    response.setContentType("application/json;charset=UTF-8");
    response.setHeader("Content-Type", "application/json;charset=UTF-8");

    final Writer w = response.getWriter();
    w.write(result.toString());
    w.close();
  }

  private static String generateNewToken() {
    byte[] randomBytes = new byte[24];
    secureRandom.nextBytes(randomBytes);
    return base64Encoder.encodeToString(randomBytes);
  }
}
