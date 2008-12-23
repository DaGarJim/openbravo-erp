/*
 ************************************************************************************
 * Copyright (C) 2001-2006 Openbravo S.L.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to  in writing,  software  distributed
 * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
 * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */
package org.openbravo.base.secureApp;

import javax.servlet.ServletException;
import org.openbravo.erpCommon.ad_combos.ClientComboData;
import org.openbravo.erpCommon.ad_combos.OrganizationComboData;
import org.openbravo.erpCommon.ad_combos.RoleComboData;
import org.openbravo.erpCommon.reference.PreferencesData;
import org.openbravo.erpCommon.utility.Utility;
import org.apache.log4j.Logger;
import org.openbravo.database.ConnectionProvider;

public class LoginUtils {

    public static Logger log4j = Logger.getLogger(LoginUtils.class);

    /** Creates a new instance of LoginUtils */
    private LoginUtils() {
    }

    public static boolean fillSessionArguments(ConnectionProvider conn,
            VariablesSecureApp vars, String strUserAuth, String strLanguage,
            String strIsRTL, String strRol, String strCliente, String strOrg,
            String strAlmacen) throws ServletException {

        // Check session options
        if (!RoleComboData.isUserRole(conn, strUserAuth, strRol)) {
            log4j.error("Login role is not in user roles list");
            log4j.error("User: " + strUserAuth);
            log4j.error("Role: " + strRol);
            return false;
        }
        if (!ClientComboData.isRoleClient(conn, strRol, strCliente)) {
            log4j.error("Login client is not in role clients list");
            return false;
        }
        if (!OrganizationComboData.isLoginRoleOrg(conn, strRol, strOrg)) {
            log4j.error("Login organization is not in role organizations list");
            return false;
        }

        // Set session vars
        vars.setSessionValue("#AD_User_ID", strUserAuth);
        vars.setSessionValue("#SalesRep_ID", strUserAuth);
        vars.setSessionValue("#AD_Language", strLanguage);
        vars.setSessionValue("#AD_Role_ID", strRol);
        vars.setSessionValue("#AD_Client_ID", strCliente);
        vars.setSessionValue("#AD_Org_ID", strOrg);
        vars.setSessionValue("#M_Warehouse_ID", strAlmacen);

        vars.setSessionValue("#StdPrecision", "2");

        // Organizations tree
        try {
            OrgTree tree = new OrgTree(conn, strCliente);
            vars.setSessionObject("#CompleteOrgTree", tree);
            OrgTree accessibleTree = tree.getAccessibleTree(conn, strRol);
            vars.setSessionValue("#AccessibleOrgTree", accessibleTree
                    .toString());
        } catch (Exception e) {
            log4j.warn("Error while setting Organzation tree to session " + e);
            return false;
        }

        try {
            SeguridadData[] data = SeguridadData.select(conn, strRol,
                    strUserAuth);
            if (data == null || data.length == 0)
                return false;
            vars.setSessionValue("#User_Level", data[0].userlevel);
            vars.setSessionValue("#User_Client", data[0].clientlist);
            vars.setSessionValue("#User_Org", data[0].orglist);
            vars
                    .setSessionValue("#Approval_C_Currency_ID",
                            data[0].cCurrencyId);
            vars.setSessionValue("#Approval_Amt", data[0].amtapproval);
            vars.setSessionValue("#Client_Value", data[0].value);
            vars.setSessionValue("#Client_SMTP", data[0].smtphost);

            data = null;
            AttributeData[] attr = AttributeData
                    .select(conn, Utility.getContext(conn, vars,
                            "#User_Client", "LoginHandler"),
                            Utility.getContext(conn, vars, "#User_Org",
                                    "LoginHandler"));
            if (attr != null && attr.length > 0) {
                vars.setSessionValue("$C_AcctSchema_ID", attr[0].value);
                vars.setSessionValue("$C_Currency_ID", attr[0].attribute);
                vars.setSessionValue("#StdPrecision", AttributeData
                        .selectStdPrecision(conn, attr[0].attribute, Utility
                                .getContext(conn, vars, "#User_Client",
                                        "LoginHandler"), Utility.getContext(
                                conn, vars, "#User_Org", "LoginHandler")));
                vars.setSessionValue("$HasAlias", attr[0].hasalias);
                for (int i = 0; i < attr.length; i++)
                    vars
                            .setSessionValue("$Element_" + attr[i].elementtype,
                                    "Y");
            }
            attr = null;
            PreferencesData[] prefs = PreferencesData
                    .select(conn, Utility.getContext(conn, vars,
                            "#User_Client", "LoginHandler"),
                            Utility.getContext(conn, vars, "#User_Org",
                                    "LoginHandler"), strUserAuth);

            if (prefs != null && prefs.length > 0) {
                for (int i = 0; i < prefs.length; i++) {
                    vars.setSessionValue("P|"
                            + (prefs[i].adWindowId.equals("") ? ""
                                    : (prefs[i].adWindowId + "|"))
                            + prefs[i].attribute, prefs[i].value);
                }
            }
            prefs = null;

            attr = AttributeData.selectIsSOTrx(conn);
            if (attr != null && attr.length > 0) {
                for (int i = 0; i < attr.length; i++)
                    vars.setSessionValue(attr[i].adWindowId + "|isSOTrx",
                            attr[i].value);
            }
            attr = null;

            DefaultSessionValuesData[] ds = DefaultSessionValuesData
                    .select(conn);
            if (ds != null && ds.length > 0) {
                for (int i = 0; i < ds.length; i++) {
                    String value = DefaultValuesData.select(conn,
                            ds[i].columnname, ds[i].tablename, Utility
                                    .getContext(conn, vars, "#User_Client",
                                            "LoginHandler"), Utility
                                    .getContext(conn, vars, "#User_Org",
                                            "LoginHandler"));
                    if (ds[i].tablename.equals("C_DocType"))
                        vars.setSessionValue("#C_DocTypeTarget_ID", value);
                    vars.setSessionValue("#" + ds[i].columnname, value);
                }
            }
            vars.setSessionValue("#Date", Utility.getContext(conn, vars,
                    "#Date", "LoginHandler"));
            vars.setSessionValue("#ShowTrl", Utility.getPreference(vars,
                    "ShowTrl", ""));
            vars.setSessionValue("#ShowAcct", Utility.getPreference(vars,
                    "ShowAcct", ""));
            vars.setSessionValue("#ShowAudit", Utility.getPreference(vars,
                    "ShowAuditDefault", ""));
            vars.setSessionValue("#ShowConfirmation", Utility.getPreference(
                    vars, "ShowConfirmationDefault", ""));
            vars.setSessionValue("#Autosave", Utility.getPreference(vars,
                    "Autosave", ""));
            SystemPreferencesData[] dataSystem = SystemPreferencesData
                    .select(conn);
            if (dataSystem != null && dataSystem.length > 0) {
                vars.setSessionValue("#RecordRange",
                        dataSystem[0].tadRecordrange);
                vars.setSessionValue("#RecordRangeInfo",
                        dataSystem[0].tadRecordrangeInfo);
                vars.setSessionValue("#Transactional$Range",
                        dataSystem[0].tadTransactionalrange);
                if (strIsRTL.equals("Y")) {
                    vars.setSessionValue("#Theme", "rtl/"
                            + dataSystem[0].tadTheme);
                    vars.setSessionValue("#TextDirection", "RTL");
                } else if (strIsRTL.equals("N")) {
                    vars.setSessionValue("#Theme", "ltr/"
                            + dataSystem[0].tadTheme);
                    vars.setSessionValue("#TextDirection", "LTR");
                } else {
                    log4j
                            .error("Can't detect direction of language: ltr? rtl? parameter isRTL missing in call to LoginUtils.getStringParameter");
                    return false;
                }
            }

        } catch (ServletException e) {
            log4j.warn("Error while loading session arguments: " + e);
            return false;
        }
        return true;
    }
}
