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
 * All portions are Copyright (C) 2001-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.wad;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.openbravo.data.FieldProvider;
import org.openbravo.data.Sqlc;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.utils.FormatUtilities;
import org.openbravo.wad.controls.WADControl;
import org.openbravo.wad.controls.WADGrid;
import org.openbravo.xmlEngine.XmlDocument;
import org.openbravo.xmlEngine.XmlEngine;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Main class of WAD project. This class manage all the process to generate the sources from the
 * model.
 * 
 * @author Fernando Iriazabal
 */
public class Wad extends DefaultHandler {
  private static final int NUM_TABS = 8;
  private static final int INCR_TABS = 8;
  private static final int HEIGHT_TABS = 38;
  private static final int MAX_SIZE_EDITION_1_COLUMNS = 90;
  private static final int MAX_TEXTBOX_LENGTH = 110;
  private static final String WELD_LISTENER_ID = "3F88D97C7E9E4DD9847A5488771F4AB3";
  private static final String NONE = "none";
  private XmlEngine xmlEngine;
  private WadConnection pool;
  private String strSystemSeparator;
  private static String jsDateFormat;
  private static String sqlDateFormat;
  private static boolean generateAllClassic250Windows;
  private static boolean excludeCDI;

  private static final Logger log4j = Logger.getLogger(Wad.class);

  /**
   * Main function, entrusted to launch the process of generation of sources. The list of arguments
   * that it can receive are the following ones:<br>
   * <ol>
   * <li>Path to XmlPool.xml</li>
   * <li>Name of the window to generate (% for all)</li>
   * <li>Path to generate the code</li>
   * <li>Path to generate common objects (reference)</li>
   * <li>Path to generate the web.xml</li>
   * <li>Parameter:
   * <ul>
   * <li>tabs (To generate only the windows and action buttons)</li>
   * <li>web.xml (To generate only the web.xml)</li>
   * <li>all (To generate everything)</li>
   * </ul>
   * <li>Path to generate the action buttons</li>
   * <li>Path to generate the translated objects</li>
   * <li>Base package for the translation objects</li>
   * <li>Path to find the client web.xml file</li>
   * <li>Path to the project root</li>
   * <li>Path to the attached files</li>
   * <li>Url to the static web contents</li>
   * <li>Path to the src</li>
   * <li>Boolean to indicate if it's gonna be made a complete generation or not</li>
   * </ol>
   * 
   * @param argv
   *          Arguments array
   * @throws Exception
   */
  public static void main(String argv[]) throws Exception {
    PropertyConfigurator.configure("log4j.lcf");
    String strWindowName;
    String module;
    String dirFin;
    String dirReference;
    String dirWebXml;
    String dirActionButton;
    boolean generateWebXml;
    boolean generateTabs;
    String attachPath;
    String webPath;
    boolean complete;
    boolean quick;
    if (argv.length < 1) {
      log4j.error("Usage: java Wad connection.xml [{% || Window} [destinyDir]]");
      return;
    }
    final String strFileConnection = argv[0];
    final Wad wad = new Wad();
    wad.strSystemSeparator = System.getProperty("file.separator");
    wad.createPool(strFileConnection + "/Openbravo.properties");
    wad.createXmlEngine(strFileConnection);
    wad.readProperties(strFileConnection + "/Openbravo.properties");
    try {
      // the second parameter is the tab to be generated
      // if there is none it's * then all them are read
      strWindowName = argv[1];

      // the third parameter is the directory where the tab files are
      // created
      if (argv.length <= 2)
        dirFin = ".";
      else
        dirFin = argv[2];

      // the fourth paramenter is the directory where the references are
      // created
      // (TableList_data.xsql y TableDir_data.xsql)
      if (argv.length <= 3)
        dirReference = dirFin;
      else
        dirReference = argv[3];

      // the fifth parameter is the directory where web.xml is created
      if (argv.length <= 4)
        dirWebXml = dirFin;
      else
        dirWebXml = argv[4];

      // the sixth parementer indicates whether web.xml has to be
      // generated or not
      if (argv.length <= 5) {
        generateWebXml = true;
        generateTabs = true;
      } else if (argv[5].equals("web.xml")) {
        generateWebXml = true;
        generateTabs = false;
      } else if (argv[5].equals("tabs")) {
        generateWebXml = false;
        generateTabs = true;
      } else {
        generateWebXml = true;
        generateTabs = true;
      }

      // Path to generate the action button
      if (argv.length <= 6)
        dirActionButton = dirFin;
      else
        dirActionButton = argv[6];

      // Path to base translation generation
      // was argv[7] no longer used

      // Translate base structure
      // was argv[8] no longer used

      // Path to find the client's web.xml file
      // was argv[9] no longer used

      // Path of the root project
      // was argv[10] no longer used

      // Path of the attach files
      if (argv.length <= 11)
        attachPath = dirFin;
      else
        attachPath = argv[11];

      // Url to the static content
      if (argv.length <= 12)
        webPath = dirFin;
      else
        webPath = argv[12];

      // Path to the src folder
      // was argv[13] no longer used

      // Boolean to indicate if we are doing a complete generation
      if (argv.length <= 14)
        complete = false;
      else
        complete = ((argv[14].equals("true")) ? true : false);

      // Module to compile
      if (argv.length <= 15)
        module = "%";
      else
        module = argv[15].equals("%") ? "%" : "'"
            + argv[15].replace(", ", ",").replace(",", "', '") + "'";

      // Check for quick build
      if (argv.length <= 16)
        quick = false;
      else
        quick = argv[16].equals("quick");

      if (quick) {
        module = "%";
        strWindowName = "xx";
      }

      if (argv.length <= 17) {
        generateAllClassic250Windows = false;
      } else {
        generateAllClassic250Windows = argv[17].equals("true");
      }

      if (argv.length <= 18) {
        excludeCDI = false;
      } else {
        excludeCDI = argv[18].equals("true");
      }

      log4j.info("File connection: " + strFileConnection);
      log4j.info("window: " + strWindowName);
      log4j.info("module: " + module);
      log4j.info("directory destiny: " + dirFin);
      log4j.info("directory reference: " + dirReference + wad.strSystemSeparator + "reference");
      log4j.info("directory web.xml: " + dirWebXml);
      log4j.info("directory ActionButtons: " + dirActionButton);
      log4j.info("generate web.xml: " + generateWebXml);
      log4j.info("generate tabs: " + generateTabs);
      log4j.info("File separator: " + wad.strSystemSeparator);
      log4j.info("Attach path: " + attachPath);
      log4j.info("Web path: " + webPath);
      log4j.info("Quick mode: " + quick);
      // TODO: make 'new' boolean -> scope: test all button windows only
      log4j.info("Generate all 2.50 windows: " + generateAllClassic250Windows);
      log4j.info("Exclude CDI: " + excludeCDI);

      final File fileFin = new File(dirFin);
      if (!fileFin.exists()) {
        log4j.error("No such directory: " + fileFin.getAbsoluteFile());

        return;
      }

      final File fileFinReloads = new File(dirReference + wad.strSystemSeparator + "ad_callouts");
      if (!fileFinReloads.exists()) {
        log4j.error("No such directory: " + fileFinReloads.getAbsoluteFile());

        return;
      }

      final File fileReference = new File(dirReference + wad.strSystemSeparator + "reference");
      if (!fileReference.exists()) {
        log4j.error("No such directory: " + fileReference.getAbsoluteFile());

        return;
      }

      final File fileWebXml = new File(dirWebXml);
      if (!fileWebXml.exists()) {
        log4j.error("No such directory: " + fileWebXml.getAbsoluteFile());

        return;
      }

      final File fileActionButton = new File(dirActionButton);
      if (!fileActionButton.exists()) {
        log4j.error("No such directory: " + fileActionButton.getAbsoluteFile());

        return;
      }

      // Calculate windows to generate
      String strCurrentWindow;
      final StringTokenizer st = new StringTokenizer(strWindowName, ",", false);
      ArrayList<TabsData> td = new ArrayList<TabsData>();
      while (st.hasMoreTokens()) {
        strCurrentWindow = st.nextToken().trim();
        TabsData tabsDataAux[];
        if (quick)
          tabsDataAux = TabsData.selectQuick(wad.pool);
        else if (module.equals("%") || complete)
          tabsDataAux = TabsData.selectTabs(wad.pool, strCurrentWindow);
        else
          tabsDataAux = TabsData.selectTabsinModules(wad.pool, strCurrentWindow, module);
        td.addAll(Arrays.asList(tabsDataAux));
      }
      TabsData[] tabsData = td.toArray(new TabsData[0]);
      log4j.info(tabsData.length + " tabs to compile.");

      // Call to update the table identifiers
      log4j.info("Updating table identifiers");
      WadData.updateIdentifiers(wad.pool, quick ? "Y" : "N");

      // Call to generate audit trail infrastructure
      log4j.info("Re-generating audit trail infrastructure");
      WadData.updateAuditTrail(wad.pool);

      // If generateTabs parameter is true, the action buttons must be
      // generated
      if (generateTabs) {
        if (!quick || ProcessRelationData.generateActionButton(wad.pool)) {
          wad.processProcessComboReloads(fileFinReloads);
          wad.processActionButton(fileReference);
        } else {
          log4j.info("No changes in ActionButton_data.xml");
        }
        if (!quick || FieldsData.buildActionButton(wad.pool)) {
          wad.processActionButtonXml(fileActionButton);
          wad.processActionButtonHtml(fileActionButton);
        } else
          log4j.info("No changes in Action button for columns");
        if (!quick || ActionButtonRelationData.buildGenerics(wad.pool)) {
          wad.processActionButtonGenerics(fileActionButton);
          wad.processActionButtonXmlGenerics(fileActionButton);
          wad.processActionButtonHtmlGenerics(fileActionButton);
          wad.processActionButtonSQLDefaultGenerics(fileActionButton);
        } else
          log4j.info("No changes in generic action button responser");

      }

      Map<String, Boolean> generateTabMap = new HashMap<String, Boolean>();

      // calculate which windows/tabs are needed/requested
      // no-op now, as no longer supported
      calculateWindowsToGenerate(wad.pool, tabsData, new HashMap<String, Boolean>());
      generateTabMap = calculateTabsToGenerate(wad.pool, tabsData, new HashMap<String, Boolean>());
      int skip = 0;
      int generate = 0;
      for (Boolean b : generateTabMap.values()) {
        if (b) {
          generate++;
        } else {
          skip++;
        }
      }
      log4j.info("After filtering generating " + generate + " tabs and skipping " + skip);

      // If generateWebXml parameter is true, the web.xml file should be
      // generated
      if (generateWebXml) {

        if (!quick || WadData.genereteWebXml(wad.pool))
          wad.processWebXml(fileWebXml, attachPath, webPath, generateTabMap);
        else
          log4j.info("No changes in web.xml");
      }

      if (tabsData.length == 0)
        log4j.info("No windows to compile");

      if (generateTabs) {
        for (int i = 0; i < tabsData.length; i++) {
          // don't compile if it is in an unactive branch
          if (wad.allTabParentsActive(tabsData[i].tabid)) {
            boolean tabJavaNeeded = generateAllClassic250Windows
                || generateTabMap.get(tabsData[i].tabid);

            if (tabJavaNeeded) {
              log4j.info("Processing Window: " + tabsData[i].windowname + " - Tab: "
                  + tabsData[i].tabname + " - id: " + tabsData[i].tabid);
              wad.processTab(fileFin, fileFinReloads, tabsData[i]);
            } else {
              log4j.debug("Skipped Window: " + tabsData[i].windowname + " - Tab: "
                  + tabsData[i].tabname + " - id: " + tabsData[i].tabid);
            }
          }
        }
      }

    } catch (final Exception e) {

      throw new Exception(e);
    } finally {
      wad.pool.destroy();
    }
  }

