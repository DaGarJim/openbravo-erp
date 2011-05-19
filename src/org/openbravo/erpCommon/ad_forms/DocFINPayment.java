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
 * All portions are Copyright (C) 2010-2011 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_forms;

import java.math.BigDecimal;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.hibernate.ejb.metamodel.AbstractManagedType;
import org.openbravo.base.model.domaintype.BigDecimalDomainType;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.util.Convert;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.FieldProviderFactory;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.common.enterprise.AcctSchemaTableDocType;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchemaDefault;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchemaTable;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;

public class DocFINPayment extends AcctServer {
  private static final long serialVersionUID = 1L;
  static Logger log4j = Logger.getLogger(DocFINPayment.class);

  String SeqNo = "0";
  String generatedAmount = "";
  String usedAmount = "";
  String financialAccountCurrencyId;
  String financialTransactionAmount;
  String financialTransactionConvertRate;

  public DocFINPayment() {
  }

  public DocFINPayment(String AD_Client_ID, String AD_Org_ID, ConnectionProvider connectionProvider) {
    super(AD_Client_ID, AD_Org_ID, connectionProvider);
  }

  @Override
  public boolean loadDocumentDetails(FieldProvider[] data, ConnectionProvider conn) {
    DateDoc = data[0].getField("PaymentDate");
    Amounts[AMTTYPE_Gross] = data[0].getField("Amount");
    generatedAmount = data[0].getField("GeneratedCredit");
    usedAmount = data[0].getField("UsedCredit");
    financialAccountCurrencyId =data[0].getField("FinancialAccountCurrencyId");
    financialTransactionAmount=data[0].getField("FinancialTransactionAmount");
    financialTransactionConvertRate =data[0].getField("FinancialTransactionConvertRate");

    loadDocumentType();
    p_lines = loadLines();
    return true;
  }

  public FieldProviderFactory[] loadLinesFieldProvider(String Id) {
    FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, Id);
    List<FIN_PaymentDetail> paymentDetails = payment.getFINPaymentDetailList();
    if (paymentDetails == null)
      return null;

    String dateFormat = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty(
        "dateFormat.java");
    SimpleDateFormat outputFormat = new SimpleDateFormat(dateFormat);

