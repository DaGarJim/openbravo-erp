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
 * All portions are Copyright (C) 2018-2025 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.views;

import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.openbravo.client.application.GCSystem;
import org.openbravo.client.application.GCTab;
import org.openbravo.client.application.window.StandardWindowComponent;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.test.base.OBBaseTest;

/**
 * It can be extended by the test classes that deal with grid configurations
 */
public class GridConfigurationTest extends OBBaseTest {

  @Before
  public void init() {
    disableCurrentGridConfigurations();
  }

  @After
  public void cleanUp() {
    rollback();
  }

  /**
   * Disables the existing grid configurations
   */
  private void disableCurrentGridConfigurations() {
    OBDal.getInstance()
        .getSession()
        .createQuery("update OBUIAPP_GC_System set active = false")
        .executeUpdate();
    OBDal.getInstance()
        .getSession()
        .createQuery("update OBUIAPP_GC_Tab set active = false")
        .executeUpdate();
    OBDal.getInstance().flush();
  }

  protected static Optional<GCSystem> getSystemGridConfig() {
    return StandardWindowComponent.getSystemGridConfig();
  }

  protected static Optional<GCTab> getTabGridConfig(Tab tab) {
    return StandardWindowComponent.getTabsGridConfig(tab.getWindow()).get(tab.getId());
  }
}
