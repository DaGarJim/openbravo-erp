/*
 ******************************************************************************
 * The contents of this file are subject to the   Compiere License  Version 1.1
 * ("License"); You may not use this file except in compliance with the License
 * You may obtain a copy of the License at http://www.compiere.org/license.html
 * Software distributed under the License is distributed on an  "AS IS"  basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * The Original Code is                  Compiere  ERP & CRM  Business Solution
 * The Initial Developer of the Original Code is Jorg Janke  and ComPiere, Inc.
 * Portions created by Jorg Janke are Copyright (C) 1999-2001 Jorg Janke, parts
 * created by ComPiere are Copyright (C) ComPiere, Inc.;   All Rights Reserved.
 * Contributor(s): Openbravo SL
 * Contributions are Copyright (C) 2001-2006 Openbravo S.L.
 ******************************************************************************
*/
package org.openbravo.erpCommon.ad_forms;

import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.exception.*;
import java.math.*;
import java.sql.*;
import javax.servlet.*;
import org.apache.log4j.Logger ;
// imports for transactions
import org.openbravo.database.ConnectionProvider;
import java.sql.Connection;



public class FactLine {
  static Logger log4jFactLine = Logger.getLogger(FactLine.class);

  public final BigDecimal ZERO = new BigDecimal("0");

  private AcctServer       m_docVO = null;
  private DocLine     m_docLine = null;
  private Account   m_acct = null;

  //  Account
  private String    m_C_AcctSchema_ID = "";
  private AcctSchema  m_acctSchema = null;

  private String  m_C_Currency_ID = "";
  private String  m_AmtSourceDr = "";
  private String  m_AmtSourceCr = "";
  //  Acct Amt
  private String  m_AmtAcctDr = "";
  private String  m_AmtAcctCr = "";
  //  Journal Info
  private String    m_GL_Budget_ID = "";
  private String    m_GL_Category_ID = "";
  private String    m_PostingType = "";
  //  Direstly set
  private String         m_M_Locator_ID = "";
  private String         m_C_LocFrom_ID = "";
  //  Balancing Segments
  private String         m_AD_Org_ID = "";
  //  Doc ID
  private String    m_AD_Table_ID;
  private String    m_Record_ID;
  private String         m_Line_ID;
  /**
   *  Public variables are set by Fact.createLine
   */
  private String    m_Fact_Acct_ID;
  public String     m_Fact_Acct_Group_ID;
  public String     m_SeqNo;
  public String     m_DocBaseType;


  /**
   *  Constructor
   *
   *  @param AD_Table_ID  - Table of Document Source
   *  @param Record_ID    - Record of document
   *  @param Line_ID      - Optional line id
   */
  public FactLine (String AD_Table_ID, String Record_ID, String Line_ID, String Fact_Acct_Group_ID, String SeqNo, String DocBaseType){
    m_AD_Table_ID = AD_Table_ID;
    m_Record_ID = Record_ID;
    m_Line_ID = Line_ID;
    m_Fact_Acct_Group_ID = Fact_Acct_Group_ID;
    m_SeqNo = SeqNo;
    m_DocBaseType = DocBaseType;
  }   //  FactLine

  /**
   *  Constructor
   *
   *  @param AD_Table_ID  - Table of Document Source
   *  @param Record_ID    - Record of document
   *  @param Line_ID      - Optional line id
   */
  public FactLine (String AD_Table_ID, String Record_ID, String Line_ID){
    m_AD_Table_ID = AD_Table_ID;
    m_Record_ID = Record_ID;
    m_Line_ID = Line_ID;
  }   //  FactLine

  /**
   *  Dispose
   */
  public void dispose(){
    m_docVO = null;
    m_docLine = null;
    m_acct = null;
  }   //  dispose

  /**
   *  Set Source Amounts
   *  @param C_Currency_ID currency
   *  @param AmtSourceDr source amount dr
   *  @param AmtSourceCr source amount cr
   *  @return true, if any if the amount is not zero
   */
  public boolean setAmtSource (String C_Currency_ID, String AmtSourceDr, String AmtSourceCr){
    m_C_Currency_ID = C_Currency_ID;
    m_AmtSourceDr = AmtSourceDr;
    if (m_AmtSourceDr.equals(""))
      m_AmtSourceDr = "0";
    m_AmtSourceCr = AmtSourceCr;
    if (m_AmtSourceCr.equals(""))
      m_AmtSourceCr = "0";
    //  one needs to be non zero
    if (m_AmtSourceDr.equals("0") && m_AmtSourceCr.equals("0"))
      return false;
    return true;
  }   //  setAmtSource

  /**
   *  Set Account Info
   *  @param acctSchema account schema
   *  @param acct account
   */
  public void setAccount (AcctSchema acctSchema, Account acct)  {
    m_acctSchema = acctSchema;
    m_C_AcctSchema_ID = acctSchema.getC_AcctSchema_ID();
    m_acct = acct;
  }   //  setAccount