  private static Map<String, Boolean> calculateWindowsToGenerate(ConnectionProvider conn,
      FieldProvider[] tabsData, Map<String, Boolean> calculatedWindowMap) throws ServletException {

    // check if some tabs/windows need to be shown in classic mode
    Map<String, Boolean> generateWindowMap = new HashMap<String, Boolean>();
    String oldWindowId = null;
    for (FieldProvider tab : tabsData) {
      if (calculatedWindowMap.get(tab.getField("key")) != null) {
        // if already calculated before set if
        generateWindowMap.put(tab.getField("key"), calculatedWindowMap.get(tab.getField("key")));
        continue;
      }
      if (oldWindowId == null || !tab.getField("key").equals(oldWindowId)) {
        // new window -> check all tabs in that window
        boolean res = TabsData.selectShowWindowIn250ClassicMode(conn, tab.getField("key"));
        if (res) {
          log4j
              .error("Window: "
                  + tab.getField("windowname")
                  + " is needed in classic 2.50 mode. This is no longer supported. The module containing the window must be fixed.");
        } else {
          res = TabsData.selectShowWindowIn250ClassicModePreference(conn, tab.getField("key"));
          if (res) {
            log4j
                .error("Window: "
                    + tab.getField("windowname")
                    + " is configured for classic 2.50 mode via preferences. This is no longer supported...");
          }
        }
        oldWindowId = tab.getField("key");
      }
    }
    return generateWindowMap;
  }

  private static Map<String, Boolean> calculateTabsToGenerate(ConnectionProvider conn,
      FieldProvider[] tabsData, Map<String, Boolean> calculatedTabs) throws ServletException {
    Map<String, Boolean> res = new HashMap<String, Boolean>();

    for (FieldProvider tab : tabsData) {
      // if already calculated before skip
      if (res.get(tab.getField("tabid")) != null) {
        continue;
      }

      if (calculatedTabs.get(tab.getField("tabid")) != null) {
        res.put(tab.getField("tabid"), calculatedTabs.get(tab.getField("tabid")));
        continue;
      }

      boolean needToCompile = tabJavaNeededforActionButtons(conn, tab.getField("tabid"));

      if (needToCompile && "Y".equals(tab.getField("issorttab"))) {
        log4j.warn("2.50 Sort Tab no longer supported (it will be skipped): "
            + tab.getField("tabname") + ",id: " + tab.getField("tabid"));
        res.put(tab.getField("tabid"), Boolean.FALSE);
        continue;
      }

      if (needToCompile) {
        log4j.debug("Need to generate tab: " + tab.getField("tabname") + ",id: "
            + tab.getField("tabid") + ", level: " + tab.getField("tablevel"));
        res.put(tab.getField("tabid"), Boolean.TRUE);
      } else {
        // mark as not needed to compile
        res.put(tab.getField("tabid"), Boolean.FALSE);
      }
    }

    return res;
  }

  private static boolean tabJavaNeededforActionButtons(ConnectionProvider conn, String tabId)
      throws ServletException {
    ActionButtonRelationData[] actBtns = ActionButtonRelationData.select(conn, tabId);
    ActionButtonRelationData[] actBtnsJava = ActionButtonRelationData.selectJava(conn, tabId);

    if ((actBtns == null || actBtns.length == 0)
        && (actBtnsJava == null || actBtnsJava.length == 0)
        && FieldsData.hasPostedButton(conn, tabId).equals("0")
        && FieldsData.hasCreateFromButton(conn, tabId).equals("0")) {
      // No action buttons
      return false;
    }

    return true;
  }

  private boolean allTabParentsActive(String tabId) {
    try {
      if (!TabsData.isTabActive(pool, tabId))
        return false;
      else {
        String parentTabId = TabsData.selectParentTab(pool, tabId);
        if (parentTabId != null && !parentTabId.equals(""))
          return allTabParentsActive(parentTabId);
      }
      return true;
    } catch (Exception e) {
      return true;
    }
  }

  /**
   * Generates the action button's xsql files
   * 
   * @param fileReference
   *          The path where to create the files.
   */
  private void processActionButton(File fileReference) {
    try {
      log4j.info("Processing ActionButton_data.xml");
      final XmlDocument xmlDocumentData = xmlEngine.readXmlTemplate(
          "org/openbravo/wad/ActionButton_data").createXmlDocument();
      final ProcessRelationData ard[] = ProcessRelationData.select(pool);

      xmlDocumentData.setData("structure1", ard);
      WadUtility.writeFile(fileReference, "ActionButton_data.xsql",
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + xmlDocumentData.print());
    } catch (final ServletException e) {
      e.printStackTrace();
      log4j.error("Problem of ServletExceptio in process of ActionButtonData");
    } catch (final IOException e) {
      e.printStackTrace();
      log4j.error("Problem of IOExceptio in process of ActionButtonData");
    }
  }

  /**
   * Generates the action button's xml files
   * 
   * @param fileReference
   *          The path where to create the files.
   */
  private void processActionButtonXml(File fileReference) {
    try {
      log4j.info("Processing ActionButtonXml");
      final FieldsData fd[] = FieldsData.selectActionButton(pool);
      if (fd != null) {
        for (int i = 0; i < fd.length; i++) {
          final Vector<Object> vecFields = new Vector<Object>();
          WadActionButton.buildXml(pool, xmlEngine, fileReference, fd[i], vecFields,
              MAX_TEXTBOX_LENGTH);
        }
      }
    } catch (final ServletException e) {
      e.printStackTrace();
      log4j.error("Problem of ServletExceptio in process of ActionButtonXml");
    } catch (final IOException e) {
      e.printStackTrace();
      log4j.error("Problem of IOExceptio in process of ActionButtonXml");
    }
  }

  /**
   * Generates the action button's html files
   * 
   * @param fileReference
   *          The path where to create the files.
   */
  private void processActionButtonHtml(File fileReference) {
    try {
      log4j.info("Processing ActionButtonHtml");
      final FieldsData fd[] = FieldsData.selectActionButton(pool);
      if (fd != null) {
        for (int i = 0; i < fd.length; i++) {
          final Vector<Object> vecFields = new Vector<Object>();

          // calculate fields that need combo reload
          final FieldsData[] dataReload = FieldsData.selectValidationProcess(pool, fd[i].reference);

          final Vector<Object> vecReloads = new Vector<Object>();
          if (dataReload != null && dataReload.length > 0) {
            for (int z = 0; z < dataReload.length; z++) {
              String code = dataReload[z].whereclause
                  + ((!dataReload[z].whereclause.equals("") && !dataReload[z].referencevalue
                      .equals("")) ? " AND " : "") + dataReload[z].referencevalue;

              if (code.equals("") && dataReload[z].type.equals("R"))
                code = "@AD_Org_ID@";
              WadUtility.getComboReloadText(code, vecFields, null, vecReloads, "",
                  dataReload[z].columnname);
            }
          }

          // build the html template
          WadActionButton.buildHtml(pool, xmlEngine, fileReference, fd[i], vecFields,
              MAX_TEXTBOX_LENGTH, MAX_SIZE_EDITION_1_COLUMNS, "", false, jsDateFormat, vecReloads);
        }
      }
    } catch (final ServletException e) {
      e.printStackTrace();
      log4j.error("Problem of ServletExceptio in process of ActionButtonHtml");
    } catch (final IOException e) {
      e.printStackTrace();
      log4j.error("Problem of IOExceptio in process of ActionButtonHtml");
    }
  }

  /**
   * Generates the main file to manage the action buttons (ActionButton_Responser.java). These are
   * the menu's action buttons.
   * 
   * @param fileReference
   *          The path where to create the files.
   */
  private void processActionButtonGenerics(File fileReference) {
    try {
      // Generic action button for jasper and PL
      log4j.info("Processing ActionButton_Responser.xml");
      XmlDocument xmlDocumentData = xmlEngine.readXmlTemplate(
          "org/openbravo/wad/ActionButton_Responser").createXmlDocument();

      ActionButtonRelationData[] abrd = WadActionButton.buildActionButtonCallGenerics(pool);
      xmlDocumentData.setData("structure1", abrd);
      xmlDocumentData.setData("structure2", abrd);
      xmlDocumentData.setData("structure3", abrd);
      xmlDocumentData.setData("structure4", abrd);

      WadUtility.writeFile(fileReference, "ActionButton_Responser.java", xmlDocumentData.print());

      // Generic action button for java
      log4j.info("Processing ActionButton_ResponserJava.xml");
      xmlDocumentData = xmlEngine.readXmlTemplate("org/openbravo/wad/ActionButtonJava_Responser")
          .createXmlDocument();
      abrd = WadActionButton.buildActionButtonCallGenericsJava(pool);

      xmlDocumentData.setData("structure1", abrd);
      xmlDocumentData.setData("structure2", abrd);
      xmlDocumentData.setData("structure3", abrd);
      xmlDocumentData.setData("structure4", abrd);

      WadUtility.writeFile(fileReference, "ActionButtonJava_Responser.java",
          xmlDocumentData.print());

    } catch (final IOException e) {
      e.printStackTrace();
      log4j.error("Problem of IOExceptio in process of ActionButton_Responser");
    }
  }

  /**
   * Generates the action button's xsql file for the action buttons called directly from menu. This
   * xsql file contains all the queries needed for SQL default values in generated parameters.
   * 
   * @param fileReference
   *          The path where to create the files.
   */
  private void processActionButtonSQLDefaultGenerics(File fileReference) {
    try {
      log4j.info("Processing ActionButtonDefault_data.xsql");

      ProcessRelationData defaults[] = ProcessRelationData.selectXSQLGenericsParams(pool);
      if (defaults != null && defaults.length > 0) {
        for (int i = 0; i < defaults.length; i++) {
          final Vector<Object> vecParametros = new Vector<Object>();
          defaults[i].reference = defaults[i].adProcessId + "_"
              + FormatUtilities.replace(defaults[i].columnname);
          defaults[i].defaultvalue = WadUtility.getSQLWadContext(defaults[i].defaultvalue,
              vecParametros);
          final StringBuffer parametros = new StringBuffer();
          for (final Enumeration<Object> e = vecParametros.elements(); e.hasMoreElements();) {
            final String paramsElement = WadUtility.getWhereParameter(e.nextElement(), true);
            parametros.append("\n" + paramsElement);
          }
          defaults[i].whereclause = parametros.toString();
        }
        XmlDocument xmlDocumentData = xmlEngine.readXmlTemplate(
            "org/openbravo/wad/ActionButtonDefault_data").createXmlDocument();
        xmlDocumentData.setData("structure16", defaults);

        WadUtility.writeFile(fileReference, "ActionButtonSQLDefault_data.xsql",
            xmlDocumentData.print());
      }
    } catch (final Exception e) {
      log4j.error(e);
    }
  }

