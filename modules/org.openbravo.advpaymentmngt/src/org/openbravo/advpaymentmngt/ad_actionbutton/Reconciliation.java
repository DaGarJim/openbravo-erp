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
 *************************************************************************
 */

package org.openbravo.advpaymentmngt.ad_actionbutton;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.dao.TransactionsDao;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;
import org.openbravo.xmlEngine.XmlDocument;

public class Reconciliation extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private AdvPaymentMngtDao dao;

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strOrgId = vars.getRequestGlobalVariable("inpadOrgId", "Reconciliation|Org");
      String strWindowId = vars.getRequestGlobalVariable("inpwindowId", "Reconciliation|windowId");
      String strTabId = vars.getRequestGlobalVariable("inpTabId", "Reconciliation|tabId");
      String strFinancialAccountId = vars.getStringParameter("inpfinFinancialAccountId");

      printPage(response, vars, strOrgId, strWindowId, strTabId, strFinancialAccountId, null, null);

    } else if (vars.commandIn("GRID")) {
      String strFinancialAccountId = vars.getStringParameter("inpFinFinancialAccountId", "");
      String strStatementDate = vars.getStringParameter("inpStatementDate");
      boolean strAfterDate = "Y".equals(vars.getStringParameter("inpAfterDate"));
      String selectedTransactionsIds = vars.getInParameter("inpTransactionId", IsIDFilter.instance);
      String strCurrentlyCleared = vars.getStringParameter("inpCalcCurrentlyCleared");
      String strTotalPayment = vars.getStringParameter("inpCalcTotalPayment");
      String strTotalDeposit = vars.getStringParameter("inpCalcTotalDeposit");

      printGrid(response, strFinancialAccountId, strStatementDate, strAfterDate,
          selectedTransactionsIds, strCurrentlyCleared, strTotalPayment, strTotalDeposit);

    } else if (vars.commandIn("SAVE") || vars.commandIn("PROCESS")) {
      String strTabId = vars.getGlobalVariable("inpTabId", "Reconciliation|tabId");
      String strFinancialAccountId = vars.getStringParameter("inpFinFinancialAccountId", "");
      String strDifference = vars.getStringParameter("inpCalcDifference");
      String strStatementDate = vars.getStringParameter("inpStatementDate");
      String strBeginBalance = vars.getStringParameter("inpBeginBalance");
      String strEndBalance = vars.getStringParameter("inpEndBalance");
      boolean process = vars.commandIn("PROCESS");
      processReconciliation(response, vars, strTabId, strFinancialAccountId, strDifference,
          strStatementDate, strBeginBalance, strEndBalance, process);

    } else if (vars.commandIn("UPDATESTATUS")) {
      String strSelectedTransId = vars.getStringParameter("inpCurrentTransIdSelected");
      boolean isChecked = "true".equals(vars.getStringParameter("inpIsCurrentTransSelected"));
      updateTransactionStatus(response, strSelectedTransId, isChecked);
    }

  }

  private void updateTransactionStatus(HttpServletResponse response, String strSelectedTransId,
      boolean isChecked) {

    OBContext.setAdminMode();
    try {
      FIN_FinaccTransaction trans = OBDal.getInstance().get(FIN_FinaccTransaction.class,
          strSelectedTransId);

      String newStatus = "RPPC";
      if (!isChecked) {
        newStatus = (trans.getPaymentAmount().compareTo(trans.getDepositAmount()) >= 0) ? "RDNC"
            : "PWNC";
      }
      trans.setStatus(newStatus);
      OBDal.getInstance().save(trans);
      OBDal.getInstance().flush();

      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      out.println("");
      out.close();
    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  private void processReconciliation(HttpServletResponse response, VariablesSecureApp vars,
      String strTabId, String strFinancialAccountId, String strDifference, String strStatementDate,
      String strBeginBalance, String strEndBalance, boolean process) throws IOException,
      ServletException {

    log4j
        .debug("Output: Process or Save button pressed on Financial Account || Transaction || Reconciliation manual window");

    dao = new AdvPaymentMngtDao();
    OBError msg = new OBError();
    OBContext.setAdminMode();
    try {

      FIN_FinancialAccount account = OBDal.getInstance().get(FIN_FinancialAccount.class,
          strFinancialAccountId);

      FIN_Reconciliation reconciliation = TransactionsDao.getLastReconciliation(account, null);

      FIN_Reconciliation lastProcessedReconciliation = TransactionsDao.getLastReconciliation(
          account, "Y");

      if (process) { // Validations
        String strMessage = "";
        boolean raiseException = false;
        if (new BigDecimal(strDifference).compareTo(BigDecimal.ZERO) != 0) {
          strMessage = "@APRM_ReconciliationDiscrepancy@" + " " + strDifference;
          raiseException = true;
        }

        Calendar calCurrent = Calendar.getInstance();
        calCurrent.setTime(FIN_Utility.getDateTime(strStatementDate));

        if (lastProcessedReconciliation != null) {
          Calendar calLast = Calendar.getInstance();
          calLast.setTime(lastProcessedReconciliation.getEndingDate());
          if (calCurrent.before(calLast)) {
            strMessage = "@APRM_ReconcileInFutureOrPast@";
            raiseException = true;
          }
        }

        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DATE, 1);
        tomorrow.set(Calendar.HOUR_OF_DAY, 0);
        tomorrow.set(Calendar.MINUTE, 0);
        tomorrow.set(Calendar.SECOND, 0);
        if (calCurrent.after(tomorrow)) {
          strMessage = "@APRM_ReconcileInFutureOrPast@";
          raiseException = true;
        }

        if (raiseException) {
          msg.setType("Error");
          msg.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
          msg.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), strMessage));
          vars.setMessage(strTabId, msg);
          msg = null;
          // printPage(response, vars, "", strWindowId, strTabId, strFinancialAccountId,
          // strStatementDate, strEndBalance);
          printPageClosePopUpAndRefreshParent(response, vars);
          return;
        }

      }

      DocumentType docType = FIN_Utility.getDocumentType(account.getOrganization(), "REC");
      String docNumber = FIN_Utility.getDocumentNo(account.getOrganization(), "REC",
          "DocumentNo_FIN_Reconciliation");

      if (docType == null || docNumber == null) {
        String strMessage = "@APRM_DocumentTypeNotFound@";
        msg.setType("Error");
        msg.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
        msg.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), strMessage));
        vars.setMessage(strTabId, msg);
        msg = null;
        // printPage(response, vars, "", strWindowId, strTabId, strFinancialAccountId,
        // strStatementDate, strEndBalance);
        printPageClosePopUpAndRefreshParent(response, vars);
        return;
      }

      if (reconciliation == null) {
        reconciliation = dao.getNewReconciliation(account.getOrganization(), account, docNumber,
            docType, FIN_Utility.getDateTime(strStatementDate), FIN_Utility
                .getDateTime(strStatementDate), new BigDecimal(strBeginBalance), new BigDecimal(
                strEndBalance), "CO");
      } else {
        if (reconciliation.isProcessed()) {
          reconciliation = dao.getNewReconciliation(account.getOrganization(), account, docNumber,
              docType, FIN_Utility.getDateTime(strStatementDate), FIN_Utility
                  .getDateTime(strStatementDate), new BigDecimal(strBeginBalance), new BigDecimal(
                  strEndBalance), "CO");
        } else {
          reconciliation.setEndingBalance(new BigDecimal(strEndBalance));
          reconciliation.setTransactionDate(FIN_Utility.getDateTime(strStatementDate));
          reconciliation.setEndingDate(FIN_Utility.getDateTime(strStatementDate));
        }
      }

      reconciliation.setProcessed(process);
      OBDal.getInstance().save(reconciliation);
      OBDal.getInstance().flush();

      List<FIN_FinaccTransaction> transactionsToReconcile = TransactionsDao
          .getTransactionsToReconciled(account, FIN_Utility.getDate(DateTimeData.nDaysAfter(this,
              DateTimeData.today(this), "1")), false);

      for (FIN_FinaccTransaction trans : transactionsToReconcile) {
        if (trans.getReconciliation() != null && !trans.getStatus().equals("RPPC")) {
          String newStatus = (trans.getPaymentAmount().compareTo(trans.getDepositAmount()) >= 0) ? "RDNC"
              : "PWNC";
          trans.setStatus(newStatus);
          trans.setReconciliation(null);
        } else if (trans.getReconciliation() == null && trans.getStatus().equals("RPPC")) {
          trans.setReconciliation(reconciliation);
        }
        OBDal.getInstance().save(trans);
        OBDal.getInstance().flush();
      }

      String strMessage = "@APRM_ReconciliationNo@" + ": " + reconciliation.getDocumentNo();
      msg.setType("Success");
      msg.setTitle(Utility.messageBD(this, "Success", vars.getLanguage()));
      msg.setMessage(Utility.parseTranslation(this, vars, vars.getLanguage(), strMessage));
      vars.setMessage(strTabId, msg);
      msg = null;
      printPageClosePopUpAndRefreshParent(response, vars);

    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars, String strOrgId,
      String strWindowId, String strTabId, String strFinancialAccountId, String strStatementDate,
      String strEndBalance) throws IOException, ServletException {

    log4j.debug("Output: Reconcile button pressed on Financial Account || Transaction tab");

    String dateFormat = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty(
        "dateFormat.java");
    SimpleDateFormat dateFormater = new SimpleDateFormat(dateFormat);
    OBContext.setAdminMode();
    try {

      FIN_FinancialAccount account = OBDal.getInstance().get(FIN_FinancialAccount.class,
          strFinancialAccountId);

      FIN_Reconciliation reconciliation = TransactionsDao.getLastReconciliation(account, "Y");
      FIN_Reconciliation currentReconciliation = TransactionsDao.getLastReconciliation(account,
          null);

      XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
          "org/openbravo/advpaymentmngt/ad_actionbutton/Reconciliation").createXmlDocument();

      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("theme", vars.getTheme());

      xmlDocument.setParameter("dateDisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
      xmlDocument.setParameter("mainDate", DateTimeData.today(this));
      xmlDocument.setParameter("windowId", strWindowId);
      xmlDocument.setParameter("tabId", strTabId);
      xmlDocument.setParameter("orgId", strOrgId);
      xmlDocument.setParameter("finFinancialAccountId", strFinancialAccountId);

      BigDecimal currentEndBalance = BigDecimal.ZERO;
      if (vars.commandIn("PROCESS")) {
        xmlDocument.setParameter("statementDate", strStatementDate);
        xmlDocument.setParameter("endBalance", strEndBalance);
        xmlDocument.setParameter("calcEndingBalance", strEndBalance);

      } else {
        String currentStatementDate = DateTimeData.today(this);
        if (currentReconciliation != null && !currentReconciliation.isProcessed()) {
          currentStatementDate = dateFormater.format(currentReconciliation.getTransactionDate());
        }
        xmlDocument.setParameter("statementDate", currentStatementDate);
        if (currentReconciliation != null && !currentReconciliation.isProcessed()) {
          currentEndBalance = currentReconciliation.getEndingBalance();
        }
        xmlDocument.setParameter("endBalance", currentEndBalance.toString());
        xmlDocument.setParameter("calcEndingBalance", currentEndBalance.toString());
      }

      BigDecimal beginBalance = (reconciliation == null) ? BigDecimal.ZERO : reconciliation
          .getEndingBalance();

      xmlDocument.setParameter("account", account.getName());
      xmlDocument.setParameter("beginBalance", beginBalance.toString());

      // Hidden inputs
      xmlDocument.setParameter("calcBeginningBalance", beginBalance.toString());
      xmlDocument.setParameter("calcTotalPayment", BigDecimal.ZERO.toString());
      xmlDocument.setParameter("calcTotalDeposit", BigDecimal.ZERO.toString());
      xmlDocument.setParameter("calcDifferenceToClear", currentEndBalance.subtract(beginBalance)
          .toString());
      xmlDocument.setParameter("calcCurrentlyCleared", BigDecimal.ZERO.toString());
      xmlDocument.setParameter("calcDifference", currentEndBalance.subtract(beginBalance)
          .toString());

      OBContext.setAdminMode();
      try {
        xmlDocument.setParameter("precision", account.getCurrency().getStandardPrecision()
            .toString());
      } finally {
        OBContext.restorePreviousMode();
      }

      OBError myMessage = vars.getMessage(strWindowId);
      vars.removeMessage(strWindowId);
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }

      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      out.println(xmlDocument.print());
      out.close();
    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void printGrid(HttpServletResponse response, String strFinancialAccountId,
      String strStatmentDate, boolean afterDate, String selectedTransactionsIds,
      String strCurrentlyCleared, String strTotalPayment, String strTotalDeposit)
      throws IOException, ServletException {

    log4j.debug("Output: Grid on Financial Account || Transaction tab || Reconciliation window");

    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "org/openbravo/advpaymentmngt/ad_actionbutton/ReconciliationGrid").createXmlDocument();

    FIN_FinancialAccount account = OBDal.getInstance().get(FIN_FinancialAccount.class,
        strFinancialAccountId);

    Map<String, String> map = FIN_Utility.getMapFromStringList(selectedTransactionsIds);

    FieldProvider[] data = TransactionsDao.getTransactionsFiltered(account, FIN_Utility
        .getDate(DateTimeData.nDaysAfter(this, strStatmentDate, "1")), afterDate);

    BigDecimal currentlyCleared = new BigDecimal(strCurrentlyCleared);
    BigDecimal totalPayment = new BigDecimal(strTotalPayment);
    BigDecimal totalDeposit = new BigDecimal(strTotalDeposit);

    for (FieldProvider fp : data) {
      if (!map.containsKey(fp.getField("transactionId"))
          && !fp.getField("markSelectedId").isEmpty()) {
        BigDecimal payAmt = new BigDecimal(fp.getField("paymentAmount"));
        BigDecimal depAmt = new BigDecimal(fp.getField("depositAmount"));
        currentlyCleared = currentlyCleared.add(payAmt).subtract(depAmt);
        totalPayment = totalPayment.add(payAmt);
        totalDeposit = totalDeposit.add(depAmt);
      }
    }

    xmlDocument.setParameter("calcTotalPayment", totalPayment.toString());
    xmlDocument.setParameter("caclTotalDeposit", totalDeposit.toString());
    xmlDocument.setParameter("calcCurrentlyCleared", currentlyCleared.toString());
    xmlDocument.setData("structure", data);

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  public String getServletInfo() {
    return "This servlet manages manual transactions reconciliations.";
  }

}
