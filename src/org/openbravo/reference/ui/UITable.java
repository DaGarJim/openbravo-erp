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
 * All portions are Copyright (C) 2009-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.reference.ui;

import static org.openbravo.erpCommon.utility.ComboTableData.CLIENT_LIST_PARAM_HOLDER;
import static org.openbravo.erpCommon.utility.ComboTableData.ORG_LIST_PARAM_HOLDER;

import java.util.Properties;

import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.ComboTableQueryData;
import org.openbravo.erpCommon.utility.TableSQLData;
import org.openbravo.reference.Reference;

public class UITable extends UIReference {
  public UITable(String reference, String subreference) {
    super(reference, subreference);
  }

  public void generateSQL(TableSQLData table, Properties prop) throws Exception {
    table.addSelectField(table.getTableName() + "." + prop.getProperty("ColumnName"),
        prop.getProperty("ColumnName"));
    identifier(table, table.getTableName(), prop, prop.getProperty("ColumnName") + "_R",
        table.getTableName() + "." + prop.getProperty("ColumnName"), false);
  }

  protected void identifier(TableSQLData tableSql, String parentTableName, Properties field,
      String identifierName, String realName, boolean tableRef) throws Exception {
    if (field == null)
      return;

    String fieldName = field.getProperty("ColumnName");

    int myIndex = tableSql.index++;
    ComboTableQueryData trd[] = ComboTableQueryData
        .selectRefTable(tableSql.getPool(), subReference);
    if (trd == null || trd.length == 0)
      return;
    String tables = "(SELECT ";
    if (trd[0].isvaluedisplayed.equals("Y")) {
      tableSql.addSelectField("td" + myIndex + ".VALUE", identifierName);
      tables += "value, ";
    }
    tables += trd[0].keyname + " AS tableID, " + trd[0].name + " FROM ";
    Properties fieldsAux = UIReferenceUtility.fieldToProperties(trd[0]);
    tables += trd[0].tablename + ") td" + myIndex;
    tables += " on " + parentTableName + "." + fieldName + " = td" + myIndex + ".tableID";
    tableSql.addFromField(tables, "td" + myIndex, realName);

    UIReference linkedReference = Reference.getUIReference(
        fieldsAux.getProperty("AD_Reference_ID"), fieldsAux.getProperty("AD_Reference_Value_ID"));
    linkedReference.identifier(tableSql, "td" + myIndex, fieldsAux, identifierName, realName, true);
  }

  public String getGridType() {
    return "dynamicEnum";
  }

  public void setComboTableDataIdentifier(ComboTableData comboTableData, String tableName,
      FieldProvider field) throws Exception {
    String fieldName = field == null ? "" : field.getField("name");
    String referenceValue = field == null ? "" : field.getField("referencevalue");

    int myIndex = comboTableData.index++;
    ComboTableQueryData trd[] = ComboTableQueryData.selectRefTable(
        comboTableData.getPool(),
        ((referenceValue != null && !referenceValue.equals("")) ? referenceValue : comboTableData
            .getObjectReference()));
    if (trd == null || trd.length == 0)
      return;
    comboTableData.addSelectField("td" + myIndex + "." + trd[0].keyname, "ID");
    if (trd[0].isvaluedisplayed.equals("Y"))
      comboTableData.addSelectField("td" + myIndex + ".VALUE", "NAME");
    ComboTableQueryData fieldsAux = new ComboTableQueryData();
    fieldsAux.name = trd[0].name;
    fieldsAux.tablename = trd[0].tablename;
    fieldsAux.reference = trd[0].reference;
    fieldsAux.referencevalue = trd[0].referencevalue;
    fieldsAux.required = trd[0].required;
    String tables = trd[0].tablename + " td" + myIndex;
    if (tableName != null && !tableName.equals("") && fieldName != null && !fieldName.equals("")) {
      tables += " on " + tableName + "." + fieldName + " = td" + myIndex + "." + trd[0].keyname
          + " \n";
      tables += "AND td" + myIndex + ".AD_Client_ID IN (" + CLIENT_LIST_PARAM_HOLDER + ") \n";
      if (!comboTableData.isAllowedCrossOrgReference()) {
        tables += "AND td" + myIndex + ".AD_Org_ID IN (" + ORG_LIST_PARAM_HOLDER + ")";
      }
    } else {
      comboTableData.addWhereField("td" + myIndex + ".AD_Client_ID IN (" + CLIENT_LIST_PARAM_HOLDER
          + ")", "CLIENT_LIST");
      if (!comboTableData.isAllowedCrossOrgReference()) {
        comboTableData.addWhereField("td" + myIndex + ".AD_Org_ID IN (" + ORG_LIST_PARAM_HOLDER
            + ")", "ORG_LIST");
      }
    }
    comboTableData.addFromField(tables, "td" + myIndex);
    String strSQL = trd[0].whereclause;
    if (strSQL == null)
      strSQL = "";

    if (!strSQL.equals("")) {
      if (strSQL.indexOf("@") != -1)
        strSQL = comboTableData.parseContext(strSQL, "WHERE");
      comboTableData.addWhereField(strSQL, "FILTER");
    }
    if (tableName == null || tableName.equals("")) {
      comboTableData.parseValidation();
      comboTableData.addWhereField("(td" + myIndex + ".isActive = 'Y' OR td" + myIndex + "."
          + trd[0].keyname + " = (?) )", "ISACTIVE");
      comboTableData.addWhereParameter("@ACTUAL_VALUE@", "ACTUAL_VALUE", "ISACTIVE");
    }
    String orderByAux = (trd[0].orderbyclause.equals("") ? "2" : trd[0].orderbyclause);
    if (orderByAux.indexOf("@") != -1)
      orderByAux = comboTableData.parseContext(orderByAux, "ORDERBY");
    comboTableData.identifier("td" + myIndex, fieldsAux);
    comboTableData.addOrderByField(orderByAux);
  }

  @Override
  public boolean canBeCached() {
    return true;
  }
}