  /**
   *  Set Accounted Amounts (alternative: call convert)
   *  @param AmtAcctDr acct amount dr
   *  @param AmtAcctCr acct amount cr
   */
  public void setAmtAcct(String AmtAcctDr, String AmtAcctCr){
    m_AmtAcctDr = AmtAcctDr;
    m_AmtAcctCr = AmtAcctCr;
  }   //  setAmtAcct

  /**
   *  Set Journal Info
   *  @param GL_Budget_ID budget
   *  @param GL_Category_ID category
   */
  public void setJournalInfo(String GL_Budget_ID, String GL_Category_ID){
    m_GL_Budget_ID = GL_Budget_ID;
    m_GL_Category_ID = GL_Category_ID;
  }   //  setJournalInfo

    /**
   *  Set Posting Type
   *  @param PostingType posting type
   */
  public void setPostingType(String PostingType){
    m_PostingType = PostingType;
  }   //  setPostingType

  /**
   *  Set Document Info
   *  @param docVO document value object
   *  @param docLine doc line
   */
  public void setDocumentInfo(AcctServer docVO, DocLine docLine){
    m_docVO = docVO;
    m_docLine = docLine;
  }   //  setDocumentInfo



  /**
   *  Convert to Accounted Currency
   *
   *  @param Acct_Currency_ID acct currency
   *  @param ConversionDate conversion date
   *  @param CurrencyRateType rate type
   *  @return true if converted
   */
  public boolean convert (String Acct_Currency_ID, String ConversionDate, String CurrencyRateType,ConnectionProvider conn){
    //  Document has no currency
    log4jFactLine.debug("convert - beginning");
    log4jFactLine.debug("convert - m_C_Currency_ID : " + m_C_Currency_ID);
    if (m_C_Currency_ID==null || m_C_Currency_ID.equals(AcctServer.NO_CURRENCY))
      m_C_Currency_ID = Acct_Currency_ID;
    log4jFactLine.debug("convert - Acct_Currency_ID : " + Acct_Currency_ID);
    if (Acct_Currency_ID.equals(m_C_Currency_ID)){
      m_AmtAcctDr = m_AmtSourceDr;
      m_AmtAcctCr = m_AmtSourceCr;
      return true;
    }
    if (m_docVO == null){
      log4jFactLine.warn("convert - No Document VO");
      return false;
    }
    m_AmtAcctDr = AcctServer.getConvertedAmt (m_AmtSourceDr, m_C_Currency_ID, Acct_Currency_ID,
      ConversionDate, CurrencyRateType, m_docVO.AD_Client_ID, m_docVO.AD_Org_ID,conn);
    if (m_AmtAcctDr == null || m_AmtAcctDr.equals(""))return false;
    m_AmtAcctCr = AcctServer.getConvertedAmt (m_AmtSourceCr, m_C_Currency_ID, Acct_Currency_ID,
      ConversionDate, CurrencyRateType, m_docVO.AD_Client_ID, m_docVO.AD_Org_ID,conn);
    return true;
  } //  convert

  /**
   *  Set Location from Organization
   *  @param AD_Org_ID org
   *  @param isFrom from
   */
  public void setLocationFromOrg (String AD_Org_ID, boolean isFrom,ConnectionProvider conn){
    if (AD_Org_ID.equals(""))return;
    String C_Location_ID = "";
    FactLineData [] data = null;
    try{
      data = FactLineData.select(conn,AD_Org_ID);
    }catch(ServletException e){
      log4jFactLine.warn(e);
    }
    if(data.length>0)C_Location_ID = data[0].location;
    if (!C_Location_ID.equals(""))
      setLocation (C_Location_ID, isFrom);
  }   //  setLocationFromOrg

  /**
   *  Set Location
   *  @param C_Location_ID location
   *  @param isFrom from
   */
  public void setLocation (String C_Location_ID, boolean isFrom){
    if (isFrom)
      m_C_LocFrom_ID = C_Location_ID;
  }   //  setLocator

