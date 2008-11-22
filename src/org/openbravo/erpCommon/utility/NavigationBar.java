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
 * All portions are Copyright (C) 2001-2008 Openbravo SL 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.utility;

import org.apache.log4j.Logger;
import org.openbravo.database.ConnectionProvider;

public class NavigationBar {
    static Logger log4j = Logger.getLogger(NavigationBar.class);
    ConnectionProvider conn;
    String language = "en_US";
    String servlet_action = "";
    String window_name = "";
    String base_direction = "";
    String breadcrumb = "";
    String window_type = "";

    public NavigationBar(ConnectionProvider _conn, String _language,
            String _action, String _windowName, String _windowType,
            String _baseDirection, String _breadcrumb) {
        this.conn = _conn;
        this.language = _language;
        this.servlet_action = _action;
        this.window_name = _windowName;
        this.base_direction = _baseDirection;
        this.breadcrumb = _breadcrumb;
        this.window_type = _windowType;
    }

    @Override
    public String toString() {
        final StringBuffer toolbar = new StringBuffer();
        toolbar
                .append("<TABLE class=\"Main_ContentPane_NavBar\" id=\"tdtopNavButtons\">\n");
        toolbar.append("  <TR class=\"Main_NavBar_bg\"><TD></TD>\n");
        toolbar.append("  <TD class=\"Main_NavBar_LeftButton_cell\">\n");
        toolbar
                .append("    <a class=\"Main_NavBar_LeftButton\" href=\"#\" onclick=\"openLink('../secureApp/GoBack.html', 'appFrame')\" border=\"0\" onmouseover=\"window.status='");
        String auxText = Utility.messageBD(conn, "GoBack", language);
        toolbar.append(auxText);
        toolbar
                .append(
                        "';return true;\" onmouseout=\"window.status='';return true;\" id=\"buttonBack\"><IMG src=\"")
                .append(base_direction)
                .append(
                        "/images/blank.gif\" class=\"Main_NavBar_LeftButton_Icon Main_NavBar_LeftButton_Icon_back\" border=\"0\" alt=\"");
        toolbar.append(auxText).append("\" title=\"").append(auxText).append(
                "\"");
        toolbar.append("/></a>\n");
        toolbar.append("  </TD>\n");
        toolbar.append("  <TD class=\"Main_NavBar_separator_cell\"></TD>\n");
        toolbar.append("  <TD class=\"Main_NavBar_LeftButton_cell\">\n");
        toolbar
                .append(
                        "    <a class=\"Main_NavBar_LeftButton\" href=\"#\" onClick=\"submitCommandForm('DEFAULT', false, null, '")
                .append(servlet_action);
        toolbar
                .append("', '_self', null, true);return false;\" border=\"0\" onmouseover=\"window.status='");
        auxText = Utility.messageBD(conn, "Refresh", language);
        toolbar
                .append(auxText)
                .append(
                        "';return true;\" onmouseout=\"window.status='';return true;\" id=\"buttonRefresh\"><IMG src=\"");
        toolbar
                .append(base_direction)
                .append(
                        "/images/blank.gif\" class=\"Main_NavBar_LeftButton_Icon Main_NavBar_LeftButton_Icon_refresh\" border=\"0\" alt=\"");
        toolbar.append(auxText).append("\" title=\"").append(auxText).append(
                "\"");
        toolbar.append("></a>\n");
        toolbar.append("  </TD>\n");
        toolbar
                .append(
                        "  <TD class=\"Main_NavBar_Breadcrumb_cell\"><span class=\"Main_NavBar_Breadcrumb\" id=\"paramBreadcrumb\">")
                .append(breadcrumb).append("</span></TD>\n");
        toolbar.append("  <TD></TD>\n");
        toolbar.append("  <TD class=\"Main_NavBar_RightButton_cell\">\n");
        toolbar
                .append("    <a class=\"Main_NavBar_RightButton\" href=\"#\" onclick=\"about();return false;\" border=\"0\" onmouseover=\"window.status='");
        auxText = Utility.messageBD(conn, "About", language);
        toolbar
                .append(auxText)
                .append(
                        "';return true;\" onmouseout=\"window.status='';return true;\" id=\"buttonAbout\"><IMG src=\"")
                .append(base_direction)
                .append(
                        "/images/blank.gif\" class=\"Main_NavBar_RightButton_Icon Main_NavBar_RightButton_Icon_about\" border=\"0\"");
        toolbar.append(" alt=\"").append(auxText).append("\" title=\"").append(
                auxText).append("\"");
        toolbar.append("></a>\n");
        toolbar.append("  </TD>\n");
        toolbar
                .append("  <TD class=\"Main_NavBar_separator_cell_small\"></TD>\n");
        toolbar.append("  <TD class=\"Main_NavBar_RightButton_cell\">\n");
        toolbar
                .append("    <a class=\"Main_NavBar_RightButton\" href=\"#\" onclick=\"openHelp(");
        if (window_type.equalsIgnoreCase("W"))
            toolbar.append("document.frmMain.inpwindowId.value");
        else
            toolbar.append("null");
        toolbar
                .append(", '../ad_help/DisplayHelp.html', 'HELP', false, null, null, '");
        toolbar.append(window_type).append("', '").append(window_name);
        toolbar
                .append("');return false;\" border=\"0\" onmouseover=\"window.status='");
        auxText = Utility.messageBD(conn, "Help", language);
        toolbar
                .append(auxText)
                .append(
                        "';return true;\" onmouseout=\"window.status='';return true;\" id=\"buttonHelp\"><IMG src=\"")
                .append(base_direction)
                .append(
                        "/images/blank.gif\" class=\"Main_NavBar_RightButton_Icon Main_NavBar_RightButton_Icon_help\" border=\"0\"");
        toolbar.append(" alt=\"").append(auxText).append("\" title=\"").append(
                auxText).append("\"");
        toolbar.append("></a>\n");
        toolbar.append("  </TD>\n");
        toolbar.append("  <TD class=\"Main_NavBar_separator_cell\"></TD>\n");
        toolbar.append("  <TD class=\"Main_NavBar_bg_logo_left\"></TD>\n");
        toolbar
                .append(
                        "  <TD class=\"Main_NavBar_bg_logo\" width=\"1\" onclick=\"openNewBrowser('http://www.openbravo.com', 'Openbravo');return false;\"><IMG src=\"")
                .append(base_direction)
                .append(
                        "/images/blank.gif\" alt=\"Openbravo\" title=\"Openbravo\" border=\"0\" id=\"openbravoLogo\" class=\"Main_NavBar_logo\"></TD>\n");
        toolbar.append("  <TD class=\"Main_NavBar_bg_logo_right\"></TD>\n");
        toolbar.append("  <TD></TD>\n");
        toolbar.append("  </TR>\n");
        toolbar.append("</TABLE>");
        return toolbar.toString();
    }
}
