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
 * All portions are Copyright (C) 2010-2024 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.kernel;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.common.enterprise.EmailTemplate;
import org.openbravo.service.json.JsonConstants;

/**
 * Contains generic code for {@link TemplateProcessor} instances.
 * 
 * The generics parameter T is the class of the template as it exists in the specific templating
 * language.
 * 
 * @author mtaal
 */
public abstract class BaseTemplateProcessor<T extends Object> implements TemplateProcessor {

  private Map<String, T> templateCache = new ConcurrentHashMap<String, T>();

  /*
   * (non-Javadoc)
   * 
   * @see org.openbravo.client.kernel.TemplateProcessor#process(org.openbravo.client.kernel.
   * ComponentTemplate , java.util.Map)
   */
  @Override
  public String process(Template template, Map<String, Object> data) {
    T templateImplementation = getTemplateImplementation(template);
    if (templateImplementation == null) {
      final String source = createTemplateSource(template);
      templateImplementation = createSetFreeMarkerTemplateInCache(template, source);
    }

    return processWithDefaults(data, templateImplementation);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.openbravo.client.kernel.TemplateProcessor#process(org.openbravo.model.common.enterprise.
   * EmailTemplate , java.util.Map)
   */
  @Override
  public String process(EmailTemplate template, Map<String, Object> data) {
    T templateImplementation = getTemplateImplementation(template);
    if (templateImplementation == null) {
      final String source = createTemplateSource(template);
      templateImplementation = createSetFreeMarkerTemplateInCache(template, source);
    }

    return processWithDefaults(data, templateImplementation);
  }

  private String processWithDefaults(Map<String, Object> data, T templateImplementation) {
    // add some defaults
    data.put("Constants_FIELDSEPARATOR", DalUtil.FIELDSEPARATOR);
    data.put("Constants_IDENTIFIER", JsonConstants.IDENTIFIER);
    return processTemplate(templateImplementation, data);
  }

  /**
   * Run a template implementation for specific data set.
   * 
   * @param templateImplementation
   *          the template to process
   * @param data
   *          the data which should be passed to the template
   * @return the template output
   */
  protected abstract String processTemplate(T templateImplementation, Map<String, Object> data);

  /**
   * Return the template language specific implementation of the template.
   * 
   * @param template
   *          the template stored in the DB
   * @return the template implementation in the template language
   */
  protected synchronized T getTemplateImplementation(Template template) {
    return getTemplateImplementationById(template.getId());
  }

  /**
   * Return the email template language specific implementation of the template.
   * 
   * @param template
   *          the email template stored in the DB
   * @return the email template implementation in the email template language
   */
  protected synchronized T getTemplateImplementation(EmailTemplate template) {
    return getTemplateImplementationById(template.getId());
  }

  private T getTemplateImplementationById(String id) {
    // can not be cached
    if (id == null) {
      return null;
    }
    final Map<String, T> localTemplateCache = templateCache;
    T templateImplementation = localTemplateCache.get(id);
    if (templateImplementation != null) {
      return templateImplementation;
    }
    return null;
  }

  /**
   * Creates the template source taking into account overriding templates and pre-prending
   * depends-on templates.
   * 
   * @param template
   *          the template to create the source for
   * @return a complete template source
   * @see TemplateResolver#resolve(Template)
   */
  protected String createTemplateSource(Template template) {
    final List<Template> resolvedTemplates = TemplateResolver.getInstance().resolve(template);
    final StringBuilder source = new StringBuilder();
    for (Template resolvedTemplate : resolvedTemplates) {
      if (resolvedTemplate.getTemplateClasspathLocation() != null) {
        source
            .append(readTemplateSourceFromClasspath(resolvedTemplate.getTemplateClasspathLocation())
                + "\n");
      } else {
        source.append(resolvedTemplate.getTemplate() + "\n");
      }
    }
    return source.toString();
  }

  /**
   * Creates the email template source from the email template body
   * 
   * @param template
   *          the email template to create the source for
   * @return a complete template source
   */
  protected String createTemplateSource(EmailTemplate template) {
    return template.getBody();
  }

  /**
   * Checks the cache if there is already a template implementation for a certain template. If so
   * that one is returned. If not then a new implementation is created.
   * 
   * @param template
   * @param source
   */
  protected synchronized T createSetFreeMarkerTemplateInCache(Template template, String source) {
    final T specificTemplate = createTemplateImplementation(template, source);
    Module module = template.getModule();
    String id = template.getId();

    return createSetFreeMarkerTemplateInCache(specificTemplate, module, id);
  }

  /**
   * Checks the cache if there is already an email template implementation for a certain email
   * template. If so that one is returned. If not then a new implementation is created.
   * 
   * @param template
   * @param source
   */
  protected synchronized T createSetFreeMarkerTemplateInCache(EmailTemplate template,
      String source) {
    final T specificTemplate = createTemplateImplementation(template, source);
    Module module = template.getEmailType().getModule();
    String id = template.getId();

    return createSetFreeMarkerTemplateInCache(specificTemplate, module, id);
  }

  private T createSetFreeMarkerTemplateInCache(final T specificTemplate, Module module, String id) {
    // do not cache if the module is in development
    if (module != null && module.isInDevelopment() != null && module.isInDevelopment()) {
      return specificTemplate;
    }
    if (id != null) {
      final Map<String, T> localTemplateCache = templateCache;
      localTemplateCache.put(id, specificTemplate);
    }

    return specificTemplate;
  }

  /**
   * To be implemented by the subclass. Based on the template from the DB and the complete template
   * source, create a language specific template implementation instance.
   * 
   * @param template
   *          the template object from the DB
   * @param source
   *          the complete source (after resolving and including all dependencies)
   * @return the template implementation
   */
  protected abstract T createTemplateImplementation(Template template, String source);

  /**
   * To be implemented by the subclass. Based on the email template from the DB and the complete
   * email template source, create a language specific template implementation instance.
   * 
   * @param template
   *          the email template object from the DB
   * @param source
   *          the complete source (after resolving and including all dependencies)
   * @return the email template implementation
   */
  protected abstract T createTemplateImplementation(EmailTemplate template, String source);

  /**
   * Reads the template source from the classpath
   * 
   * @param path
   *          the path to the template file
   * @return the read template
   */
  protected String readTemplateSourceFromClasspath(String path) {
    InputStream input = null;
    try {
      input = this.getClass().getResourceAsStream(path.trim().replace(" ", "+"));
      return IOUtils.toString(input);
    } catch (Exception e) {
      throw new IllegalArgumentException("Exception for path " + path, e);
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.openbravo.client.kernel.TemplateProcessor#clearCache()
   */
  @Override
  public void clearCache() {
    templateCache = new ConcurrentHashMap<String, T>();
  }
}
