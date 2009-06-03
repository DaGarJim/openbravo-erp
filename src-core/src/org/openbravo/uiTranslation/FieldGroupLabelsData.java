//Sqlc generated V1.O00-1
package org.openbravo.uiTranslation;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.data.FieldProvider;
import org.openbravo.data.UtilSql;
import org.openbravo.database.ConnectionProvider;

class FieldGroupLabelsData implements FieldProvider {
  static Logger log4j = Logger.getLogger(FieldGroupLabelsData.class);
  private String InitRecordNumber = "0";
  public String tabname;
  public String fieldgroupid;
  public String fieldgroupname;
  public String fieldgrouptrlname;

  public String getInitRecordNumber() {
    return InitRecordNumber;
  }

  public String getField(String fieldName) {
    if (fieldName.equalsIgnoreCase("TABNAME"))
      return tabname;
    else if (fieldName.equalsIgnoreCase("FIELDGROUPID"))
      return fieldgroupid;
    else if (fieldName.equalsIgnoreCase("FIELDGROUPNAME"))
      return fieldgroupname;
    else if (fieldName.equalsIgnoreCase("FIELDGROUPTRLNAME"))
      return fieldgrouptrlname;
    else {
      log4j.debug("Field does not exist: " + fieldName);
      return null;
    }
  }

  public static FieldGroupLabelsData[] selectFieldGroupTrl(ConnectionProvider connectionProvider,
      String ad_tab_id, String language) throws ServletException {
    return selectFieldGroupTrl(connectionProvider, ad_tab_id, language, 0, 0);
  }

