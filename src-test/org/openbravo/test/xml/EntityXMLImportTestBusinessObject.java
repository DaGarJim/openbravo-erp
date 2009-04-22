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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Expression;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.xml.EntityXMLConverter;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.payment.PaymentTermLine;
import org.openbravo.model.financialmgmt.payment.PaymentTermTrl;
import org.openbravo.service.db.DataImportService;
import org.openbravo.service.db.ImportResult;

/**
 * Test import of data with a business object, adding and removing childs
 * 
 * @author mtaal
 */

public class EntityXMLImportTestBusinessObject extends XMLBaseTest {

  private static final Logger log = Logger.getLogger(EntityXMLImportTestBusinessObject.class);

  private static int NO_OF_PT = 1;
  private static int NO_OF_PT_LINE = 1 + NO_OF_PT * NO_OF_PT;
  // add NO_OF_PT twice because it was translated to one language
  private static int TOTAL_PT_PTL = NO_OF_PT + NO_OF_PT + NO_OF_PT_LINE;

  private String[] currentPaymentTerms = new String[] { "1000000", "1000001", "1000002", "1000003",
      "1000004" };

  public void testAPaymentTerm() {
    cleanRefDataLoaded();
    setUserContext("1000000");
    createSavePaymentTerm();
  }

  // export and create in client 100001
  public void testBPaymentTerm() {

    // read from 1000000
    setUserContext("1000000");
    setAccess();

    final List<PaymentTerm> pts = getPaymentTerms();
    String xml = getXML(pts);

    log.debug(xml);

    // there is a unique constraint on name
    xml = xml.replaceAll("</name>", "t</name>");

    // export to client 1000001
    setUserContext("1000019");
    // don't be bothered by access checks...
    setAccess();
    final ImportResult ir = DataImportService.getInstance().importDataFromXML(
        OBDal.getInstance().get(Client.class, "1000001"),
        OBDal.getInstance().get(Organization.class, "1000001"), xml);
    if (ir.getException() != null) {
      ir.getException().printStackTrace(System.err);
      fail(ir.getException().getMessage());
    } else if (ir.getErrorMessages() != null) {
      fail(ir.getErrorMessages());
    }

    assertEquals(TOTAL_PT_PTL, ir.getInsertedObjects().size());
    assertEquals(0, ir.getUpdatedObjects().size());
  }

  // do the same thing again, no updates!
  public void testCPaymentTerm() {

    // read from 1000000
    setUserContext("1000000");
    setAccess();
    final List<PaymentTerm> pts = getPaymentTerms();
    String xml = getXML(pts);

    // there is a unique constraint on name
    xml = xml.replaceAll("</name>", "t</name>");

    // export to client 1000001
    setUserContext("1000019");
    setAccess();
    final ImportResult ir = DataImportService.getInstance().importDataFromXML(
        OBDal.getInstance().get(Client.class, "1000001"),
        OBDal.getInstance().get(Organization.class, "1000001"), xml);
    if (ir.getException() != null) {
      ir.getException().printStackTrace(System.err);
      fail(ir.getException().getMessage());
    }

    assertEquals(0, ir.getInsertedObjects().size());
    assertEquals(0, ir.getUpdatedObjects().size());
  }

  // change a child so that it is updated and change a parent
  public void testDPaymentTerm() {

    // read from 1000000
    setUserContext("1000000");
    setAccess();

    // make a copy of the paymentterms and their children so that the
    // original db is not updated
    final List<BaseOBObject> pts = DalUtil.copyAll(new ArrayList<BaseOBObject>(getPaymentTerms()),
        false);

    // change some data and export
    final PaymentTerm pt = (PaymentTerm) pts.get(0);
    pt.setName("testtest");
    pt.getFinancialMgmtPaymentTermLineList().get(0).setOverduePaymentDayRule("2");

    String xml = getXML(pts);
    xml = xml.replaceAll("</name>", "t</name>");

    setUserContext("1000019");
    setAccess();
    final ImportResult ir = DataImportService.getInstance().importDataFromXML(
        OBDal.getInstance().get(Client.class, "1000001"),
        OBDal.getInstance().get(Organization.class, "1000001"), xml);
    if (ir.getException() != null) {
      ir.getException().printStackTrace(System.err);
      fail(ir.getException().getMessage());
    }

    assertEquals(0, ir.getInsertedObjects().size());
    assertEquals(2, ir.getUpdatedObjects().size());
    for (final Object o : ir.getUpdatedObjects()) {
      assertTrue(o instanceof PaymentTerm || o instanceof PaymentTermLine);
      if (o instanceof PaymentTermLine) {
        final PaymentTermLine ptl = (PaymentTermLine) o;
        assertTrue(ir.getUpdatedObjects().contains(ptl.getPaymentTerms()));
      }
    }
  }

