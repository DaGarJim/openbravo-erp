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

package org.openbravo.userinterface.selector.reference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.servlet.ServletException;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.BuscadorData;
import org.openbravo.erpCommon.utility.TableSQLData;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.reference.ui.UIReference;
import org.openbravo.reference.ui.UIReferenceUtility;
import org.openbravo.reference.ui.UITableDir;
import org.openbravo.userinterface.selector.Selector;
import org.openbravo.utils.FormatUtilities;

/**
 * Implements the User Interface part of the new customizable Reference. This part takes care of the
 * user interface in the grid and in the filter popup.
 * 
 * @author mtaal
 */
public class SelectorUIReference extends UIReference {

  public SelectorUIReference(String reference, String subreference) {
    super(reference, subreference);
  }

  /**
   * Generates the HTML code for the input used to display the reference in the filter popup
   */
  public void generateFilterHtml(StringBuffer strHtml, VariablesSecureApp vars, BuscadorData field,
      String strTab, String strWindow, ArrayList<String> vecScript, Vector<Object> vecKeys)
      throws IOException, ServletException {

    OBContext.setAdminMode();
    try {
      UIReferenceUtility.addUniqueElement(vecScript, strReplaceWith
          + "/../org.openbravo.client.kernel/OBCLKER_Kernel/StaticResources");
      strHtml.append("<td class=\"TextBox_ContentCell\">");
      final String inputName = FormatUtilities.replace(field.columnname);

      strHtml.append("<script>var sc_" + inputName + " = null;</script>");
      strHtml.append("<input type='hidden' name='inpParam" + inputName + "' id='" + inputName
          + "' value='" + field.value + "'");
      strHtml.append(" onreset='sc_" + inputName
          + ".resetSelector();' onchange='OB.Utilities.updateSmartClientComponentValue(this, sc_"
          + inputName + ".selectorField);' ");
      strHtml.append("></input>");
      strHtml.append("<script src='" + generateSelectorLink(field) + "'></script>");
      strHtml.append("</td>");
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private String generateSelectorLink(BuscadorData field) {
    final StringBuilder sb = new StringBuilder();
    sb.append("../org.openbravo.client.kernel/OBUISEL_Selector/" + getSelectorID(field));
    sb.append("?columnName=" + field.columnname);
    sb.append("&disabled=false");

    if ((Integer.valueOf(field.fieldlength).intValue() > UIReferenceUtility.MAX_TEXTBOX_LENGTH)) {
      sb.append("&CssSize=TwoCells");
    } else {
      sb.append("&CssSize=OneCell");
    }

    sb.append("&DisplayLength=" + field.displaylength);
    sb.append("&required=false");
    return sb.toString();
  }

  private String getSelectorID(BuscadorData field) {
    final String hqlWhere = "reference.id=:reference or reference.id=:referenceValue";
    final Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("reference", field.reference);
    parameters.put("referenceValue", field.referencevalue);

    final OBQuery<Selector> query = OBDal.getInstance().createQuery(Selector.class, hqlWhere);
    query.setNamedParameter("reference", field.reference);
    query.setNamedParameter("referenceValue", field.referencevalue);
    final List<Selector> selectors = query.list();
    if (selectors.isEmpty()) {
      throw new IllegalArgumentException("No Selectors defined for column " + field.adColumnId
          + " " + field.columnname);
    }
    return selectors.get(0).getId();
  }

  public void generateSQL(TableSQLData tableSql, Properties prop) throws Exception {
    OBContext.setAdminMode();
    try {
      Reference ref = OBDal.getInstance().get(Reference.class, subReference);
      if (!ref.getOBUISELSelectorList().isEmpty()) {
        final Selector selector = ref.getOBUISELSelectorList().get(0);
        final Table table;
        final Column fkColumn;
        if (selector.getTable() != null) {
          table = selector.getTable();
          if (selector.getColumn() != null && !selector.getColumn().isKeyColumn()) {
            fkColumn = selector.getColumn();
          } else {
            fkColumn = null;
          }
        } else if (selector.getObserdsDatasource() != null
            && selector.getObserdsDatasource().getTable() != null) {
          table = selector.getObserdsDatasource().getTable();
          fkColumn = null;
        } else {
          table = null;
          fkColumn = null;
        }
        if (table != null) {
          UITableDir tableDir = new UITableDir("19", null);
          if (fkColumn == null) {
            prop.setProperty("ColumnNameSearch", table.getDBTableName() + "_ID");
          } else {
            prop.setProperty("ColumnNameSearch", fkColumn.getDBColumnName());
            prop.setProperty("tableDirName", table.getDBTableName());
          }
          tableDir.identifier(tableSql, tableSql.getTableName(), prop, prop
              .getProperty("ColumnName"), tableSql.getTableName() + "."
              + prop.getProperty("ColumnName"), false);
        }
      } else {
        super.generateSQL(tableSql, prop);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