  /**
   * Generates the action button's xml files. These are the menu's action buttons.
   * 
   * @param fileReference
   *          The path where to create the files.
   */
  private void processActionButtonXmlGenerics(File fileReference) {
    try {
      log4j.info("Processing ActionButtonXml Generics");
      final FieldsData fd[] = FieldsData.selectActionButtonGenerics(pool);
      if (fd != null) {
        for (int i = 0; i < fd.length; i++) {
          final Vector<Object> vecFields = new Vector<Object>();
          WadActionButton.buildXml(pool, xmlEngine, fileReference, fd[i], vecFields,
              MAX_TEXTBOX_LENGTH);
        }
      }
    } catch (final ServletException e) {
      e.printStackTrace();
      log4j.error("Problem of ServletExceptio in process of ActionButtonXml Generics");
    } catch (final IOException e) {
      e.printStackTrace();
      log4j.error("Problem of IOExceptio in process of ActionButtonXml Generics");
    }
  }

  /**
   * Generates the action button's html files. These are the menu's action button
   * 
   * @param fileReference
   *          The path where to create the files.
   */
  private void processActionButtonHtmlGenerics(File fileReference) {
    try {
      log4j.info("Processing ActionButtonHtml for generics");
      final FieldsData fd[] = FieldsData.selectActionButtonGenerics(pool);
      if (fd != null) {
        for (int i = 0; i < fd.length; i++) {
          final Vector<Object> vecFields = new Vector<Object>();

          // calculate fields that need combo reload
          final FieldsData[] dataReload = FieldsData.selectValidationProcess(pool, fd[i].reference);

          final Vector<Object> vecReloads = new Vector<Object>();
          if (dataReload != null && dataReload.length > 0) {
            for (int z = 0; z < dataReload.length; z++) {
              String code = dataReload[z].whereclause
                  + ((!dataReload[z].whereclause.equals("") && !dataReload[z].referencevalue
                      .equals("")) ? " AND " : "") + dataReload[z].referencevalue;

              if (code.equals("") && dataReload[z].type.equals("R"))
                code = "@AD_Org_ID@";
              WadUtility.getComboReloadText(code, vecFields, null, vecReloads, "",
                  dataReload[z].columnname);
            }
          }

          // build the html template
          WadActionButton.buildHtml(pool, xmlEngine, fileReference, fd[i], vecFields,
              MAX_TEXTBOX_LENGTH, MAX_SIZE_EDITION_1_COLUMNS, "", true, jsDateFormat, vecReloads);
        }
      }
    } catch (final ServletException e) {
      e.printStackTrace();
      log4j.error("Problem of ServletExceptio in process of ActionButtonHtml Generics");
    } catch (final IOException e) {
      e.printStackTrace();
      log4j.error("Problem of IOExceptio in process of ActionButtonHtml Generics");
    }
  }

  /**
   * Generates the web.xml file
   * 
   * @param fileWebXml
   *          path to generate the new web.xml file.
   * @param attachPath
   *          The path where are the attached files.
   * @param webPath
   *          The url where are the static web content.
   * @param calculatedTabMap
   * @throws ServletException
   * @throws IOException
   */
  private void processWebXml(File fileWebXml, String attachPath, String webPath,
      Map<String, Boolean> calculatedTabMap) throws ServletException, IOException {
    try {
      log4j.info("Processing web.xml");
      final XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/wad/webConf")
          .createXmlDocument();

      xmlDocument.setParameter("webPath", webPath);
      xmlDocument.setParameter("attachPath", attachPath);

      String excludeWeldListener = excludeCDI ? WELD_LISTENER_ID : NONE;
      xmlDocument.setData("structureListener", WadData.selectListener(pool, excludeWeldListener));

      xmlDocument.setData("structureResource", WadData.selectResource(pool));
      final WadData[] filters = WadData.selectFilter(pool);
      WadData[][] filterParams = null;
      if (filters != null && filters.length > 0) {
        filterParams = new WadData[filters.length][];
        for (int i = 0; i < filters.length; i++) {
          filterParams[i] = WadData.selectParams(pool, "F", filters[i].classname, filters[i].id);
        }
      } else
        filterParams = new WadData[0][0];
      xmlDocument.setData("structureFilter", filters);
      xmlDocument.setDataArray("reportFilterParams", "structure1", filterParams);

      WadData[] contextParams = WadData.selectContextParams(pool);
      xmlDocument.setData("structureContextParams", contextParams);

      WadData[] allTabs = WadData.selectAllTabs(pool);

      xmlDocument.setData("structureServletTab", getTabServlets(allTabs, calculatedTabMap));
      xmlDocument.setData("structureMappingTab", getTabMappings(allTabs, calculatedTabMap));

      final WadData[] servlets = WadData.select(pool);
      WadData[][] servletParams = null;
      if (servlets != null && servlets.length > 0) {
        servletParams = new WadData[servlets.length][];
        for (int i = 0; i < servlets.length; i++) {
          if (servlets[i].loadonstartup != null && !servlets[i].loadonstartup.equals(""))
            servlets[i].loadonstartup = "<load-on-startup>" + servlets[i].loadonstartup
                + "</load-on-startup>";
          servletParams[i] = WadData.selectParams(pool, "S", servlets[i].classname, servlets[i].id);
        }
      } else
        servletParams = new WadData[0][0];

      WadData[] timeout = WadData.selectSessionTimeOut(pool);
      if (timeout.length == 0) {
        log4j.info("No session timeout found, setting default 60min");
      } else if (timeout.length > 1) {
        log4j.error("Multiple session timeout config found (" + timeout.length
            + "), setting default 60min");
      } else {
        xmlDocument.setParameter("fieldSessionTimeOut", timeout[0].value);
      }

      xmlDocument.setData("structure1", servlets);
      xmlDocument.setDataArray("reportServletParams", "structure1", servletParams);
      xmlDocument.setData("structureFilterMapping", WadData.selectFilterMapping(pool));
      xmlDocument.setData("structure2", WadData.selectMapping(pool));

      String webXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + xmlDocument.print();
      webXml = webXml.replace("${attachPath}", attachPath);
      webXml = webXml.replace("${webPath}", webPath);

      WadUtility.writeFile(fileWebXml, "web.xml", webXml);
    } catch (final IOException e) {
      e.printStackTrace();
      log4j.error("Problem of IOException in process of Web.xml");
    }
  }

  private WadData[] getTabServlets(WadData[] allTabs, Map<String, Boolean> generateTabMap) {
    ArrayList<WadData> servlets = new ArrayList<WadData>();
    for (WadData tab : allTabs) {
      boolean tabJavaNeeded = generateAllClassic250Windows
          || (generateTabMap.get(tab.tabid) == null) || generateTabMap.get(tab.tabid);

      if (!tabJavaNeeded) {
        continue;
      }

      String tabClassName = "org.openbravo.erpWindows."
          + ("0".equals(tab.windowmodule) ? "" : tab.windowpackage + ".") + tab.windowname + "."
          + tab.tabname + ("0".equals(tab.tabmodule) ? "" : tab.tabid);

      WadData servlet = new WadData();
      servlet.displayname = tabClassName;
      servlet.name = "W" + tab.tabid;
      servlet.classname = tabClassName;
      servlets.add(servlet);
    }
    return servlets.toArray(new WadData[servlets.size()]);
  }

  private FieldProvider[] getTabMappings(WadData[] allTabs, Map<String, Boolean> generateTabMap) {
    ArrayList<WadData> mappings = new ArrayList<WadData>();
    for (WadData tab : allTabs) {
      boolean tabJavaNeeded = generateAllClassic250Windows
          || ((generateTabMap.get(tab.tabid) != null) && (generateTabMap.get(tab.tabid)));

      if (!tabJavaNeeded) {
        continue;
      }

      String prefix = "/" + ("0".equals(tab.windowmodule) ? "" : tab.windowpackage)
          + tab.windowname + "/" + tab.tabname + ("0".equals(tab.tabmodule) ? "" : tab.tabid);

      // Keeping mapping to *_Edition.html because it is the mapping used for processes
      WadData mapping2 = new WadData();
      mapping2.name = "W" + tab.tabid;
      mapping2.classname = prefix + "_Edition.html";
      mappings.add(mapping2);
    }
    return mappings.toArray(new WadData[mappings.size()]);
  }