  // remove the first payment line of each payment term
  public void testEPaymentTerm() {

    // read from 1000000
    setUserContext("1000000");
    setAccess();
    // make a copy of the paymentterms and their children so that the
    // original db is not updated
    final List<BaseOBObject> pts = DalUtil.copyAll(new ArrayList<BaseOBObject>(getPaymentTerms()),
        false);

    for (final BaseOBObject bob : pts) {
      final PaymentTerm pt = (PaymentTerm) bob;
      final PaymentTermLine ptl = pt.getFinancialMgmtPaymentTermLineList().get(1);
      pt.getFinancialMgmtPaymentTermLineList().remove(ptl);
    }

    String xml = getXML(pts);
    // there is a unique constraint on name
    xml = xml.replaceAll("</name>", "t</name>");

    setUserContext("1000019");
    // a payment term line is not deletable, but for this test it should be done anyway
    // force this by being admin
    OBContext.getOBContext().setInAdministratorMode(true);
    final ImportResult ir = DataImportService.getInstance().importDataFromXML(
        OBDal.getInstance().get(Client.class, "1000001"),
        OBDal.getInstance().get(Organization.class, "1000001"), xml);
    if (ir.getException() != null) {
      ir.getException().printStackTrace(System.err);
      fail(ir.getException().getMessage());
    }

    assertEquals(0, ir.getInsertedObjects().size());
    // name of paymentterm has changed
    // overduepaymentrule of paymenttermline is set back to 1
    assertEquals(2, ir.getUpdatedObjects().size());
    for (final Object o : ir.getUpdatedObjects()) {
      assertTrue(o instanceof PaymentTerm || o instanceof PaymentTermLine);
    }
  }

  // test that the removal was successfull
  public void testFPaymentTerm() {
    setUserContext("1000019");
    final List<PaymentTerm> pts = getPaymentTerms();
    for (final PaymentTerm pt : pts) {
      assertEquals(NO_OF_PT_LINE - 1, pt.getFinancialMgmtPaymentTermLineList().size());
      for (final PaymentTermLine ptl : pt.getFinancialMgmtPaymentTermLineList()) {
        assertTrue(!ptl.getLineNo().equals(new Integer(1)));
      }
    }
  }

  // and now add a line!
  public void testGPaymentTerm() {

    // read from 1000000
    setUserContext("1000019");
    setAccess();
    // make a copy of the paymentterms and their children so that the
    // original db is not updated
    final List<BaseOBObject> pts = DalUtil.copyAll(new ArrayList<BaseOBObject>(getPaymentTerms()),
        true);

    // add one at the back
    for (final BaseOBObject bob : pts) {
      final PaymentTerm pt = (PaymentTerm) bob;
      pt.setId("abc");
      final PaymentTermLine ptl0 = pt.getFinancialMgmtPaymentTermLineList().get(0);
      ptl0.setPaymentTerms(pt);
      final PaymentTermLine ptl = (PaymentTermLine) DalUtil.copy(ptl0);
      ptl.setId(null);
      ptl.setClient(null);
      ptl.setOrganization(null);
      ptl.setLineNo((long) NO_OF_PT_LINE);
      pt.getFinancialMgmtPaymentTermLineList().add(ptl);
      ptl.setPaymentTerms(pt);
    }

    String xml = getXML(pts);
    // log.debug(xml);
    // there is a unique constraint on name
    xml = xml.replaceAll("</name>", "t</name>");

    setUserContext("1000019");
    setAccess();
    final ImportResult ir = DataImportService.getInstance().importDataFromXML(
        OBDal.getInstance().get(Client.class, "1000001"),
        OBDal.getInstance().get(Organization.class, "1000001"), xml);
    if (ir.getException() != null) {
      ir.getException().printStackTrace(System.err);
      fail(ir.getException().getMessage());
    }

    assertEquals(NO_OF_PT + NO_OF_PT_LINE, ir.getInsertedObjects().size());
    assertEquals(NO_OF_PT, ir.getUpdatedObjects().size());
    for (final Object o : ir.getUpdatedObjects()) {
      assertTrue(o instanceof PaymentTermTrl);
    }
    for (final Object o : ir.getInsertedObjects()) {
      assertTrue(o instanceof PaymentTerm || o instanceof PaymentTermLine);
    }
  }