  /**
   *  Get AD_Org_ID (balancing segment).
   *  (if not set directly - from document line, document, account, locator)
   *  <p>
   *  Note that Locator needs to be set before - otherwise
   *  segment balancing might produce the wrong results
   *  @return AD_Org_ID
   */
  public String getAD_Org_ID (ConnectionProvider conn){
    if (m_AD_Org_ID!=null && !m_AD_Org_ID.equals(""))      //  set earlier
      return m_AD_Org_ID;
    //  Prio 1 - get from locator - if exist
    if (m_M_Locator_ID!=null && !m_M_Locator_ID.equals("")){
      FactLineData [] data = null;
      try{
        data = FactLineData.selectOrg(conn,m_M_Locator_ID,m_docVO.AD_Client_ID);
      }catch(ServletException e){
        log4jFactLine.warn(e);
      }
      if (data!=null && data.length>0){
        m_AD_Org_ID = data[0].org;
        log4jFactLine.debug("setAD_Org_ID=" + m_AD_Org_ID + " (1 from M_Locator_ID=" + m_M_Locator_ID + ")");
      }else log4jFactLine.warn("getAD_Org_ID - Did not find M_Locator_ID=" + m_M_Locator_ID);
    }   //  M_Locator_ID != 0

    //  Prio 2 - get from doc line - if exists (document context overwrites)
    if ((m_AD_Org_ID==null || m_AD_Org_ID.equals("")||m_AD_Org_ID.equals("0")) && m_docLine != null){
      m_AD_Org_ID = m_docLine.m_AD_Org_ID;
      log4jFactLine.debug ("setAD_Org_ID=" + m_AD_Org_ID + " (2 from DocumentLine)");
    }
    //  Prio 3 - get from doc - if not GL
    if (m_AD_Org_ID==null || m_AD_Org_ID.equals("")||m_AD_Org_ID.equals("0")){
      if (AcctServer.DOCTYPE_GLJournal.equals (m_docVO.DocumentType)){
        m_AD_Org_ID = m_acct.getAD_Org_ID (); //  inter-company GL
        log4jFactLine.debug ("setAD_Org_ID=" + m_AD_Org_ID + " (3 from Acct)");
      }
      else{
        m_AD_Org_ID = m_docVO.AD_Org_ID;
        log4jFactLine.debug("setAD_Org_ID=" + m_AD_Org_ID + " (3 from Document)");
      }
    }
    //  Prio 4 - get from account - if not GL
    if (m_AD_Org_ID==null || m_AD_Org_ID.equals("")||m_AD_Org_ID.equals("0")){
      if (AcctServer.DOCTYPE_GLJournal.equals (m_docVO.DocumentType)){
        m_AD_Org_ID = m_docVO.AD_Org_ID;
        log4jFactLine.debug("setAD_Org_ID=" + m_AD_Org_ID + " (4 from Document)");
      }
      else{
        m_AD_Org_ID = m_acct.getAD_Org_ID ();
        log4jFactLine.debug ("setAD_Org_ID=" + m_AD_Org_ID + " (4 from Acct)");
      }
    }
    //
    return (m_AD_Org_ID==null)?"":m_AD_Org_ID;
  }   //  getAD_Org_ID

  /**
   *  Set AD_Org_ID   (balancing segment)
   *  @param AD_Org_ID org
   */
  public void setAD_Org_ID (String AD_Org_ID){
    if (!AD_Org_ID.equals(""))
      m_AD_Org_ID = AD_Org_ID;
  }   //  setAD_Org_ID

  /**
   *  Set Warehouse Locator.
   *  - will overwrite Organization -
   *  @param M_Locator_ID locator
   */
  public void setM_Locator_ID (String M_Locator_ID)
  {
    m_M_Locator_ID = M_Locator_ID;
    //  should not happen - consequence is potentially screwed Org segment balancing
    if (!m_AD_Org_ID.equals(""))
      log4jFactLine.warn("setM_Locator_ID - Organization already calculated");
  }   //  setM_Locator_ID


  /**
   *  Returns Source Balance of line
   *  @return source balance
   */
  public BigDecimal getSourceBalance(){
    if (m_AmtSourceDr.equals(""))
      m_AmtSourceDr = "0";
    if (m_AmtSourceCr.equals(""))
      m_AmtSourceCr = "0";
    BigDecimal AmtSourceDr = new BigDecimal(m_AmtSourceDr);
    BigDecimal AmtSourceCr = new BigDecimal(m_AmtSourceCr);
    //
    return AmtSourceDr.subtract(AmtSourceCr);
  }   //  getSourceBalance


  /**
   *  Set Location from Busoness Partner Location
   *  @param C_BPartner_Location_ID bp location
   *  @param isFrom from
   */
  public void setLocationFromBPartner (String C_BPartner_Location_ID, boolean isFrom,ConnectionProvider conn){
    if (C_BPartner_Location_ID.equals(""))
      return;
    String C_Location_ID = "";
    FactLineData[] data =null;
    try{
      data = FactLineData.selectLocation(conn,C_BPartner_Location_ID);
    }catch(ServletException e){
      log4jFactLine.warn(e);
    }
    if (data.length>0){
    C_Location_ID = data[0].location;
    }
    if (!C_Location_ID.equals(""))
      setLocation (C_Location_ID, isFrom);
  }   //  setLocationFromBPartner

