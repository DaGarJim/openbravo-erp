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

package org.openbravo.test.xml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.criterion.Expression;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.structure.ClientEnabled;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.db.ClientImportProcessor;
import org.openbravo.service.db.DataExportService;
import org.openbravo.service.db.DataImportService;
import org.openbravo.service.db.ImportResult;

/**
 * Tests export and import of client dataset.
 * 
 * <b>NOTE: this test has as side effect that new clients are created in the database with all their
 * data. These clients are not removed after the tests.</b>
 * 
 * @author mtaal
 */
public class ClientExportImportTest extends XMLBaseTest {

  // public void _testImportReferenceData() throws Exception {
  // setUserContext("0");
  //
  // final String sourcePath = OBPropertiesProvider.getInstance().getOpenbravoProperties()
  // .getProperty("source.path");
  // final File importDir = new File(sourcePath, ReferenceDataTask.REFERENCE_DATA_DIRECTORY);
  //
  // for (final File importFile : importDir.listFiles()) {
  // if (importFile.isDirectory()) {
  // continue;
  // }
  // final ClientImportProcessor importProcessor = new ClientImportProcessor();
  // importProcessor.setNewName(null);
  // final ImportResult ir = DataImportService.getInstance().importClientData(importProcessor,
  // false, new FileReader(importFile));
  // if (ir.hasErrorOccured()) {
  // if (ir.getException() != null) {
  // throw new OBException(ir.getException());
  // }
  // if (ir.getErrorMessages() != null) {
  // throw new OBException(ir.getErrorMessages());
  // }
  // }
  // }
  // }

  /**
   * Exports the 1000000 client and then imports as a new client. Has as side effect that a
   * completely new client is added in the database.
   * 
   * Also tests mantis 8509: https://issues.openbravo.com/view.php?id=8509
   */
  public void testExportImportClient1000000() {
    final String newClientId = exportImport("1000000");
    testMantis8509(newClientId);
    // SystemService.getInstance().removeAllClientData(newClientId);
  }

  /**
   * Exports the 1000001 client and then imports as a new client. Has as side effect that a
   * completely new client is added in the database.
   */
  public void _testExportImportClient1000001() {
    exportImport("1000001");
    // SystemService.getInstance().removeAllClientData(newClientId);
  }

  // tests mantis issue 8509 related to import of ad tree node as
  // part of client import:
  // 8509: References in the database without using foreign keys can go wrong in import
  // https://issues.openbravo.com/view.php?id=8509
  private void testMantis8509(String clientId) {
    setUserContext("0");
    final OrganizationStructureProvider osp = new OrganizationStructureProvider();
    osp.setClientId(clientId);
    final Client client = OBDal.getInstance().get(Client.class, clientId);
    final OBCriteria<Organization> os = OBDal.getInstance().createCriteria(Organization.class);
    os.setFilterOnReadableClients(false);
    os.setFilterOnReadableOrganization(false);
    os.setFilterOnActive(false);
    os.add(Expression.eq("client", client));
    for (Organization o : os.list()) {
      final Set<String> naturalTree = osp.getNaturalTree(o.getId());
      // all the organizations should at least have a tree of size 2
      if (naturalTree.size() <= 1) {
        fail("Naturaltree computation fails for organization " + o.getId() + " in imported client "
            + clientId);
      }
    }
  }

  private String exportImport(String clientId) {
    setUserContext("0");
    final Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put(DataExportService.CLIENT_ID_PARAMETER_NAME, clientId);

    final StringWriter sw = new StringWriter();
    DataExportService.getInstance().exportClientToXML(parameters, false, sw);
    String xml = sw.toString();
    try {
      final String sourcePath = (String) OBPropertiesProvider.getInstance()
          .getOpenbravoProperties().get("source.path");
      final File dir = new File(sourcePath + File.separator + "temp");
      if (!dir.exists()) {
        dir.mkdir();
      }
      final File f = new File(dir, "export.xml");
      if (f.exists()) {
        f.delete();
      }
      f.createNewFile();
      final FileWriter fw = new FileWriter(f);
      fw.write(xml);
      fw.close();
    } catch (final Exception e) {
      throw new OBException(e);
    }

    final ClientImportProcessor importProcessor = new ClientImportProcessor();
    importProcessor.setNewName("" + System.currentTimeMillis());
    try {
      final ImportResult ir = DataImportService.getInstance().importClientData(importProcessor,
          false, new StringReader(xml));
      xml = null;
      if (ir.getException() != null) {
        throw new OBException(ir.getException());
      }
      if (ir.getErrorMessages() != null) {
        fail(ir.getErrorMessages());
      }
      // none should be updated!
      assertEquals(0, ir.getUpdatedObjects().size());

      String newClientId = null;

      // and never insert anything in client 0
      for (final BaseOBObject bob : ir.getInsertedObjects()) {
        if (bob instanceof ClientEnabled) {
          final ClientEnabled ce = (ClientEnabled) bob;
          assertNotNull(ce.getClient());
          assertTrue(!ce.getClient().getId().equals("0"));
          newClientId = ce.getClient().getId();
        }
      }
      assertTrue(newClientId != null);
      assertTrue(!clientId.equals(newClientId));
      commitTransaction();
      return newClientId;
    } catch (final Exception e) {
      throw new OBException(e);
    }
  }

  public void _testImportAccountingTest() {
    doImport("Accounting_Test.xml");
  }

  private void doImport(String fileName) {
    setUserContext("0");

    final ClientImportProcessor importProcessor = new ClientImportProcessor();
    try {
      // final URL url = this.getClass().getResource("testdata/" + fileName);
      // final File f = new File(new URI(url.toString()));

      final File f = new File(fileName); // "/home/mtaal/mytmp/" +

      final ImportResult ir = DataImportService.getInstance().importClientData(importProcessor,
          false, new FileReader(f));
      if (ir.getException() != null) {
        throw new OBException(ir.getException());
      }
      if (ir.getErrorMessages() != null && ir.getErrorMessages().trim().length() > 0) {
        fail(ir.getErrorMessages());
      }
    } catch (final Exception e) {
      throw new OBException(e);
    }
  }

}