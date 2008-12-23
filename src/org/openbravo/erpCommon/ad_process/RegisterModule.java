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
 * All portions are Copyright (C) 2008 Openbravo SL 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_process;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.xmlEngine.XmlDocument;

import org.openbravo.services.webservice.Module;
import org.openbravo.services.webservice.WebServiceImpl;
import org.openbravo.services.webservice.WebServiceImplServiceLocator;

public class RegisterModule extends HttpSecureAppServlet {
    private static final long serialVersionUID = 1L;

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        VariablesSecureApp vars = new VariablesSecureApp(request);

        if (vars.commandIn("DEFAULT")) {
            printPage(response, vars, false);
        }
        if (vars.commandIn("REGISTER")) {
            printPage(response, vars, true);
        } else
            pageError(response);
    }

    private void printPage(HttpServletResponse response,
            VariablesSecureApp vars, boolean process) throws IOException,
            ServletException {

        String discard[] = { "", "" };
        String moduleId = vars.getStringParameter("inpadModuleId");
        // execute registration process
        if (process) {
            discard[0] = "discardOk";
            discard[1] = "discardParams";

            // set the module
            log4j.info("Registering module " + moduleId);
            RegisterModuleData data = RegisterModuleData.selectModule(this,
                    moduleId);
            Module module = new Module();
            module.setModuleID(moduleId);
            module.setName(data.name);
            module.setPackageName(data.javapackage);
            module.setAuthor(data.author);
            module.setType(data.type);
            module.setHelp(data.help);
            module.setDbPrefix(data.dbPrefix);
            module.setDescription(data.description);

            WebServiceImpl ws = null;
            boolean error = false;
            try {
                // retrieve the module details from the webservice
                WebServiceImplServiceLocator loc = new WebServiceImplServiceLocator();
                ws = (WebServiceImpl) loc.getWebService();
            } catch (Exception e) {
                OBError message = new OBError();
                message.setType("Error");
                message.setTitle(Utility.messageBD(this, "Error", vars
                        .getLanguage()));
                message.setMessage(Utility.messageBD(this, "WSError", vars
                        .getLanguage()));
                vars.setMessage("RegisterModule", message);
                e.printStackTrace();
                error = true;
            }

            if (!error) {
                try {
                    module = ws.moduleRegister(module, vars
                            .getStringParameter("inpUser"), vars
                            .getStringParameter("inpPassword"));
                    RegisterModuleData.setRegistered(this, moduleId);
                } catch (Exception e) {
                    OBError message = new OBError();
                    message.setType("Error");
                    message.setTitle(Utility.messageBD(this, "Error", vars
                            .getLanguage()));
                    message.setMessage(e.getMessage());
                    vars.setMessage("RegisterModule", message);
                    error = true;
                    e.printStackTrace();
                }
            }

            if (!error) {
                OBError message = new OBError();
                message.setType("Success");
                message.setTitle(Utility.messageBD(this, "ProcessOK", vars
                        .getLanguage()));
                vars.setMessage("RegisterModule", message);
                error = true;
            }
        }

        XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
                "org/openbravo/erpCommon/ad_process/RegisterModule", discard)
                .createXmlDocument();
        xmlDocument.setParameter("language", "defaultLang=\""
                + vars.getLanguage() + "\";");
        xmlDocument.setParameter("directory", "var baseDirectory = \""
                + strReplaceWith + "/\";\r\n");
        xmlDocument.setParameter("theme", vars.getTheme());
        xmlDocument.setParameter("help", RegisterModuleData.getHelp(this, vars
                .getLanguage()));
        xmlDocument.setParameter("inpadModuleId", moduleId);

        {
            OBError myMessage = vars.getMessage("RegisterModule");
            vars.removeMessage("RegisterModule");
            if (myMessage != null) {
                xmlDocument.setParameter("messageType", myMessage.getType());
                xmlDocument.setParameter("messageTitle", myMessage.getTitle());
                xmlDocument.setParameter("messageMessage", myMessage
                        .getMessage());
            }
        }
        PrintWriter out = response.getWriter();
        response.setContentType("text/html; charset=UTF-8");
        out.println(xmlDocument.print());
        out.close();
    }
}