  public static FieldGroupLabelsData[] selectFieldGroupTrl(ConnectionProvider connectionProvider,
      String ad_tab_id, String language, String keyValue, String keyName, int numberRegisters)
      throws ServletException {
    boolean existsKey = false;
    String strSql = "";
    strSql = strSql
        + "			select  "
        + "			  tab.name as tabName"
        + "			  , fgroup.ad_fieldgroup_id as fieldGroupId "
        + "			  , fgroup.name as fieldGroupName"
        + "			  , fgroupTrl.name as fieldGroupTrlName"
        + "			from "
        + "			  ad_tab tab "
        + "			  , ad_field field "
        + "			  , ad_fieldgroup fgroup "
        + "			  , ad_fieldgroup_trl fgroupTrl "
        + "			where "
        + "			  tab.ad_tab_id = field.ad_tab_id "
        + "			  and field.ad_fieldgroup_id = fGroup.ad_fieldGroup_id "
        + "			  and tab.ad_tab_id = ?  "
        + "			  and ((fgroup.ad_fieldgroup_id = fgroupTrl.ad_fieldgroup_id and fgroupTrl.ad_language = ? ) "
        + "				or fgroupTrl.ad_fieldgroup_id is null) "
        + "			UNION "
        + "			select "
        + "			  tab.name as tabName"
        + "				, fgroup.ad_fieldgroup_id "
        + "				, fgroup.name "
        + "				, fgroupTrl.name "
        + "			from "
        + "			  ad_tab tab "
        + "			  , ad_fieldgroup fgroup "
        + "			  , ad_fieldgroup_trl fgroupTrl "
        + "			where "
        + "				fgroup.ad_fieldgroup_id = '1000100001' "
        + "			  	and tab.ad_tab_id = ?  "
        + "			  	and ((fgroup.ad_fieldgroup_id = fgroupTrl.ad_fieldgroup_id and fgroupTrl.ad_language = ? ) "
        + "					or fgroupTrl.ad_fieldgroup_id is null) "
        + "				and fgroup.ad_fieldgroup_id = fgroupTrl.ad_fieldgroup_id ";
    ResultSet result;
    Vector<java.lang.Object> vector = new Vector<java.lang.Object>(0);
    PreparedStatement st = null;

    int iParameter = 0;
    try {
      st = connectionProvider.getPreparedStatement(strSql);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, ad_tab_id);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, language);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, ad_tab_id);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, language);

      result = st.executeQuery();
      long countRecord = 0;
      long initRecord = 0;
      boolean searchComplete = false;
      while (result.next() && !searchComplete) {
        countRecord++;
        FieldGroupLabelsData objectFieldGroupLabelsData = new FieldGroupLabelsData();
        objectFieldGroupLabelsData.tabname = UtilSql.getValue(result, "TABNAME");
        objectFieldGroupLabelsData.fieldgroupid = UtilSql.getValue(result, "FIELDGROUPID");
        objectFieldGroupLabelsData.fieldgroupname = UtilSql.getValue(result, "FIELDGROUPNAME");
        objectFieldGroupLabelsData.fieldgrouptrlname = UtilSql
            .getValue(result, "FIELDGROUPTRLNAME");
        objectFieldGroupLabelsData.InitRecordNumber = Long.toString(initRecord);
        if (!existsKey)
          existsKey = (objectFieldGroupLabelsData.getField(keyName).equalsIgnoreCase(keyValue));
        vector.addElement(objectFieldGroupLabelsData);
        if (countRecord == numberRegisters) {
          if (existsKey)
            searchComplete = true;
          else {
            countRecord = 0;
            initRecord += numberRegisters;
            vector.clear();
          }
        }
      }
      result.close();
    } catch (SQLException e) {
      log4j.error("SQL error in query: " + strSql + "Exception:" + e);
      throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@"
          + e.getMessage());
    } catch (Exception ex) {
      log4j.error("Exception in query: " + strSql + "Exception:" + ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    if (existsKey) {
      FieldGroupLabelsData objectFieldGroupLabelsData[] = new FieldGroupLabelsData[vector.size()];
      vector.copyInto(objectFieldGroupLabelsData);
      return (objectFieldGroupLabelsData);
    }
    return (new FieldGroupLabelsData[0]);
  }

  public static FieldGroupLabelsData[] selectFieldGroupTrl(ConnectionProvider connectionProvider,
      String ad_tab_id, String language, int firstRegister, int numberRegisters)
      throws ServletException {
    String strSql = "";
    strSql = strSql
        + "			select  "
        + "			  tab.name as tabName"
        + "			  , fgroup.ad_fieldgroup_id as fieldGroupId "
        + "			  , fgroup.name as fieldGroupName "
        + "			  , fgroupTrl.name as fieldGroupTrlName  "
        + "			from "
        + "			  ad_tab tab "
        + "			  , ad_field field "
        + "			  , ad_fieldgroup fgroup "
        + "			  , ad_fieldgroup_trl fgroupTrl "
        + "			where "
        + "			  tab.ad_tab_id = field.ad_tab_id "
        + "			  and field.ad_fieldgroup_id = fGroup.ad_fieldGroup_id "
        + "			  and tab.ad_tab_id = ?  "
        + "			  and ((fgroup.ad_fieldgroup_id = fgroupTrl.ad_fieldgroup_id and fgroupTrl.ad_language = ? ) "
        + "				or fgroupTrl.ad_fieldgroup_id is null) "
        + "			UNION "
        + "			select "
        + "			  tab.name as tabName"
        + "				,fgroup.ad_fieldgroup_id "
        + "				, fgroup.name "
        + "				, fgroupTrl.name "
        + "			from "
        + "			  ad_tab tab "
        + "			  , ad_fieldgroup fgroup "
        + "			  , ad_fieldgroup_trl fgroupTrl "
        + "			where "
        + "				fgroup.ad_fieldgroup_id = '1000100001' "
        + "			  	and tab.ad_tab_id = ?  "
        + "			  	and ((fgroup.ad_fieldgroup_id = fgroupTrl.ad_fieldgroup_id and fgroupTrl.ad_language = ? ) "
        + "					or fgroupTrl.ad_fieldgroup_id is null) "
        + "				and fgroup.ad_fieldgroup_id = fgroupTrl.ad_fieldgroup_id ";

    ResultSet result;
    Vector<java.lang.Object> vector = new Vector<java.lang.Object>(0);
    PreparedStatement st = null;

    int iParameter = 0;
    try {
      st = connectionProvider.getPreparedStatement(strSql);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, ad_tab_id);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, language);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, ad_tab_id);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, language);

      result = st.executeQuery();
      long countRecord = 0;
      long countRecordSkip = 1;
      boolean continueResult = true;
      while (countRecordSkip < firstRegister && continueResult) {
        continueResult = result.next();
        countRecordSkip++;
      }
      while (continueResult && result.next()) {
        countRecord++;
        FieldGroupLabelsData objectFieldGroupLabelsData = new FieldGroupLabelsData();
        objectFieldGroupLabelsData.tabname = UtilSql.getValue(result, "TABNAME");
        objectFieldGroupLabelsData.fieldgroupid = UtilSql.getValue(result, "FIELDGROUPID");
        objectFieldGroupLabelsData.fieldgroupname = UtilSql.getValue(result, "FIELDGROUPNAME");
        objectFieldGroupLabelsData.fieldgrouptrlname = UtilSql
            .getValue(result, "FIELDGROUPTRLNAME");
        objectFieldGroupLabelsData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectFieldGroupLabelsData);
        if (countRecord >= numberRegisters && numberRegisters != 0) {
          continueResult = false;
        }
      }
      result.close();
    } catch (SQLException e) {
      log4j.error("SQL error in query: " + strSql + "Exception:" + e);
      throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@"
          + e.getMessage());
    } catch (Exception ex) {
      log4j.error("Exception in query: " + strSql + "Exception:" + ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    FieldGroupLabelsData objectFieldGroupLabelsData[] = new FieldGroupLabelsData[vector.size()];
    vector.copyInto(objectFieldGroupLabelsData);
    return (objectFieldGroupLabelsData);
  }

  public static FieldGroupLabelsData[] select(ConnectionProvider connectionProvider,
      String ad_tab_id) throws ServletException {
    return select(connectionProvider, ad_tab_id, 0, 0);
  }

  public static FieldGroupLabelsData[] select(ConnectionProvider connectionProvider,
      String ad_tab_id, String keyValue, String keyName, int numberRegisters)
      throws ServletException {
    boolean existsKey = false;
    String strSql = "";
    strSql = strSql + "      select " + "  			tab.name as tabName"
        + "  			, fgroup.ad_fieldgroup_id as fieldGroupId" + "  			, fgroup.name as fieldGroupName"
        + "			from" + "			  ad_tab tab" + "			  , ad_field field" + "			  , ad_fieldgroup fgroup"
        + "			where" + "			  tab.ad_tab_id = field.ad_tab_id"
        + "			  and field.ad_fieldgroup_id = fGroup.ad_fieldGroup_id"
        + "			  and tab.ad_tab_id = ? " + "		UNION " + "      select " + "  			tab.name as tabName"
        + "  			, fgroup.ad_fieldgroup_id as fieldGroupId" + "  			, fgroup.name as fieldGroupName"
        + "			from" + "			  ad_tab tab" + "			  , ad_fieldgroup fgroup" + "			where"
        + "			  fgroup.ad_fieldgroup_id = '1000100001' " + "			  and tab.ad_tab_id = ? ";

    ResultSet result;
    Vector<java.lang.Object> vector = new Vector<java.lang.Object>(0);
    PreparedStatement st = null;

    int iParameter = 0;
    try {
      st = connectionProvider.getPreparedStatement(strSql);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, ad_tab_id);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, ad_tab_id);

      result = st.executeQuery();
      long countRecord = 0;
      long initRecord = 0;
      boolean searchComplete = false;
      while (result.next() && !searchComplete) {
        countRecord++;
        FieldGroupLabelsData objectFieldGroupLabelsData = new FieldGroupLabelsData();
        objectFieldGroupLabelsData.tabname = UtilSql.getValue(result, "TABNAME");
        objectFieldGroupLabelsData.fieldgroupid = UtilSql.getValue(result, "FIELDGROUPID");
        objectFieldGroupLabelsData.fieldgroupname = UtilSql.getValue(result, "FIELDGROUPNAME");
        objectFieldGroupLabelsData.InitRecordNumber = Long.toString(initRecord);
        if (!existsKey)
          existsKey = (objectFieldGroupLabelsData.getField(keyName).equalsIgnoreCase(keyValue));
        vector.addElement(objectFieldGroupLabelsData);
        if (countRecord == numberRegisters) {
          if (existsKey)
            searchComplete = true;
          else {
            countRecord = 0;
            initRecord += numberRegisters;
            vector.clear();
          }
        }
      }
      result.close();
    } catch (SQLException e) {
      log4j.error("SQL error in query: " + strSql + "Exception:" + e);
      throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@"
          + e.getMessage());
    } catch (Exception ex) {
      log4j.error("Exception in query: " + strSql + "Exception:" + ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    if (existsKey) {
      FieldGroupLabelsData objectFieldGroupLabelsData[] = new FieldGroupLabelsData[vector.size()];
      vector.copyInto(objectFieldGroupLabelsData);
      return (objectFieldGroupLabelsData);
    }
    return (new FieldGroupLabelsData[0]);
  }

  public static FieldGroupLabelsData[] select(ConnectionProvider connectionProvider,
      String ad_tab_id, int firstRegister, int numberRegisters) throws ServletException {
    String strSql = "";
    strSql = strSql + "      select " + "  			tab.name as tabName"
        + "  			, fgroup.ad_fieldgroup_id as fieldGroupId" + "  			, fgroup.name as fieldGroupName"
        + "			from" + "			  ad_tab tab" + "			  , ad_field field" + "			  , ad_fieldgroup fgroup"
        + "			where" + "			  tab.ad_tab_id = field.ad_tab_id"
        + "			  and field.ad_fieldgroup_id = fGroup.ad_fieldGroup_id"
        + "			  and tab.ad_tab_id = ? " + "		UNION " + "      select " + "  			tab.name as tabName"
        + "  			, fgroup.ad_fieldgroup_id as fieldGroupId" + "  			, fgroup.name as fieldGroupName"
        + "			from" + "			  ad_tab tab" + "			  , ad_fieldgroup fgroup" + "			where"
        + "			  fgroup.ad_fieldgroup_id = '1000100001' " + "			  and tab.ad_tab_id = ? ";

    ResultSet result;
    Vector<java.lang.Object> vector = new Vector<java.lang.Object>(0);
    PreparedStatement st = null;

    int iParameter = 0;
    try {
      st = connectionProvider.getPreparedStatement(strSql);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, ad_tab_id);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, ad_tab_id);

      result = st.executeQuery();
      long countRecord = 0;
      long countRecordSkip = 1;
      boolean continueResult = true;
      while (countRecordSkip < firstRegister && continueResult) {
        continueResult = result.next();
        countRecordSkip++;
      }
      while (continueResult && result.next()) {
        countRecord++;
        FieldGroupLabelsData objectFieldGroupLabelsData = new FieldGroupLabelsData();
        objectFieldGroupLabelsData.tabname = UtilSql.getValue(result, "TABNAME");
        objectFieldGroupLabelsData.fieldgroupid = UtilSql.getValue(result, "FIELDGROUPID");
        objectFieldGroupLabelsData.fieldgroupname = UtilSql.getValue(result, "FIELDGROUPNAME");
        objectFieldGroupLabelsData.InitRecordNumber = Integer.toString(firstRegister);
        vector.addElement(objectFieldGroupLabelsData);
        if (countRecord >= numberRegisters && numberRegisters != 0) {
          continueResult = false;
        }
      }
      result.close();
    } catch (SQLException e) {
      log4j.error("SQL error in query: " + strSql + "Exception:" + e);
      throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@"
          + e.getMessage());
    } catch (Exception ex) {
      log4j.error("Exception in query: " + strSql + "Exception:" + ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    FieldGroupLabelsData objectFieldGroupLabelsData[] = new FieldGroupLabelsData[vector.size()];
    vector.copyInto(objectFieldGroupLabelsData);
    return (objectFieldGroupLabelsData);
  }
}