  /**
   * Generates all the windows defined in the dictionary. Also generates the translated files for
   * the defineds languages.
   * 
   * @param fileFin
   *          Path where are gonna be created the sources.
   * @param fileFinReloads
   *          Path where are gonna be created the reloads sources.
   * @param tabsData
   *          An object containing the tabs info.
   * @throws Exception
   */
  private void processTab(File fileFin, File fileFinReloads, TabsData tabsData) throws Exception {
    try {
      final String tabNamePresentation = tabsData.realtabname;
      // tabName contains tab's UUID for non core tabs
      final String tabName = FormatUtilities.replace(tabNamePresentation)
          + (tabsData.tabmodule.equals("0") ? "" : tabsData.tabid);
      final String windowName = FormatUtilities.replace(tabsData.windowname);
      final String tableName = FieldsData.tableName(pool, tabsData.tabid);
      final String isSOTrx = FieldsData.isSOTrx(pool, tabsData.tabid);
      final TabsData[] allTabs = getPrimaryTabs(tabsData.key, tabsData.tabid,
          Integer.valueOf(tabsData.tablevel).intValue(), HEIGHT_TABS, INCR_TABS);
      final FieldsData[] fieldsData = FieldsData.select(pool, tabsData.tabid);
      final EditionFieldsData efd[] = EditionFieldsData.select(pool, tabsData.tabid);
      final EditionFieldsData efdauxiliar[] = EditionFieldsData
          .selectAuxiliar(pool, tabsData.tabid);

      /************************************************
       * The 2 tab lines generation
       *************************************************/
      if (allTabs == null || allTabs.length == 0)
        throw new Exception("No tabs found for AD_Tab_ID: " + tabsData.tabid + " - key: "
            + tabsData.key + " - level: " + tabsData.tablevel);
      final TabsData[] tab1 = new TabsData[(allTabs.length > NUM_TABS) ? NUM_TABS : allTabs.length];
      final TabsData[] tab2 = new TabsData[(allTabs.length > NUM_TABS) ? NUM_TABS : 0];
      for (int i = 0; i < NUM_TABS && i < allTabs.length; i++) {
        tab1[i] = allTabs[i];
      }
      if (allTabs.length > NUM_TABS) {
        int j = 0;
        for (int i = allTabs.length - NUM_TABS; i < allTabs.length; i++)
          tab2[j++] = allTabs[i];
      }

      int parentTabIndex = -1;
      String grandfatherField = "";
      if (allTabs != null && allTabs.length > 0)
        parentTabIndex = parentTabId(allTabs, tabsData.tabid);
      FieldsData[] parentsFieldsData = null;

      parentsFieldsData = FieldsData.parentsColumnName(pool,
          (parentTabIndex != -1 ? allTabs[parentTabIndex].tabid : ""), tabsData.tabid);

      if (parentTabIndex != -1 && (parentsFieldsData == null || parentsFieldsData.length == 0)) {
        parentsFieldsData = FieldsData.parentsColumnReal(pool, allTabs[parentTabIndex].tabid,
            tabsData.tabid);
        if (parentsFieldsData == null || parentsFieldsData.length == 0) {
          log4j.info("No key found in parent tab: " + allTabs[parentTabIndex].tabname);
        }
      }

      final Vector<Object> vecFields = new Vector<Object>();
      final Vector<Object> vecTables = new Vector<Object>();
      final Vector<Object> vecWhere = new Vector<Object>();
      final Vector<Object> vecOrder = new Vector<Object>();
      final Vector<Object> vecParameters = new Vector<Object>();
      final Vector<Object> vecTableParameters = new Vector<Object>();
      final Vector<Object> vecTotalParameters = new Vector<Object>();
      final Vector<String> vecFieldParameters = new Vector<String>();
      processTable(parentsFieldsData, tabsData.tabid, vecFields, vecTables, vecWhere, vecOrder,
          vecParameters, tableName, tabsData.windowtype, tabsData.tablevel, vecTableParameters,
          fieldsData, vecFieldParameters);
      for (int i = 0; i < vecTableParameters.size(); i++) {
        vecTotalParameters.addElement(vecTableParameters.elementAt(i));
      }
      for (int i = 0; i < vecParameters.size(); i++) {
        vecTotalParameters.addElement(vecParameters.elementAt(i));
      }

      final StringBuffer strTables = new StringBuffer();
      for (final Enumeration<Object> e = vecTables.elements(); e.hasMoreElements();) {
        final String tableElement = (String) e.nextElement();
        strTables.append((tableElement.trim().toLowerCase().startsWith("left join") ? " " : ", ")
            + tableElement);
      }
      log4j.debug("Tables of select: " + strTables.toString());

      EditionFieldsData[] selCol = EditionFieldsData.selectSerchFieldsSelection(pool, "",
          tabsData.tabid);
      if (selCol == null || selCol.length == 0)
        selCol = EditionFieldsData.selectSerchFields(pool, "", tabsData.tabid);
      selCol = processSelCol(selCol, tableName);

      final String javaPackage = (!tabsData.javapackage.equals("") ? tabsData.javapackage.replace(
          ".", "/") + "/" : "")
          + windowName; // Take into account
                        // java packages for
                        // modules
      final File fileDir = new File(fileFin, javaPackage);

      int grandfatherTabIndex = -1;
      FieldsData auxFieldsData[] = null;
      if (parentTabIndex != -1 && allTabs != null && allTabs.length > 0) {
        final Vector<Object> vecParametersParent = new Vector<Object>();
        if (vecParametersParent.size() > 0) {
          ArrayList<String> usedParameters = new ArrayList<String>();
          for (int h = 0; h < vecParametersParent.size(); h++) {
            String strParam = WadUtility.getWhereParameter(vecParametersParent.get(h), false);

            if (!usedParameters.contains(strParam)) {
              usedParameters.add(strParam);
            }
          }
        }
        grandfatherTabIndex = parentTabId(allTabs, allTabs[parentTabIndex].tabid);
        auxFieldsData = FieldsData.parentsColumnName(pool,
            (grandfatherTabIndex != -1 ? allTabs[grandfatherTabIndex].tabid : ""),
            allTabs[parentTabIndex].tabid);
        if (grandfatherTabIndex != -1 && (auxFieldsData == null || auxFieldsData.length == 0)) {
          auxFieldsData = FieldsData.parentsColumnReal(pool, allTabs[grandfatherTabIndex].tabid,
              allTabs[parentTabIndex].tabid);
        }
      }
      if (auxFieldsData != null && auxFieldsData.length > 0)
        grandfatherField = auxFieldsData[0].name;
      auxFieldsData = null;
      String keyColumnName = "";
      boolean isSecondaryKey = false;
      final FieldsData[] dataKey = FieldsData.keyColumnName(pool, tabsData.tabid);
      if (dataKey != null && dataKey.length > 0) {
        keyColumnName = dataKey[0].name;
        isSecondaryKey = dataKey[0].issecondarykey.equals("Y");
      }
      log4j.debug("KeyColumnName: " + keyColumnName);
      String strProcess = "", strDirectPrint = "";
      if (!tabsData.adProcessId.equals("")) {
        strProcess = TabsData.processName(pool, tabsData.adProcessId);
        if (strProcess.indexOf("/") == -1)
          strProcess = "/" + FormatUtilities.replace(strProcess);
        strDirectPrint = TabsData.directPrint(pool, tabsData.adProcessId);
      }
      WADGrid gridControl = null;
      {
        final Properties gridProps = new Properties();
        gridProps.setProperty("id", "grid");
        gridProps.setProperty("NumRows", "20");
        gridProps.setProperty("width", "99%");
        gridProps.setProperty("ShowLineNumbers", "true");
        gridProps.setProperty("editable", "false");
        gridProps.setProperty("sortable", "true");
        gridProps.setProperty("deleteable", (tabsData.uipattern.equals("STD") ? "true" : "false"));
        gridProps.setProperty("onScrollFunction", "updateHeader");
        gridProps.setProperty("onLoadFunction", "onGridLoadDo");
        gridProps.setProperty("AD_Window_ID", tabsData.key);
        gridProps.setProperty("AD_Tab_ID", tabsData.tabid);
        gridProps.setProperty("ColumnName", keyColumnName);
        gridProps.setProperty("inpKeyName", "inp" + Sqlc.TransformaNombreColumna(keyColumnName));
        gridControl = new WADGrid(gridProps);
      }

      /************************************************
       * JAVA
       *************************************************/
      processTabJava(efd, efdauxiliar, parentsFieldsData, fileDir, tabsData.tabid, tabName,
          tableName, windowName, keyColumnName, strTables.toString(), vecFields, vecParameters,
          isSOTrx, allTabs, tabsData.key, tabsData.accesslevel, selCol, isSecondaryKey,
          grandfatherField, tabsData.tablevel, tabsData.tableId, tabsData.windowtype,
          tabsData.uipattern, tabsData.editreference, strProcess, strDirectPrint,
          vecTableParameters, fieldsData, gridControl, tabsData.javapackage,
          "Y".equals(tabsData.isdeleteable), tabsData.tabmodule);

      /************************************************
       * XSQL
       *************************************************/
      processTabXSQL(parentsFieldsData, fileDir, tabsData.tabid, tabName, tableName, windowName,
          keyColumnName, strTables.toString(), vecParameters, selCol, tabsData.tablevel,
          tabsData.windowtype, vecTableParameters, fieldsData, isSecondaryKey,
          tabsData.javapackage, vecFieldParameters);

    } catch (final ServletException e) {
      e.printStackTrace();
      log4j.error("Problem of ServletException in the file: " + tabsData.tabid);
    } catch (final IOException e) {
      e.printStackTrace();
      log4j.error("Problem at close of the file: " + tabsData.tabid);
    } catch (final Exception e) {
      e.printStackTrace();
      log4j.error("Problem at close of the file: " + tabsData.tabid);
    }
  }

  /**
   * Generates all the info to build the selection columns structure.
   * 
   * @param selCol
   *          The array with the info of the selection columns.
   * @param tableName
   *          The name of the selection column's table.
   * @return Array with the selection columns info.
   */
  private EditionFieldsData[] processSelCol(EditionFieldsData[] selCol, String tableName) {
    final Vector<Object> vecAuxSelCol = new Vector<Object>(0);
    final Vector<Object> vecSelCol = new Vector<Object>(0);
    if (selCol != null) {
      for (int i = 0; i < selCol.length; i++) {

        selCol[i].htmltext = "strParam" + selCol[i].columnname + ".equals(\"\")";
        selCol[i].columnnameinp = FormatUtilities.replace(selCol[i].columnname);
        WADControl control = WadUtility.getWadControlClass(pool, selCol[i].reference,
            selCol[i].referencevalue);
        control.processSelCol(tableName, selCol[i], vecAuxSelCol);

        vecSelCol.addElement(selCol[i]);
      }
      for (int i = 0; i < vecAuxSelCol.size(); i++)
        vecSelCol.addElement(vecAuxSelCol.elementAt(i));
      return vecSelCol.toArray(new EditionFieldsData[0]);
    }
    return selCol;
  }

  /**
   * Generates the structure for the query fields.
   * 
   * @param parentsFieldsData
   *          Array with the parents fields.
   * @param strTab
   *          The id of the tab.
   * @param vecFields
   *          Vector of query fields (select fields).
   * @param vecTables
   *          Vector of query tables (from tables).
   * @param vecWhere
   *          Vector of where clauses.
   * @param vecOrder
   *          Vector of order clauses.
   * @param vecParameters
   *          Vector of query parameters.
   * @param tableName
   *          The name of the table.
   * @param windowType
   *          The type of window.
   * @param tablevel
   *          The tab level.
   * @param vecTableParameters
   *          Vector of the from clause parameters.
   * @param fieldsDataSelectAux
   *          Array with the fields of the tab.
   * @param vecFieldParameters
   * @throws ServletException
   * @throws IOException
   */
  private void processTable(FieldsData[] parentsFieldsData, String strTab,
      Vector<Object> vecFields, Vector<Object> vecTables, Vector<Object> vecWhere,
      Vector<Object> vecOrder, Vector<Object> vecParameters, String tableName, String windowType,
      String tablevel, Vector<Object> vecTableParameters, FieldsData[] fieldsDataSelectAux,
      Vector<String> vecFieldParameters) throws ServletException, IOException {
    int ilist = 0;
    final int itable = 0;
    final Vector<Object> vecCounters = new Vector<Object>();
    final Vector<Object> vecOrderAux = new Vector<Object>();
    vecCounters.addElement(Integer.toString(itable));
    vecCounters.addElement(Integer.toString(ilist));
    FieldsData[] fieldsData = null;
    fieldsData = copyarray(fieldsDataSelectAux);
    for (int i = 0; i < fieldsData.length; i++) {
      if (!fieldsData[i].columnname.equalsIgnoreCase("Created")
          && !fieldsData[i].columnname.equalsIgnoreCase("CreatedBy")
          && !fieldsData[i].columnname.equalsIgnoreCase("Updated")
          && !fieldsData[i].columnname.equalsIgnoreCase("UpdatedBy")) {

        // hardcoded these special cases
        if (fieldsData[i].reference.equals("24")) {
          vecFields.addElement("TO_CHAR(" + tableName + "." + fieldsData[i].name
              + ", 'HH24:MI:SS') AS " + fieldsData[i].name);
        } else if (fieldsData[i].reference.equals("20")) {
          vecFields.addElement("COALESCE(" + tableName + "." + fieldsData[i].name + ", 'N') AS "
              + fieldsData[i].name);
        } else if (fieldsData[i].reference.equals("16")) { // datetime
          vecFields.addElement("TO_CHAR(" + tableName + "." + fieldsData[i].name + ", ?) AS "
              + fieldsData[i].name);
          vecFieldParameters.addElement("<Parameter name=\"dateTimeFormat\"/>");
        } else {
          vecFields.addElement(tableName + "." + fieldsData[i].name);
        }

        WADControl control = WadUtility.getWadControlClass(pool, fieldsData[i].reference,
            fieldsData[i].referencevalue);
        control.processTable(strTab, vecFields, vecTables, vecWhere, vecOrderAux, vecParameters,
            tableName, vecTableParameters, fieldsData[i], vecFieldParameters, vecCounters);
      }
    }
    final FieldsData sfd1[] = FieldsData.selectSequence(pool, strTab);
    if (sfd1 != null && sfd1.length > 0) {
      for (int i = 0; i < sfd1.length; i++) {
        final String aux = findOrderVector(vecOrderAux, sfd1[i].name);
        if (aux != null && aux.length() > 0)
          vecOrder.addElement(aux);
      }
    }
  }