  /**
   *  Set Location from Locator
   *  @param M_Locator_ID locator
   *  @param isFrom from
   */
  public void setLocationFromLocator (String M_Locator_ID, boolean isFrom, ConnectionProvider conn){
    if (M_Locator_ID.equals(""))
      return;
    String C_Location_ID = "";
    FactLineData [] data = null;
    try{
      data = FactLineData.selectLocationFromLocator(conn, M_Locator_ID);
    }catch(ServletException e){
      log4jFactLine.debug(e);
    }
    C_Location_ID = data[0].location;
    if (!C_Location_ID.equals(""))
      setLocation (C_Location_ID, isFrom);
  }   //  setLocationFromLocator


  /**
   *  Save to Disk.
   *  Get Info from this, doc-line, document, account
   *  Optionally create Revenue Recognition Plan
   *  @param con connection
   *  @return true if saved
   */
  public boolean save (Connection con,ConnectionProvider conn,VariablesSecureApp vars)throws ServletException{
    /**
     *  Fill variables
     */
    String AD_Client_ID = getAD_Client_ID();
    String AD_Org_ID = getAD_Org_ID(conn);

    //  Set Account
    String Account_ID = m_acct.Account_ID;
    if (Account_ID == null) Account_ID="";
    //  Doc Dates
    String  DateDoc = "";
    if (m_docLine != null)
      DateDoc = m_docLine.m_DateDoc;
    if (DateDoc == null || DateDoc.equals(""))
      DateDoc = m_docVO.DateDoc;
    String  DateAcct = "";
    if (m_docLine != null)
      DateAcct = m_docLine.m_DateAcct;
    if (DateAcct == null || DateAcct.equals(""))
      DateAcct = m_docVO.DateAcct;
    log4jFactLine.debug("FactLine - save - antes de Record_ID2 " + m_Record_ID);
    String  Record_ID2 = "";
    if (m_docLine != null)
      Record_ID2 = m_docLine.m_Record_Id2;
    log4jFactLine.debug("FactLine - save - despues de Record_ID2 = " + Record_ID2);
    String C_Period_ID = "";
    if (m_docLine != null)
      C_Period_ID = setC_Period_ID(m_docVO,m_docLine.m_DateAcct, conn);
    if (C_Period_ID == null || C_Period_ID.equals(""))
      C_Period_ID = m_docVO.C_Period_ID;

    //  Set Line Optional Info
    String C_UOM_ID = "";
    String Qty = m_docVO.Qty;
    String C_Tax_ID = "";
    if (m_docLine != null){
      C_UOM_ID = m_docLine.m_C_UOM_ID;
      Qty = m_docLine.m_qty;
      C_Tax_ID = m_docLine.m_C_Tax_ID;
    }
    log4jFactLine.debug("FactLine - save - despues de line optional info");
    //  Set Account Info
    String M_Product_ID = "";
    if (m_docLine != null)
      M_Product_ID = m_docLine.m_M_Product_ID;
    if (M_Product_ID==null) M_Product_ID="";
    if (M_Product_ID.equals(""))
      M_Product_ID = m_docVO.M_Product_ID;
    if (M_Product_ID==null) M_Product_ID="";
    if (M_Product_ID.equals(""))
      M_Product_ID = m_acct.M_Product_ID;
    if (M_Product_ID==null) M_Product_ID="";

    String C_LocFrom_ID = m_C_LocFrom_ID;
    if (C_LocFrom_ID==null) C_LocFrom_ID="";
    if (C_LocFrom_ID.equals("") && m_docLine != null)
      C_LocFrom_ID = m_docLine.m_C_LocFrom_ID;
    if (C_LocFrom_ID==null) C_LocFrom_ID="";
    if (C_LocFrom_ID.equals(""))
      C_LocFrom_ID = m_docVO.C_LocFrom_ID;
    if (C_LocFrom_ID==null) C_LocFrom_ID="";
    if (C_LocFrom_ID.equals(""))
      C_LocFrom_ID = m_acct.C_LocFrom_ID;
    if (C_LocFrom_ID==null) C_LocFrom_ID="";

    String C_LocTo_ID = m_C_LocFrom_ID;//Here Compiere was wrong, they had locFrom
    if (C_LocTo_ID==null) C_LocTo_ID="";
    if (C_LocTo_ID.equals("") && m_docLine != null)
      C_LocTo_ID = m_docLine.m_C_LocTo_ID;
    if (C_LocTo_ID==null) C_LocTo_ID="";
    if (C_LocTo_ID.equals(""))
      C_LocTo_ID = m_docVO.C_LocTo_ID;
    if (C_LocTo_ID==null) C_LocTo_ID="";
    if (C_LocTo_ID.equals(""))
      C_LocTo_ID = m_acct.C_LocTo_ID;
    if (C_LocTo_ID==null) C_LocTo_ID="";

    String C_BPartner_ID = "";
    if (m_docLine != null)
      C_BPartner_ID = m_docLine.m_C_BPartner_ID;
    if (C_BPartner_ID==null) C_BPartner_ID="";
    if (C_BPartner_ID.equals(""))
      C_BPartner_ID = m_docVO.C_BPartner_ID;
    if (C_BPartner_ID==null) C_BPartner_ID="";
    if (C_BPartner_ID.equals(""))
      C_BPartner_ID = m_acct.C_BPartner_ID;
    if (C_BPartner_ID==null) C_BPartner_ID="";

    String AD_OrgTrx_ID = "";
    if (m_docLine != null)
      AD_OrgTrx_ID = m_docLine.m_AD_OrgTrx_ID;
    if (AD_OrgTrx_ID==null) AD_OrgTrx_ID="";
    if (AD_OrgTrx_ID.equals(""))
      AD_OrgTrx_ID = m_docVO.AD_OrgTrx_ID;
    if (AD_OrgTrx_ID==null) AD_OrgTrx_ID="";
    if (AD_OrgTrx_ID.equals(""))
      AD_OrgTrx_ID = m_acct.AD_OrgTrx_ID;
    if (AD_OrgTrx_ID==null) AD_OrgTrx_ID="";

    String C_SalesRegion_ID = getC_SalesRegion_ID(conn);

    String C_Project_ID = "";
    if (m_docLine != null)
      C_Project_ID = m_docLine.m_C_Project_ID;
    if (C_Project_ID==null) C_Project_ID="";
    if (C_Project_ID.equals(""))
      C_Project_ID = m_docVO.C_Project_ID;
    if (C_Project_ID==null) C_Project_ID="";
    if (C_Project_ID.equals(""))
      C_Project_ID = m_acct.C_Project_ID;
    if (C_Project_ID==null) C_Project_ID="";

    String C_Campaign_ID = "";
    if (m_docLine != null)
      C_Campaign_ID = m_docLine.m_C_Campaign_ID;
    if (C_Campaign_ID==null) C_Campaign_ID="";
    if (C_Campaign_ID.equals(""))
      C_Campaign_ID = m_docVO.C_Campaign_ID;
    if (C_Campaign_ID==null) C_Campaign_ID="";
    if (C_Campaign_ID.equals(""))
      C_Campaign_ID = m_acct.C_Campaign_ID;
    if (C_Campaign_ID==null) C_Campaign_ID="";

    String C_Activity_ID = "";
    if (m_docLine != null)
      C_Activity_ID = m_docLine.m_C_Activity_ID;
    if (C_Activity_ID==null) C_Activity_ID="";
    if (C_Activity_ID.equals(""))
      C_Activity_ID = m_docVO.C_Activity_ID;
    if (C_Activity_ID==null) C_Activity_ID="";
    if (C_Activity_ID.equals(""))
      C_Activity_ID = m_acct.C_Activity_ID;
    if (C_Activity_ID==null) C_Activity_ID="";

    String User1_ID = "";
    if (m_docLine != null)
      User1_ID = m_docLine.m_User1_ID;
    if (User1_ID==null) User1_ID="";
    if (User1_ID.equals(""))
      User1_ID = m_docVO.User1_ID;
    if (User1_ID==null) User1_ID="";
    if (User1_ID.equals(""))
      User1_ID = m_acct.User1_ID;
    if (User1_ID==null) User1_ID="";

    String User2_ID = "";
    if (m_docLine != null)
      User2_ID = m_docLine.m_User2_ID;
    if (User2_ID==null) User2_ID="";
    if (User2_ID.equals(""))
      User2_ID = m_docVO.User2_ID;
    if (User2_ID==null) User2_ID="";
    if (User2_ID.equals(""))
      User2_ID = m_acct.User2_ID;
    if (User2_ID==null) User2_ID="";

    log4jFactLine.debug("FactLine - save - antes de Revenue Recognition for AR Invoices");

    //  Revenue Recognition for AR Invoices
    if (m_docVO.DocumentType.equals(AcctServer.DOCTYPE_ARInvoice) &&
      m_docLine != null && m_docLine.p_productInfo!=null && m_docLine.getC_RevenueRecognition_ID() != null && !m_docLine.getC_RevenueRecognition_ID().equals("")){
      Account_ID = createRevenueRecognition(con,conn,m_docLine.getC_RevenueRecognition_ID(), m_docLine.m_TrxLine_ID,AD_Client_ID, AD_Org_ID, "0", Account_ID,
        M_Product_ID, C_BPartner_ID, AD_OrgTrx_ID,C_LocFrom_ID, C_LocTo_ID, C_SalesRegion_ID, C_Project_ID,C_Campaign_ID, C_Activity_ID, User1_ID, User2_ID, vars);
    }
    log4jFactLine.debug("FactLine - save - despues de Revenue Recognition for AR Invoices");
    //  Description
    StringBuffer description = new StringBuffer();
    description = getDescription(conn , C_BPartner_ID,m_C_AcctSchema_ID,m_AD_Table_ID, m_Record_ID, (m_docLine!=null?m_docLine.m_TrxLine_ID:null));
    int no =0;
    try{
      /**
       *  Create SQL Statement
       */
      m_Fact_Acct_ID = SequenceIdData.getSequence(conn, "Fact_Acct", vars.getClient());
      /**
       *  Save to DB
       */
      log4jFactLine.debug("FactLine - save - m_Record_ID = " + m_Record_ID + " - Account_ID = " + Account_ID + " - m_Fact_Acct_Group_ID = " + m_Fact_Acct_Group_ID + " - m_SeqNo = " + m_SeqNo);
      FactLineData [] cuenta = FactLineData.selectAccountValue(conn, Account_ID);
      BigDecimal zero = new BigDecimal("0.0");
      if(zero.compareTo(new BigDecimal(m_AmtSourceDr))==0 && zero.compareTo(new BigDecimal(m_AmtSourceCr))==0 && zero.compareTo(new BigDecimal(m_AmtAcctDr))==0 && zero.compareTo(new BigDecimal(m_AmtAcctCr))==0) return true; 
      else no = FactLineData.insertFactAct(con,conn,m_Fact_Acct_ID,AD_Client_ID,AD_Org_ID,m_C_AcctSchema_ID,Account_ID, cuenta[0].value, 
        cuenta[0].description,DateDoc,DateAcct,C_Period_ID,m_AD_Table_ID,m_Record_ID,m_Line_ID,m_GL_Category_ID,m_GL_Budget_ID,C_Tax_ID,
        m_PostingType,m_C_Currency_ID,m_AmtSourceDr,m_AmtSourceCr,m_AmtAcctDr,m_AmtAcctCr,C_UOM_ID,Qty,m_M_Locator_ID,
        M_Product_ID,C_BPartner_ID,AD_OrgTrx_ID,C_LocFrom_ID,C_LocTo_ID,C_SalesRegion_ID,C_Project_ID,C_Campaign_ID,
        C_Activity_ID,User1_ID,User2_ID,description.toString(), m_Fact_Acct_Group_ID, m_SeqNo, m_DocBaseType, Record_ID2, m_docLine.m_A_Asset_ID);
      if(m_docVO.m_IsOpening.equals("Y")) FactLineData.updateFactAcct(con,conn,m_AD_Table_ID,m_Record_ID);
    }catch(ServletException e){
      log4jFactLine.warn(e);
    }
    return no==1;
  }   //  save