  // test that the Addition was successfull
  public void testHPaymentTerm() {
    setUserContext("1000019");
    setAccess();
    final List<PaymentTerm> pts = getPaymentTerms();
    for (final PaymentTerm pt : pts) {
      // one pt has 2 lines, one has 1 line
      final int size = pt.getFinancialMgmtPaymentTermLineList().size();
      assertTrue(size == 1 || size == 2);
    }
  }

  // cleans up everything
  public void testZPaymentTerm() {
    setUserContext("1000000");
    final List<PaymentTerm> pts = getPaymentTerms();
    // financialmanagementpaymenttermline is not deletable, but as we are cleaning up
    // force delete by being the admin
    OBContext.getOBContext().setInAdministratorMode(true);
    for (final PaymentTerm pt : pts) {
      OBDal.getInstance().remove(pt);
    }
    OBDal.getInstance().commitAndClose();

    setUserContext("1000019");
    final List<PaymentTerm> pts2 = getPaymentTerms();
    // financialmanagementpaymenttermline is not deletable, but as we are cleaning up
    // force delete by being the admin
    OBContext.getOBContext().setInAdministratorMode(true);
    for (final PaymentTerm pt : pts2) {
      OBDal.getInstance().remove(pt);
    }
    OBDal.getInstance().commitAndClose();
  }

  private void createSavePaymentTerm() {
    setAccess();
    final List<PaymentTerm> result = new ArrayList<PaymentTerm>();
    for (int i = 0; i < NO_OF_PT; i++) {
      final PaymentTerm source = OBDal.getInstance().get(PaymentTerm.class, "1000000");
      final PaymentTerm pt = (PaymentTerm) DalUtil.copy(source);
      pt.setName("test " + i);
      pt.setOrganization(OBContext.getOBContext().getCurrentOrganization());

      // force new
      // now add a payment termline
      for (int j = 0; j < NO_OF_PT_LINE; j++) {
        final PaymentTermLine ptl = OBProvider.getInstance().get(PaymentTermLine.class);
        ptl.setExcludeTax(true);
        ptl.setLastDayCutoff(new Long(10));
        ptl.setMaturityDate1(new Long(5));
        ptl.setMaturityDate2(new Long(1));
        ptl.setMaturityDate3(new Long(1));
        ptl.setOffsetMonthDue(new Long(j));
        ptl.setLineNo((long) j);
        ptl.setOverduePaymentDayRule("1");
        ptl.setOverduePaymentDaysRule((long) 10);
        ptl.setNextBusinessDay(true);
        ptl.setRest(true);
        ptl.setPaymentTerms(pt);
        ptl.setPercentageDue(1.0f);
        pt.getFinancialMgmtPaymentTermLineList().add(ptl);
      }
      result.add(pt);
    }
    for (final PaymentTerm pt : result) {
      OBDal.getInstance().save(pt);
    }
  }

  private List<PaymentTerm> getPaymentTerms() {
    final OBCriteria<PaymentTerm> obc = OBDal.getInstance().createCriteria(PaymentTerm.class);
    obc.add(Expression.not(Expression.in("id", currentPaymentTerms)));
    return obc.list();
  }

  @SuppressWarnings("unchecked")
  public <T extends BaseOBObject> String getXML(List<?> pts) {
    final EntityXMLConverter exc = EntityXMLConverter.newInstance();
    exc.setOptionIncludeReferenced(true);
    exc.setOptionEmbedChildren(true);
    exc.setOptionIncludeChildren(true);
    exc.setAddSystemAttributes(false);
    return exc.toXML((List<BaseOBObject>) pts);
  }

  // set the access so that the test are not bothered by security checks
  // these are not tested here
  private void setAccess() {
    addReadWriteAccess(PaymentTerm.class);
    addReadWriteAccess(PaymentTermLine.class);
  }
}