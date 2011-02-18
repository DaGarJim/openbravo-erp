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
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2009 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.kernel;

import java.util.List;

import org.hibernate.criterion.Expression;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.module.Module;

/**
 * Base implementation, can be extended.
 * 
 * @author mtaal
 */
public abstract class BaseComponentProvider implements ComponentProvider {

  private Module module;

  public Module getModule() {
    if (module != null) {
      return module;
    }
    OBContext.setAdminMode();
    try {
      final OBCriteria<Module> modules = OBDal.getInstance().createCriteria(Module.class);
      modules.add(Expression.eq(Module.PROPERTY_JAVAPACKAGE, getModulePackageName()));
      if (modules.list().isEmpty()) {
        throw new IllegalStateException("Component " + this.getClass().getName()
            + " is not in a module or it does not belong to a package of a module. "
            + "Consider overriding the getModulePackageName method as it now returns " + "a value "
            + getModulePackageName() + " which does not correspond to a module package name");
      }
      module = modules.list().get(0);
      return module;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Computes parameters to add to a link of a resource. The parameters include the version and
   * language of the user.
   * 
   * The version computation logic depends on if the module is in development (
   * {@link Module#isInDevelopment()}. If in developers mode then the
   * {@link System#currentTimeMillis()} is used. If not in developers mode then the
   * {@link Module#getVersion()} is used. These values are prepended with the language id of the
   * user. This makes it possible to generate language specific components on the server.
   * 
   * @param resource
   *          , the resource to compute the version string for, is typically a resource provided by
   *          the getGlobalResources method
   * @return the version parameter string, a concatenation of the version and language with
   *         parameter names
   * @see KernelConstants#RESOURCE_VERSION_PARAMETER
   * @see KernelConstants#RESOURCE_LANGUAGE_PARAMETER
   * @see KernelUtils#getVersionParameters(Module)
   */
  public String getVersionParameters(String resource) {
    return KernelUtils.getInstance().getVersionParameters(getModule());
  }

  /**
   * Override this method if the component is in a different package than the module.
   * 
   * @return
   */
  protected String getModulePackageName() {
    return this.getClass().getPackage().getName();
  }

  public List<String> getTestResources() {
    return null;
  }
}
