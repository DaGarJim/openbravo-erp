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
 * All portions are Copyright (C) 2010 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.test;

import org.openbravo.client.application.MenuManager;
import org.openbravo.client.application.MenuManager.MenuOption;
import org.openbravo.test.base.BaseTest;

/**
 * Tests the reading of the menu in memory
 * 
 * @author iperdomo
 */
public class MenuTest extends BaseTest {

  /**
   * Test reading the menu
   */
  public void testSystemAdministratorMenu() throws Exception {
    setSystemAdministratorContext();
    final MenuManager menuManager = new MenuManager();
    final long time = System.currentTimeMillis();
    final MenuManager.MenuOption rootMenuOption = menuManager.getMenu();
    System.err.println((System.currentTimeMillis() - time));
    dumpMenuOption(rootMenuOption, 0);
    assertFalse(menuManager.getSelectableMenuOptions().isEmpty());
  }

  /**
   * Test reading the menu
   */
  public void testOpenbravoAdminMenu() throws Exception {
    setBigBazaarAdminContext();
    final MenuManager menuManager = new MenuManager();
    final MenuManager.MenuOption rootMenuOption = menuManager.getMenu();
    dumpMenuOption(rootMenuOption, 0);
    assertFalse(menuManager.getSelectableMenuOptions().isEmpty());
  }

  private void dumpMenuOption(MenuOption menuOption, int level) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < level; i++) {
      sb.append(">");
    }
    sb.append(menuOption.getLabel());
    sb.append(" (" + menuOption.getType() + "): " + menuOption.getId());
    System.err.println(sb.toString());
    for (MenuOption childOption : menuOption.getChildren()) {
      dumpMenuOption(childOption, level + 1);
    }
  }
}