  /**
   * Searchs a field in the order vector and returns the column name.
   * 
   * @param vecOrder
   *          Vector with the order fields
   * @param name
   *          The name of the field to find
   * @return String with the name of the column.
   */
  private String findOrderVector(Vector<Object> vecOrder, String name) {
    if (vecOrder.size() == 0 || name.equals(""))
      return "";
    for (int i = 0; i < vecOrder.size(); i++) {
      final String[] aux = (String[]) vecOrder.elementAt(i);
      if (aux[0].equalsIgnoreCase(name))
        return aux[1];
    }
    return "";
  }

  /**
   * Generates the java files for a normal tab type.
   * 
   * @param allfields
   *          Array with the fields of the tab.
   * @param auxiliarsData
   *          Array with the auxiliar inputs for this tab.
   * @param parentsFieldsData
   *          Array with the parents fields for the tab.
   * @param fileDir
   *          Path where to build the file.
   * @param strTab
   *          The id of the tab.
   * @param tabName
   *          The name of the tab.
   * @param tableName
   *          The name of the tab's table.
   * @param windowName
   *          The name of the window.
   * @param keyColumnName
   *          The name of the key column.
   * @param strTables
   *          String with the from clause.
   * @param vecFields
   *          Vector with the fields of the tab.
   * @param vecParametersTop
   *          Vector with parameters for the query.
   * @param isSOTrx
   *          String that indicates if is a Sales Order tab or not (Y | N).
   * @param allTabs
   *          Array with all the tabs.
   * @param strWindow
   *          The id of the window.
   * @param accesslevel
   *          The access level.
   * @param selCol
   *          Array with the selection columns.
   * @param isSecondaryKey
   *          Boolean that identifies if the key column is a secondary key.
   * @param grandfatherField
   *          The grandfather column of the tab.
   * @param tablevel
   *          The tab level.
   * @param tableId
   *          The id of the tab's table.
   * @param windowType
   *          The tab's window type.
   * @param uiPattern
   *          The patter for the tab.
   * @param editReference
   *          The id of the manual tab for the edition mode.
   * @param strProcess
   *          The id of the tab's process.
   * @param strDirectPrint
   *          If is a direct printing type process (Y | N).
   * @param vecTableParametersTop
   *          Vector with parameters for the from clause of the query.
   * @param fieldsDataSelectAux
   *          Array with the auxiliar inputs info
   * @param relationControl
   *          Object with the WADGrid control
   * @param tabmodule
   * @throws ServletException
   * @throws IOException
   */
  private void processTabJava(EditionFieldsData[] allfields, EditionFieldsData[] auxiliarsData,
      FieldsData[] parentsFieldsData, File fileDir, String strTab, String tabName,
      String tableName, String windowName, String keyColumnName, String strTables,
      Vector<Object> vecFields, Vector<Object> vecParametersTop, String isSOTrx,
      TabsData[] allTabs, String strWindow, String accesslevel, EditionFieldsData[] selCol,
      boolean isSecondaryKey, String grandfatherField, String tablevel, String tableId,
      String windowType, String uiPattern, String editReference, String strProcess,
      String strDirectPrint, Vector<Object> vecTableParametersTop,
      FieldsData[] fieldsDataSelectAux, WADControl relationControl, String javaPackage,
      boolean deleteable, String tabmodule) throws ServletException, IOException {
    log4j.debug("Processing java: " + strTab + ", " + tabName);
    XmlDocument xmlDocument;
    final boolean isHighVolumen = (FieldsData.isHighVolume(pool, strTab).equals("Y"));
    boolean hasParentsFields = true;
    final String createFromProcess = FieldsData.hasCreateFromButton(pool, strTab);
    final boolean hasCreateFrom = !createFromProcess.equals("0");
    final String postedProcess = FieldsData.hasPostedButton(pool, strTab);
    final boolean hasPosted = !postedProcess.equals("0");

    final boolean noPInstance = (ActionButtonRelationData.select(pool, strTab).length == 0);
    final boolean noActionButton = FieldsData.hasActionButton(pool, strTab).equals("0");

    final String[] discard = { "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
        "", "", "", "", "hasReference", "", "", "", "", "", "", "", "hasOrgKey", "", "", "", "" };

    if (parentsFieldsData == null || parentsFieldsData.length == 0) {
      discard[0] = "parent"; // remove the parent tags
      hasParentsFields = false;
    } else if (!"Y".equals(parentsFieldsData[0].issecondarykey)) {
      discard[32] = "parentSecondaryKey";
    }

    if (tableName.toUpperCase().endsWith("_ACCESS")) {
      discard[18] = "client";
      discard[1] = "org";
    }

    if (!isHighVolumen || !tablevel.equals("0")) {
      discard[3] = "sectionIsHighVolume";
    }

    if (isHighVolumen)
      discard[10] = "sectionNotIsHighVolume";
    if (isSecondaryKey)
      discard[11] = "keySequence";
    else
      discard[24] = "withSecondaryKey";
    if (grandfatherField.equals(""))
      discard[12] = "grandfather";
    if (!hasCreateFrom)
      discard[13] = "sectionCreateFrom";
    if (!hasPosted)
      discard[19] = "sectionPosted";
    if (!(windowType.equalsIgnoreCase("T") && tablevel.equals("0")))
      discard[15] = "isTransactional";

    if (uiPattern.equals("STD"))
      discard[17] = "sectionReadOnly";
    if (!editReference.equals(""))
      discard[21] = "NothasReference";
    if ((noPInstance) && (noActionButton))
      discard[22] = "hasAdPInstance";
    if (noActionButton)
      discard[23] = "hasAdActionButton";

    if (FieldsData.hasButtonFixed(pool, strTab).equals("0"))
      discard[26] = "buttonFixed";
    if (strWindow.equals("110"))
      discard[27] = "sectionOrganizationCheck";
    discard[28] = "sameParent";
    if (!(parentsFieldsData == null || parentsFieldsData.length == 0)
        && (keyColumnName.equals(parentsFieldsData[0].name)))
      discard[28] = "";
    if (isSecondaryKey && !EditionFieldsData.isOrgKey(pool, strTab).equals("0")
        && !strTab.equals("170"))
      discard[29] = "";

    if (strWindow.equals("250"))
      discard[30] = "refreshTabParentSession"; // TODO: This fixes
    // [1879633] and shoudn't
    // be necessary in r2.5x
    // because of new PKs

    // Obtain action buttons processes to be called from tab trough buttons
    final ActionButtonRelationData[] actBtns = WadActionButton.buildActionButtonCall(pool, strTab,
        tabName, keyColumnName, isSOTrx, strWindow);
    final ActionButtonRelationData[] actBtnsJava = WadActionButton.buildActionButtonCallJava(pool,
        strTab, tabName, keyColumnName, isSOTrx, strWindow);

    if ((actBtns == null || actBtns.length == 0)
        && (actBtnsJava == null || actBtnsJava.length == 0)) {
      // No action buttons, service method is not neccessary
      discard[31] = "discardService";
    }

    if (parentsFieldsData.length > 0) {
      String parentTableName = WadData.tabTableName(pool, parentsFieldsData[0].adTabId);
      if (parentTableName != null && parentTableName.toUpperCase().endsWith("_ACCESS")) {
        discard[33] = "parentAccess";
      }
    }

    xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/wad/javasource", discard)
        .createXmlDocument();

    fileDir.mkdirs();
    xmlDocument.setParameter("class", tabName);
    xmlDocument.setParameter("package", (!javaPackage.equals("") ? javaPackage + "." : "")
        + windowName);
    xmlDocument.setParameter("path", (!javaPackage.equals("") ? javaPackage.replace(".", "/") + "/"
        : "") + windowName);
    xmlDocument.setParameter("key", keyColumnName);
    final Vector<Object> vecTotalParameters = new Vector<Object>();
    for (int i = 0; i < vecTableParametersTop.size(); i++) {
      vecTotalParameters.addElement(vecTableParametersTop.elementAt(i));
    }
    for (int i = 0; i < vecParametersTop.size(); i++) {
      vecTotalParameters.addElement(vecParametersTop.elementAt(i));
    }
    xmlDocument.setParameter("reportPDF", strProcess);
    xmlDocument.setParameter("reportDirectPrint", strDirectPrint);
    xmlDocument.setParameter("relationControl", relationControl.toJava());

    xmlDocument.setParameter("deleteable", deleteable ? "true" : "false");

    if (parentsFieldsData.length > 0) {

      xmlDocument.setParameter("keyParent", parentsFieldsData[0].name);
      xmlDocument.setParameter("keyParentColName",
          "Y".equals(parentsFieldsData[0].issecondarykey) ? "arentId" : parentsFieldsData[0].name);

      xmlDocument.setParameter("keyParentSimple", WadUtility.columnName(parentsFieldsData[0].name,
          parentsFieldsData[0].tablemodule, parentsFieldsData[0].columnmodule));
      xmlDocument.setParameter("keyParentT",
          Sqlc.TransformaNombreColumna(parentsFieldsData[0].name));

      xmlDocument.setParameter("parentTab", parentsFieldsData[0].adTabId);
      xmlDocument.setParameter("parentFieldID", parentsFieldsData[0].adFieldId);
    }
    xmlDocument.setParameter("keyData", Sqlc.TransformaNombreColumna(keyColumnName));
    xmlDocument.setParameter("table", tableName);
    xmlDocument.setParameter("windowId", strWindow);
    xmlDocument.setParameter("accessLevel", accesslevel);
    xmlDocument.setParameter("moduleId", tabmodule);
    xmlDocument.setParameter("tabId", strTab);
    xmlDocument.setParameter("tableId", tableId);
    xmlDocument.setParameter("createFromProcessId",
        ((Integer.valueOf(createFromProcess).intValue() > 0) ? createFromProcess : ""));
    xmlDocument.setParameter("postedProcessId",
        ((Integer.valueOf(postedProcess).intValue() > 0) ? postedProcess : ""));
    xmlDocument.setParameter("editReference", TabsData.formClassName(pool, editReference));
    // read only for relation toolbar: it is the same for Single Record and Read Only
    xmlDocument.setParameter("isReadOnly", uiPattern.equals("STD") ? "false" : "true");

    // UI Patter for edition toolbar
    xmlDocument.setParameter("uiPattern", uiPattern);

    if (WadUtility.findField(vecFields, "adClientId"))
      xmlDocument.setParameter("clientId", "data.adClientId");
    else
      xmlDocument.setParameter("clientId",
          "Utility.getContext(this, vars, \"#AD_Client_ID\", windowId)");

    if (WadUtility.findField(vecFields, "adOrgId"))
      xmlDocument.setParameter("orgId", "data.adOrgId");
    else
      xmlDocument.setParameter("orgId", "Utility.getContext(this, vars, \"#AD_Org_ID\", windowId)");

    // Parent field language
    if (parentsFieldsData != null && parentsFieldsData.length > 0) {
      final Vector<Object> vecCounters2 = new Vector<Object>();
      final Vector<Object> vecFields2 = new Vector<Object>();
      final Vector<Object> vecTable2 = new Vector<Object>();
      final Vector<Object> vecWhere2 = new Vector<Object>();
      final Vector<Object> vecParameters2 = new Vector<Object>();
      final Vector<Object> vecTableParameters2 = new Vector<Object>();
      vecCounters2.addElement("0");
      vecCounters2.addElement("0");
      WadUtility.columnIdentifier(pool, parentsFieldsData[0].tablename, true, parentsFieldsData[0],
          vecCounters2, true, vecFields2, vecTable2, vecWhere2, vecParameters2,
          vecTableParameters2, sqlDateFormat);

      xmlDocument.setParameter("parentLanguage", (vecParameters2.size() > 0 || vecTableParameters2
          .size() > 0) ? ", vars.getLanguage()" : "");
    }
    FieldsData[] fieldsData = null;
    boolean defaultValue;
    {
      final Vector<Object> vecFieldsSelect = new Vector<Object>();
      FieldsData[] fieldsData1 = null;
      fieldsData1 = copyarray(fieldsDataSelectAux);
      for (int i = 0; i < fieldsData1.length; i++) {
        if (!fieldsData1[i].name.equalsIgnoreCase("Created")
            && !fieldsData1[i].name.equalsIgnoreCase("CreatedBy")
            && !fieldsData1[i].name.equalsIgnoreCase("Updated")
            && !fieldsData1[i].name.equalsIgnoreCase("UpdatedBy")) {
          fieldsData1[i].name = Sqlc.TransformaNombreColumna(fieldsData1[i].name);
          fieldsData1[i].columnname = fieldsData1[i].name;
          defaultValue = false;

          WADControl control = WadUtility.getWadControlClass(pool, fieldsData1[i].reference,
              fieldsData1[i].adReferenceValueId);

          if (fieldsData1[i].reference.equals("20")) {
            fieldsData1[i].xmltext = ", \"N\"";
            defaultValue = true;
          } else {
            fieldsData1[i].xmltext = "";
          }

          if (fieldsData1[i].iskey.equals("Y")) {
            fieldsData1[i].xmltext = ", windowId + \"|" + fieldsData1[i].realname + "\"";
            fieldsData1[i].type = "RequestGlobalVariable";
          } else if (fieldsData1[i].issessionattr.equals("Y")) {
            if (control.isNumericType()) {
              fieldsData1[i].xmltext = ", vars.getSessionValue(windowId + \"|"
                  + fieldsData1[i].realname + "\")";
            } else {
              fieldsData1[i].xmltext = ", windowId + \"|" + fieldsData1[i].realname + "\"";
            }
            if (fieldsData1[i].reference.equals("20"))
              fieldsData1[i].xmltext += ", \"N\"";
            if (fieldsData1[i].required.equals("Y")
                && !fieldsData1[i].columnname.equalsIgnoreCase("Value") && !defaultValue) {
              if (fieldsData1[i].reference.equals("20"))
                fieldsData1[i].type = "RequiredInputGlobalVariable";
              else
                fieldsData1[i].type = "RequiredGlobalVariable";
            } else {
              if (fieldsData1[i].reference.equals("20"))
                fieldsData1[i].type = "RequiredInputGlobalVariable";
              else
                fieldsData1[i].type = "RequestGlobalVariable";
            }
          } else if (fieldsData1[i].required.equals("Y")
              && !fieldsData1[i].columnname.equalsIgnoreCase("Value") && !defaultValue) {
            fieldsData1[i].type = "RequiredStringParameter";
          }

          if (control.isNumericType()) {
            if (fieldsData1[i].required.equals("Y")) {
              fieldsData1[i].type = "RequiredNumericParameter";
            } else {
              fieldsData1[i].type = "NumericParameter";
            }
          }

          if (control.isNumericType()) {
            fieldsData1[i].trytext = " try { ";
            fieldsData1[i].catchtext = " } catch (ServletException paramEx) { ex = paramEx; } ";
          } else {
            fieldsData1[i].trytext = "";
            fieldsData1[i].catchtext = "";
          }

          vecFieldsSelect.addElement(fieldsData1[i]);
          if (control.has2UIFields() && fieldsData1[i].isdisplayed.equals("Y")) {
            FieldsData fieldsData2 = null;
            fieldsData2 = copyarrayElement(fieldsData1[i]);
            fieldsData2.name += "r";// (WadUtility.isSearchType(fieldsData1[i].reference)?"D":"r");
            fieldsData2.columnname += "_R";
            fieldsData2.type = "StringParameter";
            fieldsData2.xmltext = "";
            vecFieldsSelect.addElement(fieldsData2);
          }
        }
      }
      fieldsData = new FieldsData[vecFieldsSelect.size()];
      vecFieldsSelect.copyInto(fieldsData);
    }

    xmlDocument.setData("structure1", fieldsData);

    // process action buttons
    xmlDocument.setData("structure14", actBtns);
    xmlDocument.setData("structure15", actBtns);
    xmlDocument.setData("structure16", actBtns);
    xmlDocument.setData("structureActionBtnService", actBtns);

    // process standard UI java implemented buttons
    xmlDocument.setData("structure14java", actBtnsJava);
    xmlDocument.setData("structure15java", actBtnsJava);
    xmlDocument.setData("structure16java", actBtnsJava);
    xmlDocument.setData("structureActionBtnServiceJava", actBtnsJava);
    xmlDocument.setData("structureActionBtnServiceJavaSecuredProcess", actBtnsJava);

    final StringBuffer controlsJavaSource = new StringBuffer();
    boolean needsComboTableData = false;
    for (int i = 0; i < allfields.length; i++) {
      WADControl auxControl = null;
      try {
        auxControl = WadUtility.getControl(pool, allfields[i], uiPattern.equals("RO"), tabName, "",
            xmlEngine, false, false, false, hasParentsFields);
      } catch (final Exception ex) {
        throw new ServletException(ex);
      }
      if ((!auxControl.toJava().equals("")) && (!needsComboTableData)) {
        needsComboTableData = true;
        controlsJavaSource.append("    try {\n      ComboTableData comboTableData = null;\n");
      }
      controlsJavaSource.append(auxControl.toJava()).append(
          (auxControl.toJava().equals("") ? "" : "\n"));
    }

    xmlDocument.setData("structure38", FieldsData.explicitAccessProcess(pool, strTab));

    if (needsComboTableData)
      controlsJavaSource
          .append("    } catch (Exception ex) {\n      ex.printStackTrace();\n      throw new ServletException(ex);\n    }\n");
    xmlDocument.setParameter("controlsJavaCode", controlsJavaSource.toString());
    WadUtility.writeFile(fileDir, tabName + ".java", xmlDocument.print());
  }

