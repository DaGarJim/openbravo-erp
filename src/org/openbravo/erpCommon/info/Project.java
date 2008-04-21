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
 * All portions are Copyright (C) 2001-2006 Openbravo SL 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
*/
package org.openbravo.erpCommon.info;

import org.openbravo.base.secureApp.*;
import org.openbravo.xmlEngine.XmlDocument;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.SQLReturnObject;
import org.openbravo.erpCommon.utility.Utility;
import java.io.*;
import java.util.Vector;

import javax.servlet.*;
import javax.servlet.http.*;

import org.openbravo.utils.Replace;



public class Project extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void init (ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost (HttpServletRequest request, HttpServletResponse response) throws IOException,ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      removePageSessionVariables(vars);
      String strKey = "";
      String strWindow = vars.getGlobalVariable("WindowID", "Project.windowId", "");
      String strBpartner = vars.getGlobalVariable("inpBpartnerId", "Project.bpartner", "");
      String strNameValue = vars.getGlobalVariable("inpNameValue", "Project.key", "");
      vars.removeSessionValue("Project.key");
      if (!strNameValue.equals("")) {
        int guion = strNameValue.indexOf(" - ");
        if (guion!=-1) {
          strKey = strNameValue.substring(0, guion).trim();
          strNameValue = strNameValue.substring(guion+3).trim();
          vars.setSessionValue("Project.key", strKey);
        }
        vars.setSessionValue("Project.name", strNameValue + "%");
      }
      printPage(response, vars, strKey, strNameValue + "%", strBpartner, strWindow);
    } else if (vars.commandIn("KEY")) {
      removePageSessionVariables(vars);
      String strWindow = vars.getGlobalVariable("WindowID", "Project.windowId", "");
      String strBpartner = vars.getGlobalVariable("inpBpartnerId", "Project.bpartner", "");
      String strKeyValue = vars.getGlobalVariable("inpNameValue", "Project.key", "");
      vars.setSessionValue("Project.key", strKeyValue + "%");
      ProjectData[] data = ProjectData.selectKey(this, Utility.getContext(this, vars, "#User_Client", "Project"), Utility.getContext(this, vars, "#User_Org", "Project"), strBpartner, strKeyValue + "%");
      if (data!=null && data.length==1) {
        printPageKey(response, vars, data);
      } else printPage(response, vars, strKeyValue + "%", "", strBpartner, strWindow);
    }   else if(vars.commandIn("STRUCTURE")) {
    	printGridStructure(response, vars);
    } else if(vars.commandIn("DATA")) {
    	if(vars.getStringParameter("newFilter").equals("1")){
    	  removePageSessionVariables(vars);
    	}
      String strWindowId = vars.getGlobalVariable("inpWindowId", "Project.windowId", "");
      String strKey = vars.getGlobalVariable("inpKey", "Project.key", "");
      String strName = vars.getGlobalVariable("inpName", "Project.name", "");
      String strBpartners = vars.getGlobalVariable("inpBpartnerId", "Project.bpartner", "");
      String strIsSOTrx = Utility.getContext(this, vars, "isSOTrx", strWindowId);

        String strNewFilter = vars.getStringParameter("newFilter");
        String strOffset = vars.getStringParameter("offset");
        String strPageSize = vars.getStringParameter("page_size");
        String strSortCols = vars.getStringParameter("sort_cols").toUpperCase();
        String strSortDirs = vars.getStringParameter("sort_dirs").toUpperCase();
    	printGridData(response, vars, strKey, strName, strBpartners, strSortCols + " " + strSortDirs, strOffset, strPageSize, strNewFilter);
    } else pageError(response);
  }
  
  private void removePageSessionVariables(VariablesSecureApp vars){
    vars.removeSessionValue("Project.key");
    vars.removeSessionValue("Project.name");
    vars.removeSessionValue("Project.bpartner");
  }

  void printPage(HttpServletResponse response, VariablesSecureApp vars, String strKeyValue, String strNameValue, String strBpartners, String strWindow) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) log4j.debug("Output: Frame 1 of the projects seeker");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/info/Project").createXmlDocument();
    if (strKeyValue.equals("") && strNameValue.equals("")) {
      xmlDocument.setParameter("key", "%");
    } else {
      xmlDocument.setParameter("key", strKeyValue);
    }
    xmlDocument.setParameter("direction", "var baseDirection = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("language", "LNG_POR_DEFECTO=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setParameter("windowId", strWindow);
    xmlDocument.setParameter("name", strNameValue);
    xmlDocument.setParameter("claveTercero", strBpartners);
    xmlDocument.setParameter("tercero", ProjectData.selectTercero(this, strBpartners));

	    xmlDocument.setParameter("grid", "20");
	    xmlDocument.setParameter("grid_Offset", "");
	    xmlDocument.setParameter("grid_SortCols", "1");
	    xmlDocument.setParameter("grid_SortDirs", "ASC");
	    xmlDocument.setParameter("grid_Default", "0");

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  void printPageKey(HttpServletResponse response, VariablesSecureApp vars, ProjectData[] data) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) log4j.debug("Output: Project seeker Frame Set");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/info/SearchUniqueKeyResponse").createXmlDocument();

    xmlDocument.setParameter("script", generateResult(data));
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  String generateResult(ProjectData[] data) throws IOException, ServletException {
    StringBuffer html = new StringBuffer();
    
    html.append("\nfunction depurarSelector() {\n");
    html.append("var clave = \"" + data[0].cProjectId + "\";\n");
    html.append("var texto = \"" + Replace.replace((data[0].value + " - " + data[0].name), "\"", "\\\"") + "\";\n");
    html.append("parent.opener.closeSearch(\"SAVE\", clave, texto);\n");
    html.append("}\n");
    return html.toString();
  }

  void printGridStructure(HttpServletResponse response, VariablesSecureApp vars) throws IOException, ServletException {
	  if (log4j.isDebugEnabled()) log4j.debug("Output: print page structure");
	    XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/utility/DataGridStructure").createXmlDocument();
	    
	    SQLReturnObject[] data = getHeaders(vars);
	    String type = "Hidden";
	    String title = "";
	    String description = "";
	   	    
	    xmlDocument.setParameter("type", type);
	    xmlDocument.setParameter("title", title);
	    xmlDocument.setParameter("description", description);
	    xmlDocument.setData("structure1", data);
	    response.setContentType("text/xml; charset=UTF-8");
	    response.setHeader("Cache-Control", "no-cache");
	    PrintWriter out = response.getWriter();
	    if (log4j.isDebugEnabled()) log4j.debug(xmlDocument.print());
	    out.println(xmlDocument.print());
	    out.close();
  }
  
  private SQLReturnObject[] getHeaders(VariablesSecureApp vars) {
	  SQLReturnObject[] data = null;
	  Vector<SQLReturnObject> vAux = new Vector<SQLReturnObject>();	  
	  String[] colNames = {"value", "name","bpartner","projectstatus", "rowkey"};
//	  String[] gridNames = {"Key", "Name","Disp. Credit","Credit used", "Contact", "Phone no.", "Zip", "City", "Income", "c_bpartner_id", "c_bpartner_contact_id", "c_bpartner_location_id", "rowkey"};
	  String[] colWidths = {"98", "300", "250", "120", "0"};
	  for(int i=0; i < colNames.length; i++) {
		  SQLReturnObject dataAux = new SQLReturnObject();
		  dataAux.setData("columnname", colNames[i]);
	      dataAux.setData("gridcolumnname", colNames[i]);
	      dataAux.setData("adReferenceId", "AD_Reference_ID");
	      dataAux.setData("adReferenceValueId", "AD_ReferenceValue_ID");	      
	      dataAux.setData("isidentifier", (colNames[i].equals("rowkey")?"true":"false"));
	      dataAux.setData("iskey", (colNames[i].equals("rowkey")?"true":"false"));
	      dataAux.setData("isvisible", (colNames[i].endsWith("_id") || colNames[i].equals("rowkey")?"false":"true"));
	      String name = Utility.messageBD(this, "PJS_" + colNames[i].toUpperCase(), vars.getLanguage());
	      dataAux.setData("name", (name.startsWith("PJS_")?colNames[i]:name));
	      dataAux.setData("type", "string");
	      dataAux.setData("width", colWidths[i]);
	      vAux.addElement(dataAux);
	  }
	  data = new SQLReturnObject[vAux.size()];
	  vAux.copyInto(data);
	  return data;
  }
  
  void printGridData(HttpServletResponse response, VariablesSecureApp vars, String strKey, String strName, String strBpartners, String strOrderBy, String strOffset, String strPageSize, String strNewFilter ) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) log4j.debug("Output: print page rows");
    
    SQLReturnObject[] headers = getHeaders(vars);
    FieldProvider[] data = null;
    String type = "Hidden";
    String title = "";
    String description = "";
    String strNumRows = "0";
    
    if (headers!=null) {
      try{
	  	if(strNewFilter.equals("1") || strNewFilter.equals("")) { // New filter or first load    	
	  		data = ProjectData.select(this, "1", vars.getLanguage(), Utility.getContext(this, vars, "#User_Client", "Project"), Utility.getContext(this, vars, "#User_Org", "Project"), strKey, strName, strBpartners, strOrderBy, "", "");
	  		strNumRows = String.valueOf(data.length);
	  		vars.setSessionValue("ProjectData.numrows", strNumRows);
	  	}
  		else {
  			strNumRows = vars.getSessionValue("ProjectData.numrows");
  		}
	  			
  		// Filtering result
    	if(this.myPool.getRDBMS().equalsIgnoreCase("ORACLE")) {
    		String oraLimit = strOffset + " AND " + String.valueOf(Integer.valueOf(strOffset).intValue() + Integer.valueOf(strPageSize));    		
    		data = ProjectData.select(this, "ROWNUM", vars.getLanguage(), Utility.getContext(this, vars, "#User_Client", "Project"), Utility.getContext(this, vars, "#User_Org", "Project"), strKey, strName, strBpartners, strOrderBy, oraLimit, "");
    	}
    	else {
    		String pgLimit = strPageSize + " OFFSET " + strOffset;
    		data = ProjectData.select(this, "1", vars.getLanguage(), Utility.getContext(this, vars, "#User_Client", "Project"), Utility.getContext(this, vars, "#User_Org", "Project"), strKey, strName, strBpartners, strOrderBy, "", pgLimit);
    	}    	
      } catch (ServletException e) {
        log4j.error("Error in print page data: " + e);
        e.printStackTrace();
        OBError myError = Utility.translateError(this, vars, vars.getLanguage(), e.getMessage());
        if (!myError.isConnectionAvailable()) {
          bdErrorAjax(response, "Error", "Connection Error", "No database connection");
          return;
        } else {
          type = myError.getType();
          title = myError.getTitle();
          if (!myError.getMessage().startsWith("<![CDATA[")) description = "<![CDATA[" + myError.getMessage() + "]]>";
          else description = myError.getMessage();
        }
      } catch (Exception e) { 
        if (log4j.isDebugEnabled()) log4j.debug("Error obtaining rows data");
        type = "Error";
        title = "Error";
        if (e.getMessage().startsWith("<![CDATA[")) description = "<![CDATA[" + e.getMessage() + "]]>";
        else description = e.getMessage();
        e.printStackTrace();
      }
    }
    
    if (!type.startsWith("<![CDATA[")) type = "<![CDATA[" + type + "]]>";
    if (!title.startsWith("<![CDATA[")) title = "<![CDATA[" + title + "]]>";
    if (!description.startsWith("<![CDATA[")) description = "<![CDATA[" + description + "]]>";
    StringBuffer strRowsData = new StringBuffer();
    strRowsData.append("<xml-data>\n");
    strRowsData.append("  <status>\n");
    strRowsData.append("    <type>").append(type).append("</type>\n");
    strRowsData.append("    <title>").append(title).append("</title>\n");
    strRowsData.append("    <description>").append(description).append("</description>\n");
    strRowsData.append("  </status>\n");
    strRowsData.append("  <rows numRows=\"").append(strNumRows).append("\">\n");
    if (data!=null && data.length>0) {
      for (int j=0;j<data.length;j++) {
        strRowsData.append("    <tr>\n");
        for (int k=0;k<headers.length;k++) {
          strRowsData.append("      <td><![CDATA[");
          String columnname = headers[k].getField("columnname");
          
          /*
          if ((
        	   (headers[k].getField("iskey").equals("false") 
        	&& !headers[k].getField("gridcolumnname").equalsIgnoreCase("keyname"))
        	 || !headers[k].getField("iskey").equals("true")) && !tableSQL.getSelectField(columnname + "_R").equals("")) {
        	  columnname += "_R";
          }*/
          
          if ((data[j].getField(columnname)) != null) {
            if (headers[k].getField("adReferenceId").equals("32")) strRowsData.append(strReplaceWith).append("/images/");
            strRowsData.append(data[j].getField(columnname).replaceAll("<b>","").replaceAll("<B>","").replaceAll("</b>","").replaceAll("</B>","").replaceAll("<i>","").replaceAll("<I>","").replaceAll("</i>","").replaceAll("</I>","").replaceAll("<p>","&nbsp;").replaceAll("<P>","&nbsp;").replaceAll("<br>","&nbsp;").replaceAll("<BR>","&nbsp;"));
          } else {
            if (headers[k].getField("adReferenceId").equals("32")) {
              strRowsData.append(strReplaceWith).append("/images/blank.gif");
            } else strRowsData.append("&nbsp;");
          }
          strRowsData.append("]]></td>\n");
        }
        strRowsData.append("    </tr>\n");
      }
    }
    strRowsData.append("  </rows>\n");
    strRowsData.append("</xml-data>\n");
        
    response.setContentType("text/xml; charset=UTF-8");
    response.setHeader("Cache-Control", "no-cache");
    PrintWriter out = response.getWriter();
    if (log4j.isDebugEnabled()) log4j.debug(strRowsData.toString());  
    out.print(strRowsData.toString());
    out.close();
  }

  public String getServletInfo() {
    return "Servlet that presents the project seeker";
  } // end of getServletInfo() method
}