  /**
   *  Get AD_Client
   *  @return AD_Client_ID
   */
  private String getAD_Client_ID(){
    String AD_Client_ID = m_docVO.AD_Client_ID;
    if (AD_Client_ID==null || AD_Client_ID.equals(""))
      AD_Client_ID = m_acct.AD_Client_ID;
    return (AD_Client_ID==null)?"":AD_Client_ID;
  }   //  getAD_Client_ID

  /**
   *  Get/derive Sales Region
   *  @return Sales Region
   */
  private String getC_SalesRegion_ID (ConnectionProvider conn){
    String C_SalesRegion_ID = "";
    if (m_docLine != null)
      C_SalesRegion_ID = m_docLine.m_C_SalesRegion_ID;
    if (C_SalesRegion_ID==null) C_SalesRegion_ID="";
    if (C_SalesRegion_ID.equals(""))
      C_SalesRegion_ID = m_docVO.C_SalesRegion_ID;
    if (C_SalesRegion_ID==null) C_SalesRegion_ID="";
    if (C_SalesRegion_ID.equals("") && !m_docVO.BP_C_SalesRegion_ID.equals(""))
      C_SalesRegion_ID = m_docVO.BP_C_SalesRegion_ID;
    if (C_SalesRegion_ID==null) C_SalesRegion_ID="";
    //  derive SalesRegion if AcctSegment
    if (C_SalesRegion_ID.equals("") && !m_docVO.C_BPartner_Location_ID.equals("") && m_docVO.BP_C_SalesRegion_ID.equals("")// never tried
      && m_acctSchema.isAcctSchemaElement(AcctSchemaElement.SEGMENT_SalesRegion)){
      FactLineData [] data = null;
      try{
        data = FactLineData.selectSalesRegion(conn,m_docVO.C_BPartner_Location_ID);
      }catch(ServletException e){
        log4jFactLine.warn(e);
      }
      if (data.length>0){
      C_SalesRegion_ID = data[0].salesregion;
      }
      if (C_SalesRegion_ID!=null && !C_SalesRegion_ID.equals(""))m_docVO.BP_C_SalesRegion_ID = C_SalesRegion_ID;//  save
      else m_docVO.BP_C_SalesRegion_ID = "";  //  don't try again
      log4jFactLine.debug("getC_SalesRegion_ID=" + C_SalesRegion_ID + " (from BPL)" );
    }
    if (C_SalesRegion_ID==null || C_SalesRegion_ID.equals(""))
      C_SalesRegion_ID = m_acct.C_SalesRegion_ID;
    //
    return (C_SalesRegion_ID==null)?"":C_SalesRegion_ID;
  } //  getC_SalesRegion_ID