  /**
   * Generates the xsql file for the tab
   * 
   * @param parentsFieldsData
   *          Array with the parent fields of the tab
   * @param fileDir
   *          Path where the file is gonna be created.
   * @param strTab
   *          Id of the tab.
   * @param tabName
   *          Name of the tab.
   * @param tableName
   *          Tab's table name.
   * @param windowName
   *          Window name.
   * @param keyColumnName
   *          Name of the key column.
   * @param strTables
   *          From clause for the tab.
   * @param vecParametersTop
   *          Vector of where clause parameters.
   * @param selCol
   *          Array with the selection columns.
   * @param tablevel
   *          Tab level.
   * @param windowType
   *          Type of window.
   * @param vecTableParametersTop
   *          Array of from clause parameters.
   * @param fieldsDataSelectAux
   *          Array with the tab's fields.
   * @throws ServletException
   * @throws IOException
   */
  private void processTabXSQL(FieldsData[] parentsFieldsData, File fileDir, String strTab,
      String tabName, String tableName, String windowName, String keyColumnName, String strTables,
      Vector<Object> vecParametersTop, EditionFieldsData[] selCol, String tablevel,
      String windowType, Vector<Object> vecTableParametersTop, FieldsData[] fieldsDataSelectAux,
      boolean isSecondaryKey, String javaPackage, Vector<String> vecFieldParameters)
      throws ServletException, IOException {
    log4j.debug("Procesig xsql: " + strTab + ", " + tabName);
    XmlDocument xmlDocumentXsql;
    final String[] discard = { "", "", "", "", "", "", "", "", "", "", "" };

    if (parentsFieldsData == null || parentsFieldsData.length == 0) {
      discard[0] = "parent"; // remove the parent tags
    } else if (!"Y".equals(parentsFieldsData[0].issecondarykey)) {
      discard[10] = "parentSecondaryKey";
    }

    if (tableName.toUpperCase().endsWith("_ACCESS")) {
      discard[6] = "client";
      discard[1] = "org";
    } // else if (tableName.toUpperCase().startsWith("M_PRODUCT") ||
    // tableName.toUpperCase().startsWith("C_BP") ||
    // tableName.toUpperCase().startsWith("AD_ORG")) discard[1] = "org";

    boolean isHighVolumen = (FieldsData.isHighVolume(pool, strTab).equals("Y"));
    if (!isHighVolumen || !tablevel.equals("0")) {
      discard[8] = "sectionIsHighVolume";
    }

    if (selCol == null || selCol.length == 0) {
      discard[2] = "sectionHighVolume";
      discard[3] = "sectionHighVolume1";
      discard[9] = "sectionIsHighVolume4";
    }
    if (!(windowType.equalsIgnoreCase("T") && tablevel.equals("0")))
      discard[4] = "sectionTransactional";
    if ((!(isSecondaryKey && !EditionFieldsData.isOrgKey(pool, strTab).equals("0")))
        || strTab.equals("170"))
      discard[7] = "hasOrgKey";
    else {
      discard[7] = "hasNoOrgKey";
    }

    xmlDocumentXsql = xmlEngine.readXmlTemplate("org/openbravo/wad/datasource", discard)
        .createXmlDocument();

    xmlDocumentXsql.ignoreTranslation = true;
    xmlDocumentXsql.setParameter("class", tabName + "Data");
    xmlDocumentXsql.setParameter("package", "org.openbravo.erpWindows."
        + (!javaPackage.equals("") ? javaPackage + "." : "") + windowName);
    xmlDocumentXsql.setParameter("table", tableName);
    xmlDocumentXsql.setParameter("key", tableName + "." + keyColumnName);
    if (parentsFieldsData != null && parentsFieldsData.length > 0) {

      xmlDocumentXsql.setParameter("keyParent", tableName + "." + parentsFieldsData[0].name);
    }
    xmlDocumentXsql.setParameter("paramKey", Sqlc.TransformaNombreColumna(keyColumnName));
    if (parentsFieldsData != null && parentsFieldsData.length > 0) {
      xmlDocumentXsql.setParameter("paramKeyParent",
          Sqlc.TransformaNombreColumna(parentsFieldsData[0].name));
      if (isSecondaryKey && (!EditionFieldsData.isOrgKey(pool, strTab).equals("0"))) {
        xmlDocumentXsql.setParameter("paramKeyParentOrg", "currentAdOrgId");
      }
      parentsFieldsData[0].name = WadUtility.columnName(parentsFieldsData[0].name,
          parentsFieldsData[0].tablemodule, parentsFieldsData[0].columnmodule);
    }

    // Relation select
    xmlDocumentXsql.setParameter("tables", strTables);

    final StringBuffer strParameters = new StringBuffer();
    final StringBuffer strParametersFields = new StringBuffer();

    for (String param : vecFieldParameters) {
      strParametersFields.append(param);
    }

    for (int i = 0; i < vecTableParametersTop.size(); i++) {
      strParameters.append(vecTableParametersTop.elementAt(i).toString()).append("\n");
    }
    for (int i = 0; i < vecParametersTop.size(); i++) {
      strParameters.append(vecParametersTop.elementAt(i).toString()).append("\n");
    }
    xmlDocumentXsql.setParameter("parameterFields", strParametersFields.toString());
    xmlDocumentXsql.setParameter("parameters", strParameters.toString());

    {
      // default values for search references in parameter windows for action buttons
      // keep it hardcoded by now
      final ProcessRelationData[] data = ProcessRelationData.selectXSQL(pool, strTab);
      if (data != null) {
        for (int i = 0; i < data.length; i++) {
          String tableN = "";
          if (data[i].adReferenceId.equals("28"))
            tableN = "C_ValidCombination";
          else if (data[i].adReferenceId.equals("31"))
            tableN = "M_Locator";
          else
            tableN = data[i].name.substring(0, data[i].searchname.length() - 3);
          String strName = "";
          if (data[i].adReferenceId.equals("28"))
            strName = "C_ValidCombination_ID";
          else if (data[i].adReferenceId.equals("31"))
            strName = "M_Locator_ID";
          else
            strName = data[i].searchname;
          final String strColumnName = FieldsData.columnIdentifier(pool, tableN);
          final StringBuffer fields = new StringBuffer();
          fields.append("SELECT " + strColumnName);
          fields.append(" FROM " + tableN);
          fields.append(" WHERE isActive='Y'");
          fields.append(" AND " + strName + " = ? ");
          data[i].whereclause = fields.toString();
          data[i].name = FormatUtilities.replace(data[i].name);
        }
      }
      xmlDocumentXsql.setData("structure12", data);
    }
    // SQLs of the defaultvalue of the parameter of the tab-associated
    // processes
    {
      final ProcessRelationData fieldsAux[] = ProcessRelationData.selectXSQLParams(pool, strTab);
      if (fieldsAux != null && fieldsAux.length > 0) {
        for (int i = 0; i < fieldsAux.length; i++) {
          final Vector<Object> vecParametros = new Vector<Object>();
          fieldsAux[i].reference = fieldsAux[i].adProcessId + "_"
              + FormatUtilities.replace(fieldsAux[i].columnname);
          fieldsAux[i].defaultvalue = WadUtility.getSQLWadContext(fieldsAux[i].defaultvalue,
              vecParametros);
          final StringBuffer parametros = new StringBuffer();
          for (final Enumeration<Object> e = vecParametros.elements(); e.hasMoreElements();) {
            final String paramsElement = WadUtility.getWhereParameter(e.nextElement(), true);
            parametros.append("\n" + paramsElement);
          }
          fieldsAux[i].whereclause = parametros.toString();
        }
      }
      xmlDocumentXsql.setData("structure16", fieldsAux);
    }

    {
      final ActionButtonRelationData[] abrd = WadActionButton.buildActionButtonSQL(pool, strTab);
      xmlDocumentXsql.setData("structure11", abrd);
    }

    xmlDocumentXsql.setData("structure13", selCol);

    WadUtility.writeFile(fileDir, tabName + "_data.xsql",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + xmlDocumentXsql.print());
  }