    FieldProviderFactory[] data = new FieldProviderFactory[paymentDetails.size()];
    OBContext.setAdminMode();
    try {
      for (int i = 0; i < data.length; i++) {
        // Details refunded used credit are excluded as the entry will be created using the credit
        // used
        if (paymentDetails.get(i).isRefund() && paymentDetails.get(i).isPrepayment())
          continue;
        data[i] = new FieldProviderFactory(null);
        FieldProviderFactory.setField(data[i], "AD_Client_ID", paymentDetails.get(i).getClient()
            .getId());
        FieldProviderFactory.setField(data[i], "AD_Org_ID", paymentDetails.get(i).getOrganization()
            .getId());
        FieldProviderFactory.setField(data[i], "FIN_Payment_Detail_ID", paymentDetails.get(i)
            .getId());
        FieldProviderFactory.setField(data[i], "Amount", paymentDetails.get(i).getAmount()
            .toString());
        FieldProviderFactory.setField(data[i], "isprepayment",
            paymentDetails.get(i).isPrepayment() ? "Y" : "N");
        FieldProviderFactory.setField(data[i], "WriteOffAmt", paymentDetails.get(i)
            .getWriteoffAmount().toString());
        FieldProviderFactory.setField(data[i], "C_GLItem_ID",
            paymentDetails.get(i).getGLItem() != null ? paymentDetails.get(i).getGLItem().getId()
                : "");
        FieldProviderFactory.setField(data[i], "Refund", paymentDetails.get(i).isRefund() ? "Y"
            : "N");
        FieldProviderFactory.setField(data[i], "isprepayment",
            paymentDetails.get(i).isPrepayment() ? "Y" : "N");

        String invoiceAccountingDate = "";
        for(FIN_PaymentScheduleDetail detail : paymentDetails.get(i).getFINPaymentScheduleDetailList() ) {
          if( detail.getInvoicePaymentSchedule() != null &&
              detail.getInvoicePaymentSchedule().getInvoice() != null ) {
            final Date accountingDate = detail.getInvoicePaymentSchedule().getInvoice().getAccountingDate();
            invoiceAccountingDate = outputFormat.format(accountingDate);
      }
        }
        FieldProviderFactory.setField(data[i], "invoiceAccountingDate", invoiceAccountingDate);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return data;
  }

  private DocLine[] loadLines() {
    ArrayList<Object> list = new ArrayList<Object>();
    FieldProviderFactory[] data = loadLinesFieldProvider(Record_ID);
    if (data == null || data.length == 0)
      return null;
    for (int i = 0; i < data.length; i++) {
      if (data[i] == null)
        continue;
      String Line_ID = data[i].getField("FIN_Payment_Detail_ID");
      OBContext.setAdminMode();
      try {
        FIN_PaymentDetail detail = OBDal.getInstance().get(FIN_PaymentDetail.class, Line_ID);
        DocLine_FINPayment docLine = new DocLine_FINPayment(DocumentType, Record_ID, Line_ID);
        docLine.loadAttributes(data[i], this);
        docLine.setAmount(data[i].getField("Amount"));
        docLine.setIsPrepayment(data[i].getField("isprepayment"));
        docLine.setWriteOffAmt(data[i].getField("WriteOffAmt"));
        docLine.setC_GLItem_ID(data[i].getField("C_GLItem_ID"));
        for(FIN_PaymentScheduleDetail paymentSchedule : detail.getFINPaymentScheduleDetailList() ) {
          if( paymentSchedule.getInvoicePaymentSchedule() != null &&
              paymentSchedule.getInvoicePaymentSchedule().getInvoice() != null ) {
            docLine.setInvoice(paymentSchedule.getInvoicePaymentSchedule().getInvoice() );
          }
        }

        list.add(docLine);
      } finally {
        OBContext.restorePreviousMode();
      }
    }
    // Return Array
    DocLine_FINPayment[] dl = new DocLine_FINPayment[list.size()];
    list.toArray(dl);
    return dl;
  } // loadLines

  @Override
  public Fact createFact(AcctSchema as, ConnectionProvider conn, Connection con,
      VariablesSecureApp vars) throws ServletException {
    // Select specific definition
    String strClassname = "";
    final StringBuilder whereClause = new StringBuilder();
    Fact fact = new Fact(this, as, Fact.POST_Actual);
    String Fact_Acct_Group_ID = SequenceIdData.getUUID();
    OBContext.setAdminMode();
    try {
      whereClause.append(" as astdt ");
      whereClause.append(" where astdt.acctschemaTable.accountingSchema.id = '"
          + as.m_C_AcctSchema_ID + "'");
      whereClause.append(" and astdt.acctschemaTable.table.id = '" + AD_Table_ID + "'");
      whereClause.append(" and astdt.documentCategory = '" + DocumentType + "'");

      final OBQuery<AcctSchemaTableDocType> obqParameters = OBDal.getInstance().createQuery(
          AcctSchemaTableDocType.class, whereClause.toString());
      final List<AcctSchemaTableDocType> acctSchemaTableDocTypes = obqParameters.list();

      if (acctSchemaTableDocTypes != null && acctSchemaTableDocTypes.size() > 0)
        strClassname = acctSchemaTableDocTypes.get(0).getCreatefactTemplate().getClassname();

      if (strClassname.equals("")) {
        final StringBuilder whereClause2 = new StringBuilder();

        whereClause2.append(" as ast ");
        whereClause2.append(" where ast.accountingSchema.id = '" + as.m_C_AcctSchema_ID + "'");
        whereClause2.append(" and ast.table.id = '" + AD_Table_ID + "'");

        final OBQuery<AcctSchemaTable> obqParameters2 = OBDal.getInstance().createQuery(
            AcctSchemaTable.class, whereClause2.toString());
        final List<AcctSchemaTable> acctSchemaTables = obqParameters2.list();
        if (acctSchemaTables != null && acctSchemaTables.size() > 0
            && acctSchemaTables.get(0).getCreatefactTemplate() != null)
          strClassname = acctSchemaTables.get(0).getCreatefactTemplate().getClassname();
      }
      if (!strClassname.equals("")) {
        try {
          DocFINPaymentTemplate newTemplate = (DocFINPaymentTemplate) Class.forName(strClassname)
              .newInstance();
          return newTemplate.createFact(this, as, conn, con, vars);
        } catch (Exception e) {
          log4j.error("Error while creating new instance for DocFINPaymentTemplate - ", e);
        }
      }

      final boolean isReceipt = DocumentType.equals("ARR");

      for (int i = 0; p_lines != null && i < p_lines.length; i++) {
        DocLine_FINPayment line = (DocLine_FINPayment) p_lines[i];

        boolean isPrepayment = line.getIsPrepayment().equals("Y");

        String bpAmount = line.getAmount();
        if (line.WriteOffAmt != null && !line.WriteOffAmt.equals("")
            && new BigDecimal(line.WriteOffAmt).compareTo(ZERO) != 0) {
          fact.createLine(line, getAccount(AcctServer.ACCTTYPE_WriteOffDefault, as, conn),
              C_Currency_ID, (isReceipt ? line.WriteOffAmt : ""), (isReceipt ? ""
                  : line.WriteOffAmt), Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
          bpAmount = new BigDecimal(bpAmount).add(new BigDecimal(line.WriteOffAmt)).toString();
        }
        // Multicurrency balancing

        if(!C_Currency_ID.equals(financialAccountCurrencyId)) {
          // If payment currency different from account currency, flag as multicurrency so that
          // source balancing doesn't try match amounts in different currencies
          MultiCurrency = true;
        }
        if ("".equals(line.getC_GLItem_ID())) {
          Invoice invoice = line.getInvoice();
          String invoiceAccountingDate = null;
          if (invoice != null && !C_Currency_ID.equals(as.m_C_Currency_ID)) {
            String dateFormat = OBPropertiesProvider.getInstance().getOpenbravoProperties()
                .getProperty("dateFormat.java");
            SimpleDateFormat outputFormat = new SimpleDateFormat(dateFormat);
            invoiceAccountingDate = outputFormat.format(invoice.getAccountingDate());
          }
          fact
              .createLine(line,
                  getAccountBPartner((line.m_C_BPartner_ID == null || line.m_C_BPartner_ID
                      .equals("")) ? this.C_BPartner_ID : line.m_C_BPartner_ID, as, isReceipt,
                      isPrepayment, conn), C_Currency_ID, (isReceipt ? "" : bpAmount),
                  (isReceipt ? bpAmount : ""), Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType,
                  invoiceAccountingDate, conn);
        } else {
          fact.createLine(line, getAccountGLItem(OBDal.getInstance().get(GLItem.class,
              line.getC_GLItem_ID()), as, isReceipt, conn), C_Currency_ID, (isReceipt ? ""
              : bpAmount), (isReceipt ? bpAmount : ""), Fact_Acct_Group_ID, nextSeqNo(SeqNo),
              DocumentType, conn);
        }
      }
      FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, Record_ID);
      fact.createLine(null, getAccount(conn, payment.getPaymentMethod(), payment.getAccount(), as,
          payment.isReceipt()), financialAccountCurrencyId,
          (payment.isReceipt() ? financialTransactionAmount : ""),
          (payment.isReceipt() ? "" : financialTransactionAmount), Fact_Acct_Group_ID, "999999",
          DocumentType, conn);
      // Pre-payment is consumed when Used Credit Amount not equals Zero. When consuming Credit no
      // credit is generated
      if (new BigDecimal(usedAmount).compareTo(ZERO) != 0
          && new BigDecimal(generatedAmount).compareTo(ZERO) == 0) {
        fact.createLine(null,
            getAccountBPartner(C_BPartner_ID, as, payment.isReceipt(), true, conn), C_Currency_ID,
            (payment.isReceipt() ? usedAmount : ""), (payment.isReceipt() ? "" : usedAmount),
            Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
      }

      if( MultiCurrency ) {
        createFactCurrencyBalancing(as, conn, fact, Fact_Acct_Group_ID, payment);
      }
    } finally {
      OBContext.restorePreviousMode();
    }

    SeqNo = "0";
    return fact;
  }

  private void createFactCurrencyBalancing(AcctSchema as, ConnectionProvider conn, Fact fact,
      String fact_Acct_Group_ID, FIN_Payment payment) throws ServletException {
    final BigDecimal acctBalance = fact.getAcctBalance();
    if( BigDecimal.ZERO.compareTo(acctBalance) != 0 ) {
      // Need to add balancing entry
      // Balance == AcctDr - AcctCr
      String currencyLossDR = "0";
      String currencyGainCR = "0";
      boolean isGain = acctBalance.compareTo(BigDecimal.ZERO) > 0;
      if(isGain) {
        // debit > credit = need to credit
        currencyGainCR = Convert.toString(acctBalance);
      } else {
        // debit < credit = need to debit
        currencyLossDR = Convert.toString(acctBalance.negate());
      }

      String currencyLossGainAcctComboId = null;
      // Find gain / loss accounts from account
      for( FIN_FinancialAccountAccounting accounting:
          payment.getAccount().getFINFinancialAccountAcctList() ) {
        if( accounting.getAccountingSchema().getId().equals(as.getC_AcctSchema_ID())) {
          AccountingCombination revaluationAcct;
          if( isGain ){
            revaluationAcct = accounting.getFINBankrevaluationgainAcct();
          } else {
            revaluationAcct = accounting.getFINBankrevaluationlossAcct();
          }
          if( revaluationAcct != null ) {
            currencyLossGainAcctComboId = revaluationAcct.getId();
          }
          break;
        }
      }
      if( currencyLossGainAcctComboId == null) {
        // Find default gain/loss accounts from schema
        final OBQuery<AcctSchemaDefault> obqAcctSchemDefault= OBDal.getInstance().createQuery(
            AcctSchemaDefault.class, " where accountingSchema.id = '" + as.m_C_AcctSchema_ID + "'");
        final AcctSchemaDefault acctSchemaDefault = obqAcctSchemDefault.list().get(0);
        AccountingCombination defaultRevaluationAcct;
        if( isGain) {
          defaultRevaluationAcct = acctSchemaDefault.getBankRevaluationGain();
        } else {
          defaultRevaluationAcct = acctSchemaDefault.getBankRevaluationLoss();
        }
        if(currencyLossGainAcctComboId == null && defaultRevaluationAcct != null ) {
          currencyLossGainAcctComboId = defaultRevaluationAcct.getId();
        }
      }
      Account accountGainLoss= Account.getAccount(conn, currencyLossGainAcctComboId);
      if( accountGainLoss == null ) {
        // Fall back to currency balancing
        accountGainLoss = as.getCurrencyBalancing_Acct();
      }


      if( accountGainLoss != null ) {
        final FactLine line = fact.createLine(null, accountGainLoss, as.getC_Currency_ID(), currencyLossDR, currencyGainCR, fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
        line.setAmtSource(C_Currency_ID,"0","0"); // Mimic normal currency balancing
      }
    }
  }

  public String nextSeqNo(String oldSeqNo) {
    BigDecimal seqNo = new BigDecimal(oldSeqNo);
    SeqNo = (seqNo.add(new BigDecimal("10"))).toString();
    return SeqNo;
  }

  /**
   * Get Source Currency Balance - subtracts line amounts from total - no rounding
   * 
   * @return positive amount, if total is bigger than lines
   */
  @Override
  public BigDecimal getBalance() {
    BigDecimal retValue = ZERO;
    StringBuffer sb = new StringBuffer(" [");
    // Total
    retValue = retValue.add(new BigDecimal(getAmount(AcctServer.AMTTYPE_Gross)));
    sb.append(getAmount(AcctServer.AMTTYPE_Gross));
    // - Lines
    for (int i = 0; i < p_lines.length; i++) {
      BigDecimal lineBalance = new BigDecimal(((DocLine_FINPayment) p_lines[i]).Amount);
      retValue = retValue.subtract(lineBalance);
      sb.append("-").append(lineBalance);
    }
    sb.append("]");
    //
    log4j.debug(" Balance=" + retValue + sb.toString());
    return retValue;
  } // getBalance

  @Override
  public boolean getDocumentConfirmation(ConnectionProvider conn, String strRecordId) {
    // Checks if this step is configured to generate accounting for the selected financial account
    boolean confirmation = false;
    final String PAYMENT_RECEIVED = "RPR";
    final String PAYMENT_MADE = "PPM";
    final String DEPOSITED_NOT_CLEARED = "RDNC";
    final String WITHDRAWN_NOT_CLEARED = "PWNC";
    final String PAYMENT_CLEARED = "RPPC";
    OBContext.setAdminMode();
    try {
      FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, strRecordId);
      // Posting can just happen if payment is in the right status
      if (payment.getStatus().equals(PAYMENT_RECEIVED) || payment.getStatus().equals(PAYMENT_MADE)
          || payment.getStatus().equals(DEPOSITED_NOT_CLEARED)
          || payment.getStatus().equals(WITHDRAWN_NOT_CLEARED)
          || payment.getStatus().equals(PAYMENT_CLEARED)) {
        OBCriteria<FinAccPaymentMethod> obCriteria = OBDal.getInstance().createCriteria(
            FinAccPaymentMethod.class);
        obCriteria.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACCOUNT, payment.getAccount()));
        obCriteria.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD, payment
            .getPaymentMethod()));
        obCriteria.setFilterOnReadableClients(false);
        obCriteria.setFilterOnReadableOrganization(false);
        List<FinAccPaymentMethod> lines = obCriteria.list();
        List<FIN_FinancialAccountAccounting> accounts = payment.getAccount()
            .getFINFinancialAccountAcctList();
        for (FIN_FinancialAccountAccounting account : accounts) {
          if (confirmation)
            return confirmation;
          if (payment.isReceipt()) {
            if (("INT").equals(lines.get(0).getUponReceiptUse())
                && account.getInTransitPaymentAccountIN() != null)
              confirmation = true;
            else if (("DEP").equals(lines.get(0).getUponReceiptUse())
                && account.getDepositAccount() != null)
              confirmation = true;
            else if (("CLE").equals(lines.get(0).getUponReceiptUse())
                && account.getClearedPaymentAccount() != null)
              confirmation = true;
          } else {
            if (("INT").equals(lines.get(0).getUponPaymentUse())
                && account.getFINOutIntransitAcct() != null)
              confirmation = true;
            else if (("WIT").equals(lines.get(0).getUponPaymentUse())
                && account.getWithdrawalAccount() != null)
              confirmation = true;
            else if (("CLE").equals(lines.get(0).getUponPaymentUse())
                && account.getClearedPaymentAccountOUT() != null)
              confirmation = true;
          }
          // For payments with Amount ZERO always create an entry as no transaction will be created
          if (payment.getAmount().compareTo(ZERO) == 0) {
            confirmation = true;
          }
        }
      }
    } catch (Exception e) {
      setStatus(STATUS_DocumentDisabled);
      return confirmation;
    } finally {
      OBContext.restorePreviousMode();
    }
    if (!confirmation)
      setStatus(STATUS_DocumentDisabled);
    return confirmation;
  }

  @Override
  public void loadObjectFieldProvider(ConnectionProvider conn, String strAD_Client_ID, String Id)
      throws ServletException {
    FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, Id);
    FieldProviderFactory[] data = new FieldProviderFactory[1];
    data[0] = new FieldProviderFactory(null);
    FieldProviderFactory.setField(data[0], "AD_Client_ID", payment.getClient().getId());
    FieldProviderFactory.setField(data[0], "AD_Org_ID", payment.getOrganization().getId());
    FieldProviderFactory.setField(data[0], "C_BPartner_ID",
        payment.getBusinessPartner() != null ? payment.getBusinessPartner().getId() : "");
    FieldProviderFactory.setField(data[0], "DocumentNo", payment.getDocumentNo());
    String dateFormat = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty(
        "dateFormat.java");
    SimpleDateFormat outputFormat = new SimpleDateFormat(dateFormat);
    FieldProviderFactory.setField(data[0], "PaymentDate", outputFormat.format(payment
        .getPaymentDate()));
    FieldProviderFactory.setField(data[0], "C_DocType_ID", payment.getDocumentType().getId());
    FieldProviderFactory.setField(data[0], "C_Currency_ID", payment.getCurrency().getId());
    FieldProviderFactory.setField(data[0], "Amount", payment.getAmount().toString());
    FieldProviderFactory.setField(data[0], "GeneratedCredit", payment.getGeneratedCredit()
        .toString());
    FieldProviderFactory.setField(data[0], "UsedCredit", payment.getUsedCredit().toString());
    FieldProviderFactory.setField(data[0], "WriteOffAmt", payment.getWriteoffAmount().toString());
    FieldProviderFactory.setField(data[0], "Description", payment.getDescription());
    FieldProviderFactory.setField(data[0], "FinancialAccountCurrencyId", payment.getAccount().getCurrency().getId());
    FieldProviderFactory.setField(data[0], "FinancialTransactionAmount", payment.getFinancialTransactionAmount().toString());
    FieldProviderFactory.setField(data[0], "FinancialTransactionConvertRate", payment.getFinancialTransactionConvertRate().toString());
    FieldProviderFactory.setField(data[0], "Posted", payment.getPosted());
    FieldProviderFactory.setField(data[0], "Processed", payment.isProcessed() ? "Y" : "N");
    FieldProviderFactory.setField(data[0], "Processing", payment.isProcessNow() ? "Y" : "N");
    FieldProviderFactory.setField(data[0], "C_Project_ID", payment.getProject() != null ? payment
        .getProject().getId() : "");
    FieldProviderFactory.setField(data[0], "C_Campaign_ID",
        payment.getSalesCampaign() != null ? payment.getSalesCampaign().getId() : "");
    FieldProviderFactory.setField(data[0], "C_Activity_ID", payment.getActivity() != null ? payment
        .getActivity().getId() : "");
    // This lines can be uncommented when User1 and User2 are implemented
    // FieldProviderFactory.setField(data[0], "User1_ID", payment.getStDimension().getId());
    // FieldProviderFactory.setField(data[0], "User2_ID", payment.getNdDimension().getId());
    setObjectFieldProvider(data);
  }

  /*
   * Retrieves Account for receipt / Payment for the given payment method + Financial Account
   */
  public Account getAccount(ConnectionProvider conn, FIN_PaymentMethod paymentMethod,
      FIN_FinancialAccount finAccount, AcctSchema as, boolean bIsReceipt) throws ServletException {
    OBContext.setAdminMode();
    Account account = null;
    try {
      OBCriteria<FIN_FinancialAccountAccounting> accounts = OBDal.getInstance().createCriteria(
          FIN_FinancialAccountAccounting.class);
      accounts.add(Restrictions.eq(FIN_FinancialAccountAccounting.PROPERTY_ACCOUNT, finAccount));
      accounts.add(Restrictions.eq(FIN_FinancialAccountAccounting.PROPERTY_ACCOUNTINGSCHEMA, OBDal
          .getInstance().get(org.openbravo.model.financialmgmt.accounting.coa.AcctSchema.class,
              as.m_C_AcctSchema_ID)));
      accounts.add(Restrictions.eq(FIN_FinancialAccountAccounting.PROPERTY_ACTIVE, true));
      accounts.setFilterOnReadableClients(false);
      accounts.setFilterOnReadableOrganization(false);
      List<FIN_FinancialAccountAccounting> accountList = accounts.list();
      if (accountList == null || accountList.size() == 0)
        return null;
      OBCriteria<FinAccPaymentMethod> accPaymentMethod = OBDal.getInstance().createCriteria(
          FinAccPaymentMethod.class);
      accPaymentMethod.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACCOUNT, finAccount));
      accPaymentMethod.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD,
          paymentMethod));
      accPaymentMethod.setFilterOnReadableClients(false);
      accPaymentMethod.setFilterOnReadableOrganization(false);
      List<FinAccPaymentMethod> lines = accPaymentMethod.list();
      if (bIsReceipt) {
        account = getAccount(conn, lines.get(0).getUponReceiptUse(), accountList.get(0), bIsReceipt);
      } else {
        account = getAccount(conn, lines.get(0).getUponPaymentUse(), accountList.get(0), bIsReceipt);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return account;
  }

  public String getSeqNo() {
    return SeqNo;
  }

  public void setSeqNo(String seqNo) {
    SeqNo = seqNo;
  }

  public String getGeneratedAmount() {
    return generatedAmount;
  }

  public void setGeneratedAmount(String generatedAmount) {
    this.generatedAmount = generatedAmount;
  }

  public String getUsedAmount() {
    return usedAmount;
  }

  public void setUsedAmount(String usedAmount) {
    this.usedAmount = usedAmount;
  }
}