  /**
   *  Revenue Recognition.
   *  Called from FactLine.save
   *  <p>
   *  Create Revenue recognition plan and return Unearned Revenue account
   *  to be used instead of Revenue Account. If not found, it returns
   *  the revenue account.
   *
   *  @param con connection
   *  @param C_RevenueRecognition_ID revenue recognition
   *  @param C_InvoiceLine_ID invoice line
   *  @param AD_Client_ID client
   *  @param AD_Org_ID org
   *  @param AD_User_ID user
   *  @param Account_ID of Revenue Account
   *  @param M_Product_ID product
   *  @param C_BPartner_ID bpartner
   *  @param AD_OrgTrx_ID trx org
   *  @param C_LocFrom_ID loc from
   *  @param C_LocTo_ID loc to
   *  @param C_SRegion_ID sales region
   *  @param C_Project_ID project
   *  @param C_Campaign_ID campaign
   *  @param C_Activity_ID activity
   *  @param User1_ID user1
   *  @param User2_ID user2
   *  @return Account_ID for Unearned Revenue or Revenue Account if not found
   */
  private String createRevenueRecognition (Connection con,ConnectionProvider conn,
    String C_RevenueRecognition_ID, String C_InvoiceLine_ID,
    String AD_Client_ID, String AD_Org_ID, String AD_User_ID, String Account_ID,
    String M_Product_ID, String C_BPartner_ID, String AD_OrgTrx_ID,
    String C_LocFrom_ID, String C_LocTo_ID, String C_SRegion_ID, String C_Project_ID,
    String C_Campaign_ID, String C_Activity_ID, String User1_ID, String User2_ID,VariablesSecureApp vars){
    //  get VC for P_Revenue (from Product)
    log4jFactLine.debug("FactLine - createRevenueRecognition START");
    String P_Revenue_Acct = AcctServer.getValidCombination(AD_Client_ID, AD_Org_ID,
      m_C_AcctSchema_ID, Account_ID,"0", "", AD_User_ID,
      M_Product_ID, C_BPartner_ID, AD_OrgTrx_ID,
      C_LocFrom_ID, C_LocTo_ID, C_SRegion_ID, C_Project_ID,
      C_Campaign_ID, C_Activity_ID, User1_ID, User2_ID,conn);
    log4jFactLine.debug("FactLine - createRevenueRecognition end");
    if (P_Revenue_Acct.equals("")){
      log4jFactLine.warn("FactLine - createRevenueRecognition - Revenue_Acct not found");
      return Account_ID;
    }

    //  get Unearned Revenue Acct from BPartner Group
    String UnearnedRevenue_Acct = "";
    String new_Account_ID = "";
    FactLineData[] data = null;
    try{
      data = FactLineData.selectUnearnedRevenue(conn, m_C_AcctSchema_ID, C_BPartner_ID);

      if (data.length>0){
        UnearnedRevenue_Acct = data[0].unearnedrevenue;
        new_Account_ID = data[0].account;
      }
      if (new_Account_ID.equals("")){
        log4jFactLine.warn ("createRevenueRecognition - UnearnedRevenue_Acct not found");
        return Account_ID;
      }

      //  Insert record in C_RevenueRecognition_Plan to start batch process generation
      String C_RevenueRecognition_Plan_ID = SequenceIdData.getSequence(conn, "C_RevenueRecognition_Plan", vars.getClient());
      int no = FactLineData.insertRevenueRecognitionPlan(con,conn, C_RevenueRecognition_Plan_ID, C_RevenueRecognition_ID, 
      m_C_AcctSchema_ID, AD_Client_ID, AD_Org_ID, AD_User_ID, C_InvoiceLine_ID, UnearnedRevenue_Acct, P_Revenue_Acct, m_C_Currency_ID,
      getAcctBalance());
      if (no != 1){
        log4jFactLine.warn ("createRevenueRecognition - Plan NOT created");
        return Account_ID;
      }
    }catch(ServletException e){
      log4jFactLine.warn(e);
    }
    log4jFactLine.debug ("createRevenueRecognition From Acctount_ID=" + Account_ID + " to " + new_Account_ID
      + " - Plan from UnearnedRevenue_Acct=" + UnearnedRevenue_Acct + " to Revenue_Acct=" + P_Revenue_Acct);
    return new_Account_ID;
  }   //  createRevenueRecognition