  /**
   * Generates combo reloads for all action buttons
   * 
   * @param fileDir
   *          Directory to save the generated java
   * @throws ServletException
   * @throws IOException
   */
  private void processProcessComboReloads(File fileDir) throws ServletException, IOException {
    log4j.info("Processig combo reloads for action buttons ");
    Vector<FieldsData> generatedProcesses = new Vector<FieldsData>();
    Vector<FieldsData[]> processCode = new Vector<FieldsData[]>();
    FieldsData[] processes = FieldsData.selectProcessesWithReloads(pool);

    for (FieldsData process : processes) {

      String processId = process.id;

      final FieldsData[] data = FieldsData.selectValidationProcess(pool, processId);
      if (data == null || data.length == 0)
        return;

      final boolean hasOrg = FieldsData.processHasOrgParam(pool, processId);

      final Vector<Object> vecReloads = new Vector<Object>();
      final Vector<Object> vecTotal = new Vector<Object>();
      final Vector<Object> vecCounters = new Vector<Object>();
      vecCounters.addElement("0");
      vecCounters.addElement("0");

      FieldsData[] result = null;

      for (int i = 0; i < data.length; i++) {

        final String code = data[i].whereclause
            + ((!data[i].whereclause.equals("") && !data[i].referencevalue.equals("")) ? " AND "
                : "") + data[i].referencevalue;
        data[i].columnname = "inp" + Sqlc.TransformaNombreColumna(data[i].columnname);
        data[i].whereclause = WadUtility.getComboReloadText(code, null, null, vecReloads, "inp");
        if (data[i].whereclause.equals("") && data[i].type.equals("R")) {
          // Add combo reloads for all combo references in case there is a ad_org parameter, if not
          // only for the params with validation rule
          if (!hasOrg) {
            continue;
          }
          data[i].whereclause = "\"inpadOrgId\"";
        }
        if (data[i].reference.equals("17") && data[i].whereclause.equals(""))
          data[i].whereclause = "\"inp" + data[i].columnname + "\"";
        if (!data[i].whereclause.equals("")
            && (data[i].reference.equals("17") || data[i].reference.equals("18") || data[i].reference
                .equals("19"))) {

          data[i].orgcode = "Utility.getReferenceableOrg(vars, vars.getStringParameter(\"inpadOrgId\"))";

          if (data[i].reference.equals("17")) { // List
            data[i].tablename = "List";
            data[i].tablenametrl = "List";
            data[i].htmltext = "select";
            data[i].htmltexttrl = "selectLanguage";
            data[i].xmltext = ", \"" + data[i].nameref + "\"";
            data[i].xmltexttrl = data[i].xmltext + ", vars.getLanguage()";
            data[i].xmltext += ", \"\"";
            data[i].xmltexttrl += ", \"\"";
          } else if (data[i].reference.equals("18")) { // Table
            final FieldsData[] tables = FieldsData.selectColumnTableProcess(pool, data[i].id);
            if (tables == null || tables.length == 0)
              throw new ServletException("No se ha encontrado la Table para la columnId: "
                  + data[i].id);
            final StringBuffer where = new StringBuffer();
            final Vector<Object> vecFields1 = new Vector<Object>();
            final Vector<Object> vecTables = new Vector<Object>();
            final Vector<Object> vecWhere = new Vector<Object>();
            final Vector<Object> vecParameters = new Vector<Object>();
            final Vector<Object> vecTableParameters = new Vector<Object>();

            WADControl control = WadUtility.getWadControlClass(pool, data[i].reference,
                data[i].referencevalue);
            control.columnIdentifier(tables[0].tablename, tables[0], vecCounters, vecFields1,
                vecTables, vecWhere, vecParameters, vecTableParameters);

            where.append(tables[0].whereclause);

            data[i].tablename = "TableList";
            data[i].htmltext = "select" + tables[0].referencevalue;
            if (!tables[0].columnname.equals("")) {
              data[i].htmltext += "_" + tables[0].columnname;
              data[i].tablename = "TableListVal";
              if (!where.toString().equals(""))
                where.append(" AND ");
              where.append(tables[0].defaultvalue);
            }
            data[i].tablenametrl = data[i].tablename + "Trl";
            data[i].htmltexttrl = data[i].htmltext;
            data[i].xmltext = "";
            if (vecTableParameters.size() > 0) {
              data[i].xmltext = ", vars.getLanguage()";
            }
            data[i].xmltext += ", Utility.getContext(this, vars, \"#User_Org\", windowId), Utility.getContext(this, vars, \"#User_Client\", windowId)";
            data[i].xmltext += WadUtility.getWadComboReloadContext(where.toString(), "N");
            data[i].xmltexttrl = data[i].xmltext;
            if (vecParameters.size() > 0 && vecTableParameters.size() == 0) {
              data[i].xmltext += ", vars.getLanguage()";
              data[i].xmltexttrl += ", vars.getLanguage()";
            }
            data[i].xmltext += ", \"\"";
            data[i].xmltexttrl += ", \"\"";
          } else if (data[i].reference.equals("19")) { // TableDir
            final FieldsData[] tableDir = FieldsData.selectColumnTableDirProcess(pool, data[i].id);
            if (tableDir == null || tableDir.length == 0)
              throw new ServletException("No se ha encontrado la TableDir para la columnId: "
                  + data[i].id);
            data[i].tablename = "TableDir";
            data[i].htmltext = "select" + tableDir[0].referencevalue;
            final String table_Name = tableDir[0].name.substring(0, tableDir[0].name.length() - 3);
            final Vector<Object> vecFields1 = new Vector<Object>();
            final Vector<Object> vecTables = new Vector<Object>();
            final Vector<Object> vecWhere = new Vector<Object>();
            final Vector<Object> vecParameters = new Vector<Object>();
            final Vector<Object> vecTableParameters = new Vector<Object>();

            WADControl control = WadUtility.getWadControlClass(pool, data[i].reference,
                data[i].referencevalue);
            control.columnIdentifier(table_Name, data[i], vecCounters, vecFields1, vecTables,
                vecWhere, vecParameters, vecTableParameters);

            data[i].xmltext = "";
            if (vecTableParameters.size() > 0) {
              data[i].xmltext = ", vars.getLanguage()";
            }
            data[i].xmltext += ", Utility.getContext(this, vars, \"#User_Org\", windowId), Utility.getContext(this, vars, \"#User_Client\", windowId)";
            if (!tableDir[0].columnname.equals("")) {
              data[i].htmltext += "_" + tableDir[0].columnname;
              data[i].tablename = "TableDirVal";
              data[i].xmltext += WadUtility.getWadComboReloadContext(tableDir[0].defaultvalue, "N");
            } else {
              data[i].tablename = "TableDir";
            }
            data[i].tablenametrl = data[i].tablename + "Trl";
            data[i].htmltexttrl = data[i].htmltext;
            data[i].xmltexttrl = data[i].xmltext;
            if (vecParameters.size() > 0 && vecTableParameters.size() == 0) {
              data[i].xmltext += ", vars.getLanguage()";
              data[i].xmltexttrl += ", vars.getLanguage()";
            }
            data[i].xmltext += ", \"\"";
            data[i].xmltexttrl += ", \"\"";
          }
          vecTotal.addElement(data[i]);
        }
      }
      if (vecTotal != null && vecTotal.size() > 0) {
        result = new FieldsData[vecTotal.size()];
        vecTotal.copyInto(result);
        processCode.add(result);
        generatedProcesses.add(process);
      }

    }
    if (generatedProcesses.size() > 0) {
      // create the helper class, it is a servlet that manages all combo reloads
      XmlDocument xmlDocumentHelper = xmlEngine.readXmlTemplate(
          "org/openbravo/wad/ComboReloadsProcessHelper").createXmlDocument();
      FieldsData[] processesGenerated = new FieldsData[generatedProcesses.size()];
      generatedProcesses.copyInto(processesGenerated);
      FieldsData[][] processData = new FieldsData[generatedProcesses.size()][];
      for (int i = 0; i < generatedProcesses.size(); i++) {
        processData[i] = processCode.get(i);
      }

      xmlDocumentHelper.setData("structure1", processesGenerated);
      xmlDocumentHelper.setData("structure2", processesGenerated);
      xmlDocumentHelper.setDataArray("reportComboReloadsProcess", "structure1", processData);
      WadUtility.writeFile(fileDir, "ComboReloadsProcessHelper.java", xmlDocumentHelper.print());
      log4j.debug("created :" + fileDir + "/ComboReloadsProcessHelper.java");
    }
  }

