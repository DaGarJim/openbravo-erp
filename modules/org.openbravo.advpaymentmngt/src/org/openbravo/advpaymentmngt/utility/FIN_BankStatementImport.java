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

package org.openbravo.advpaymentmngt.utility;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatement;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.service.db.CallStoredProcedure;

public abstract class FIN_BankStatementImport {
  private FIN_FinancialAccount financialAccount;
  OBError myError = null;
  String filename = "";

  /** TALONES - REINTEGROS */
  public static final String DOCUMENT_BankStatementFile = "BSF";

  public FIN_BankStatementImport(FIN_FinancialAccount _financialAccount) {
    setFinancialAccount(_financialAccount);
  }

  public FIN_BankStatementImport() {
  }

  /**
   * @return the myError
   */
  public OBError getMyError() {
    return myError;
  }

  /**
   * @param myError
   *          the myError to set
   */
  public void setMyError(OBError error) {
    this.myError = error;
  }

  public void init(FIN_FinancialAccount _financialAccount) {
    setFinancialAccount(_financialAccount);
  }

  private void setFinancialAccount(FIN_FinancialAccount _financialAccount) {
    financialAccount = _financialAccount;
  }

  private InputStream getFile(VariablesSecureApp vars) throws IOException {
    FileItem fi = vars.getMultiFile("inpFile");
    if (fi == null)
      throw new IOException("Invalid filename");
    filename = fi.getName();
    InputStream in = fi.getInputStream();
    if (in == null)
      throw new IOException("Corrupted file");
    return in;
  }

  private FIN_BankStatement createFINBankStatement(ConnectionProvider conn, VariablesSecureApp vars)
      throws Exception {
    final FIN_BankStatement newBankStatement = OBProvider.getInstance()
        .get(FIN_BankStatement.class);
    newBankStatement.setAccount(financialAccount);
    DocumentType doc = null;
    try {
      doc = getDocumentType();
    } catch (Exception e) {
      throw new Exception(e);
    }
    String documentNo = getDocumentNo(conn, vars, doc);
    newBankStatement.setDocumentType(doc);
    newBankStatement.setDocumentNo(documentNo);
    newBankStatement.setOrganization(financialAccount.getOrganization());
    newBankStatement.setName(documentNo + " - " + filename);
    newBankStatement.setImportdate(new Date());
    newBankStatement.setTransactionDate(new Date());
    newBankStatement.setFileName(filename);
    OBDal.getInstance().save(newBankStatement);
    OBDal.getInstance().flush();
    return newBankStatement;
  }

  public OBError importFile(ConnectionProvider conn, VariablesSecureApp vars) {
    InputStream file = null;
    FIN_BankStatement bankStatement;
    try {
      file = getFile(vars);
    } catch (IOException e) {
      return getOBError(conn, vars, "@WrongFile@", "Error", "Error");
    }
    try {
      bankStatement = createFINBankStatement(conn, vars);
    } catch (Exception ex) {
      return getOBError(conn, vars, "@APRM_DocumentTypeNotFound@", "Error", "Error");
    }
    List<FIN_BankStatementLine> bankStatementLines = loadFile(file, bankStatement);
    int numberOfLines = saveFINBankStatementLines(bankStatementLines);
    process(bankStatement);
    if (getMyError() != null) {
      OBDal.getInstance().rollbackAndClose();
      return getMyError();
    } else
      return getOBError(conn, vars, "@NoOfLines@" + numberOfLines, "Success", "Success");
  }

  OBError getOBError(ConnectionProvider conn, VariablesSecureApp vars, String strMessage,
      String strMsgType, String strTittle) {
    OBError message = new OBError();
    message.setType(strMsgType);
    message.setTitle(Utility.messageBD(conn, strTittle, vars.getLanguage()));
    message.setMessage(Utility.parseTranslation(conn, vars, vars.getLanguage(), strMessage));
    return message;
  }

  private int saveFINBankStatementLines(List<FIN_BankStatementLine> bankStatementLines) {
    int counter = 0;
    for (FIN_BankStatementLine bankStatementLine : bankStatementLines) {
      bankStatementLine
          .setBusinessPartner(matchBusinessPartner(bankStatementLine.getBpartnername()));
      OBDal.getInstance().save(bankStatementLine);
      counter++;
    }
    OBDal.getInstance().flush();
    return counter;
  }

  private DocumentType getDocumentType() throws Exception {
    final List<Object> parameters = new ArrayList<Object>();
    parameters.add(financialAccount.getClient());
    parameters.add(financialAccount.getOrganization());
    parameters.add(DOCUMENT_BankStatementFile);
    String strDocTypeId = (String) CallStoredProcedure.getInstance().call("AD_GET_DOCTYPE",
        parameters, null);
    if (strDocTypeId == null) {
      throw new Exception("The Document Type is missing for the Bank Statement");
    }
    return new AdvPaymentMngtDao().getObject(DocumentType.class, strDocTypeId);
  }

  private String getDocumentNo(ConnectionProvider conn, VariablesSecureApp vars,
      DocumentType documentType) {
    return Utility.getDocumentNo(conn, vars, "AddPaymentFromInvoice", "FIN_Payment", documentType
        .getId(), documentType.getId(), false, true);

  }

  private void process(FIN_BankStatement bankStatement) {
    bankStatement.setProcessed(true);
    OBDal.getInstance().save(bankStatement);
    OBDal.getInstance().flush();
    return;

  }

  BusinessPartner matchBusinessPartner(String partnername) {
    // TODO extend with other matching methods. It will make it easier to later reconcile
    return matchBusinessPartnerByName(partnername);
  }

  BusinessPartner matchBusinessPartnerByName(String partnername) {
    final StringBuilder whereClause = new StringBuilder();

    OBContext.setAdminMode();
    try {

      whereClause.append(" as bsl ");
      whereClause.append(" where bsl." + FIN_BankStatementLine.PROPERTY_BPARTNERNAME + " = '"
          + partnername + "'");
      whereClause.append(" and bsl." + FIN_BankStatementLine.PROPERTY_BUSINESSPARTNER
          + " is not null");
      whereClause.append(" order by bsl." + FIN_BankStatementLine.PROPERTY_CREATIONDATE + " desc");
      final OBQuery<FIN_BankStatementLine> bsl = OBDal.getInstance().createQuery(
          FIN_BankStatementLine.class, whereClause.toString());
      List<FIN_BankStatementLine> matchedLines = bsl.list();
      if (matchedLines.size() == 0)
        return null;
      else
        return matchedLines.get(0).getBusinessPartner();

    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public abstract List<FIN_BankStatementLine> loadFile(InputStream in,
      FIN_BankStatement targetBankStatement);

}