  /**
   *  Get Accounted Balance
   *  @return accounting balance
   */
  public String getAcctBalance(){
    if (m_AmtAcctDr.equals(""))
      m_AmtAcctDr = "0";
    if (m_AmtAcctCr.equals(""))
      m_AmtAcctCr = "0";
    BigDecimal AmtAcctDr = new BigDecimal(m_AmtAcctDr);
    BigDecimal AmtAcctCr = new BigDecimal(m_AmtAcctCr);
    return AmtAcctDr.subtract(AmtAcctCr).toString();
  }   //  getAcctBalance

  /**
   *  Is Account on Balance Sheet
   *  @return true if account is a balance sheet account
   */
  public boolean isBalanceSheet(){
    return m_acct.isBalanceSheet();
  } //  isBalanceSheet

  /**
   *  Currect Accounting Amount.
   *  <pre>
   *  Example:    1       -1      1       -1
   *  Old         100/0   100/0   0/100   0/100
   *  New         101/0   99/0    0/99    0/101
   *  </pre>
   *  @param deltaAmount delta amount
   */
  public void currencyCorrect (BigDecimal deltaAmount){
    boolean negative = deltaAmount.compareTo(ZERO) < 0;
    BigDecimal AmtAcctDr = new BigDecimal(m_AmtAcctDr);
    BigDecimal AmtAcctCr = new BigDecimal(m_AmtAcctCr);
    boolean adjustDr = AmtAcctDr.compareTo(AmtAcctCr) > 0;

    log4jFactLine.debug ("currencyCorrect - " + deltaAmount.toString()
      + "; Old-AcctDr=" + m_AmtAcctDr + ",AcctCr=" + m_AmtAcctCr
      + "; Negative=" + negative + "; AdjustDr=" + adjustDr);
    if (adjustDr)
      if (negative) m_AmtAcctDr = AmtAcctDr.subtract(deltaAmount).toString();
      else  m_AmtAcctDr = AmtAcctDr.add(deltaAmount).toString();
    else
      if (negative) m_AmtAcctCr = AmtAcctCr.add(deltaAmount).toString();
      else  m_AmtAcctCr = AmtAcctCr.subtract(deltaAmount).toString();
    log4jFactLine.debug("currencyCorrect - New-AcctDr=" + m_AmtAcctDr + ",AcctCr=" + m_AmtAcctCr);
  } //  currencyCorrect