  /*
   * ##########################################################################
   * ################################################### # Utilities # ########
   * ##################################################################
   * #####################################################
   */
  /**
   * Returns the subtabs for a given parent tab id. Also marks as selected one of them.
   * 
   * @param vec
   *          Vector with the subtabs.
   * @param strTabParent
   *          Id of the parent tab.
   * @param strTabSelected
   *          Id of the selected tab.
   * @throws IOException
   * @throws ServletException
   */
  private void getSubTabs(Vector<Object> vec, String strTabParent, String strTabSelected)
      throws IOException, ServletException {
    TabsData[] aux = null;
    aux = TabsData.selectSubtabs(pool, strTabParent);
    if (aux == null || aux.length <= 0)
      return;
    for (int i = 0; i < aux.length; i++) {
      vec.addElement(aux[i]);
      getSubTabs(vec, aux[i].tabid, strTabSelected);
    }
  }

  /**
   * Returns the primary tabs of a given window.
   * 
   * @param strWindowId
   *          Id of the window.
   * @param strTabSelected
   *          The selected tab.
   * @param level
   *          The level of the tab to return.
   * @param heightTabs
   *          The default height for the tabs.
   * @param incrTabs
   *          The increment over the height.
   * @return Array with the primary tabs.
   * @throws IOException
   * @throws ServletException
   */
  private TabsData[] getPrimaryTabs(String strWindowId, String strTabSelected, int level,
      int heightTabs, int incrTabs) throws IOException, ServletException {
    TabsData[] aux = null;
    TabsData[] aux1 = null;
    int mayor = 0;
    final Vector<Object> vec = new Vector<Object>();
    aux1 = TabsData.selectTabParent(pool, strWindowId);
    if (aux1 == null || aux1.length == 0)
      return null;
    for (int i = 0; i < aux1.length; i++) {
      vec.addElement(aux1[i]);
      getSubTabs(vec, aux1[i].tabid, strTabSelected);
    }
    aux = new TabsData[vec.size()];
    vec.copyInto(aux);
    for (int i = 0; i < aux.length; i++)
      if (mayor < Integer.valueOf(aux[i].tablevel).intValue())
        mayor = Integer.valueOf(aux[i].tablevel).intValue();
    for (int i = 0; i < aux.length; i++)
      debugTab(aux[i], strTabSelected, level, heightTabs, incrTabs, mayor);
    return aux;
  }

  /**
   * Assigns the correct command to the given tab.
   * 
   * @param tab
   *          Tab to manipulate.
   * @param strTab
   *          The id of the actual tab.
   * @param level
   *          The level of the actual tab.
   * @param heightTabs
   *          The height of the tab.
   * @param incrTabs
   *          The increment for the height.
   * @param mayor
   *          operand to calculate the height.
   * @throws ServletException
   */
  private void debugTab(TabsData tab, String strTab, int level, int heightTabs, int incrTabs,
      int mayor) throws ServletException {
    final String tabName = FormatUtilities.replace(tab.tabname)
        + (tab.tabmodule.equals("0") ? "" : tab.tabid);
    if (strTab.equals(tab.tabid)) {
      tab.tdClass = "";
      tab.href = "return false;";
    } else {
      tab.tdClass = "";
      tab.href = "submitCommandForm('DEFAULT', false, null, '" + tabName
          + "_Relation.html', 'appFrame');return false;";
      if ((level + 1) >= Integer.valueOf(tab.tablevel).intValue())
        tab.href = "submitCommandForm('"
            + ((level > Integer.valueOf(tab.tablevel).intValue()) ? "DEFAULT" : "TAB") + "', "
            + ((level >= Integer.valueOf(tab.tablevel).intValue()) ? "false" : "true")
            + ", null, '" + tabName + "_Relation.html', 'appFrame');return false;";
      else
        tab.href = "return false;";
    }

    final int height = ((mayor - Integer.valueOf(tab.tablevel).intValue()) * incrTabs + heightTabs);
    tab.tdHeight = Integer.toString(height);
  }

  /**
   * Returns the index of the parent tab in the given array.
   * 
   * @param allTabs
   *          Array of tabs.
   * @param tabId
   *          The id of the actual tab.
   * @return Int with the index of the parent tab or -1 if there is no parent.
   * @throws ServletException
   * @throws IOException
   */
  private int parentTabId(TabsData[] allTabs, String tabId) throws ServletException, IOException {
    if (allTabs == null || allTabs.length == 0)
      return -1;
    else if (tabId == null || tabId.equals(""))
      return -1;
    else if (tabId.equals(allTabs[0].tabid))
      return -1;
    String parentTab = "";
    for (int i = 1; i < allTabs.length; i++) {
      if (allTabs[i].tabid.equals(tabId)) {
        parentTab = allTabs[i].parentKey;
        break;
      }
    }
    if (!parentTab.equals("-1")) {
      for (int i = 0; i < allTabs.length; i++) {
        if (allTabs[i].tabid.equals(parentTab))
          return i;
      }
    }
    return -1;
  }

  /**
   * Method to prepare the XmlEngine object, which is the one in charged of the templates.
   * 
   * @param fileConnection
   *          The path to the connection file.
   */
  private void createXmlEngine(String fileConnection) {
    // pass null as connection to running the translation at compile time
    xmlEngine = new XmlEngine(null);
    xmlEngine.isResource = true;
    xmlEngine.fileBaseLocation = new File(".");
    xmlEngine.strReplaceWhat = null;
    xmlEngine.strReplaceWith = null;
    XmlEngine.strTextDividedByZero = "TextDividedByZero";
    xmlEngine.fileXmlEngineFormat = new File(fileConnection, "Format.xml");
    log4j.debug("xmlEngine format file: " + xmlEngine.fileXmlEngineFormat.getAbsoluteFile());
    xmlEngine.initialize();
  }

  /**
   * Creates an instance of the connection's pool.
   * 
   * @param strFileConnection
   *          Path where is allocated the connection file.
   */
  private void createPool(String strFileConnection) {
    pool = new WadConnection(strFileConnection);
    WADControl.setConnection(pool);
  }

  /**
   * Auxiliar method to make a copy of a FieldsData element.
   * 
   * @param from
   *          The FieldsData object to copy.
   * @return The new copy of the given FieldsData object.
   */
  private FieldsData copyarrayElement(FieldsData from) {
    final FieldsData toAux = new FieldsData();
    toAux.realname = from.realname;
    toAux.name = from.name;
    toAux.nameref = from.nameref;
    toAux.xmltext = from.xmltext;
    toAux.reference = from.reference;
    toAux.referencevalue = from.referencevalue;
    toAux.required = from.required;
    toAux.isdisplayed = from.isdisplayed;
    toAux.isupdateable = from.isupdateable;
    toAux.defaultvalue = from.defaultvalue;
    toAux.fieldlength = from.fieldlength;
    toAux.textAlign = from.textAlign;
    toAux.xmlFormat = from.xmlFormat;
    toAux.displaylength = from.displaylength;
    toAux.columnname = from.columnname;
    toAux.whereclause = from.whereclause;
    toAux.tablename = from.tablename;
    toAux.type = from.type;
    toAux.issessionattr = from.issessionattr;
    toAux.iskey = from.iskey;
    toAux.isparent = from.isparent;
    toAux.accesslevel = from.accesslevel;
    toAux.isreadonly = from.isreadonly;
    toAux.issecondarykey = from.issecondarykey;
    toAux.showinrelation = from.showinrelation;
    toAux.isencrypted = from.isencrypted;
    toAux.sortno = from.sortno;
    toAux.istranslated = from.istranslated;
    toAux.id = from.id;
    toAux.htmltext = from.htmltext;
    toAux.htmltexttrl = from.htmltexttrl;
    toAux.xmltexttrl = from.xmltexttrl;
    toAux.tablenametrl = from.tablenametrl;
    toAux.nowrap = from.nowrap;
    toAux.iscolumnencrypted = from.iscolumnencrypted;
    toAux.isdesencryptable = from.isdesencryptable;
    toAux.adReferenceValueId = from.adReferenceValueId;
    return toAux;
  }

  /**
   * Auxiliar method to copy an array of FieldsData objects.
   * 
   * @param from
   *          The array of FieldsData objects to copy.
   * @return The copy array of FieldsData objects.
   */
  private FieldsData[] copyarray(FieldsData[] from) {
    if (from == null)
      return null;
    log4j.debug("Starting copyarray: " + from.length);
    final FieldsData[] to = new FieldsData[from.length];
    for (int i = 0; i < from.length; i++) {
      log4j.debug("For copyarray");
      to[i] = copyarrayElement(from[i]);
    }
    return to;
  }

  /**
   * Method to read the Openbravo.properties file.
   * 
   * @param strFileProperties
   *          The path of the property file to read.
   */
  private void readProperties(String strFileProperties) {
    // Read properties file.
    final Properties properties = new Properties();
    try {
      log4j.info("strFileProperties: " + strFileProperties);
      properties.load(new FileInputStream(strFileProperties));
      jsDateFormat = properties.getProperty("dateFormat.js");
      log4j.info("jsDateFormat: " + jsDateFormat);
      sqlDateFormat = properties.getProperty("dateFormat.sql");
      WADControl.setDateFormat(sqlDateFormat);
      log4j.info("sqlDateFormat: " + sqlDateFormat);
    } catch (final IOException e) {
      // catch possible io errors from readLine()
      e.printStackTrace();
    }
  }
}