    public String setC_Period_ID(AcctServer m_docVO,String strDateAcct, ConnectionProvider conn){
        AcctServerData [] data=null;
        try{
          data = AcctServerData.periodOpen(conn,m_docVO.AD_Client_ID,m_docVO.DocumentType,strDateAcct);
        }catch (ServletException e){
            log4jFactLine.warn(e);
        }
        return data[0].period;
    }   //  setC_Period_ID

    public StringBuffer getDescription(ConnectionProvider connectionProvider,String strC_Bpartner_ID,String strC_AcctSchema_ID, String strAD_Table_ID, String strRecord_ID, String strLine)throws ServletException{
      StringBuffer description = new StringBuffer();
      String strSql = AcctServerData.selectDescription(connectionProvider, strAD_Table_ID, strC_AcctSchema_ID);
      try {
        if (!strSql.equals("")/* && strLine!=null && !strLine.equals("")*/){
          strSql = strSql.replaceAll("@RecordId@",strRecord_ID);
          if(strLine==null || strLine.equals("")) strLine = "NULL";
          strSql = strSql.replaceAll("@Line@",strLine);
          Statement st = connectionProvider.getStatement();
          ResultSet result;
          try{
            if (st.execute(strSql)) {
                result = st.getResultSet();
                while(result.next()) {
                  description.append(result.getString(1));
                }
                result.close();
            }
          }catch (SQLException e){
            log4jFactLine.error("SQL error in query: " + strSql + "Exception:"+ e);
            throw new ServletException(Integer.toString(e.getErrorCode()));
          } finally {
            try {
              connectionProvider.releaseStatement(st);
            } catch (Exception ignored) {}
          }
        }
        if(description.length()==0){
          description.append(m_docVO.DocumentNo);
          if (!strC_Bpartner_ID.equals("")) description.append(" # ").append(AcctServerData.selectBpartnerName(connectionProvider,strC_Bpartner_ID));
          //  ... line
          if (m_docLine != null){
            if (m_docLine.m_Line!=null && !m_docLine.m_Line.equals(""))
              description.append(" # ").append(m_docLine.m_Line);
            if (m_docLine.m_description!=null && !m_docLine.m_description.equals(""))
              description.append(" (").append(m_docLine.m_description).append(")");
          }
          //  ... cannot distinguish between header and tax
        }
        if (description.length() > 255) description = new StringBuffer(description.substring(0,254));
      } catch (NoConnectionAvailableException ex) {
        throw new ServletException("@CODE=NoConnectionAvailable");
      } catch (SQLException ex2) {
        throw new ServletException("@CODE=" + Integer.toString(ex2.getErrorCode()) + "@" + ex2.getMessage());
      } catch (Exception ex3) {
        throw new ServletException("@CODE=@" + ex3.getMessage());
      }
      if (description.length() > 255) description = new StringBuffer(description.substring(0,254));
      return description;
    }

}
