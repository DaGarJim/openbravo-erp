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

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Expression;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.utility.ReferenceDataStore;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.geography.Location;
import org.openbravo.service.db.DataImportService;
import org.openbravo.service.db.ImportResult;

/**
 * Test import of data for a business object (in this case {@link Warehouse} and its referenced
 * information. A warehouse refers to a {@link Location}, the export exports both the warehouses as
 * their locations. Then the tests check that both are inserted and then when the warehouses are
 * reimported that the locations previously imported are re-used. Note that this test also tests the
 * use of the {@link ReferenceDataStore} which is used to map id's used in the xml to the eventual
 * id of the imported object in the database.
 * 
 * @author mtaal
 */

public class EntityXMLImportTestReference extends XMLBaseTest {

  /**
   * Exports {@link Warehouse} objects from client/org 1000000 and imports them in 1000001. Also the
   * referenced {@link Location} objects are imported.
   */
  public void test1Warehouse() {
    cleanRefDataLoaded();
    setUserContext("1000000");
    addReadWriteAccess(Warehouse.class);
    final String xml = getXML(Warehouse.class);
    // insert in org 1000001
    setUserContext("1000019");
    final ImportResult ir = DataImportService.getInstance().importDataFromXML(
        OBDal.getInstance().get(Client.class, "1000001"),
        OBDal.getInstance().get(Organization.class, "1000001"), xml);
    if (ir.getException() != null) {
      ir.getException().printStackTrace(System.err);
      fail(ir.getException().getMessage());
    } else if (ir.getErrorMessages() != null) {
      fail(ir.getErrorMessages());
    } else {
      // 2 warehouses, 2 locations
      assertEquals(4, ir.getInsertedObjects().size());
      assertEquals(0, ir.getUpdatedObjects().size());
    }
    if (ir.hasErrorOccured()) {
      fail(ir.getErrorMessages());
    }
  }

  /**
   * Remove the imported {@link Warehouse} objects from the 1000001 client. The imported
   * {@link Location} objects should still remain.
   */
  public void test2Warehouse() {
    setUserContext("1000019");
    // a warehouse is not deletable, but as we are cleaning up, they should be
    // deleted, force this by being admin
    OBContext.getOBContext().setInAdministratorMode(true);
    removeAll(Warehouse.class, 2, Expression.ne("id", "1000002"));
  }

  /**
   * Repeat the action of importing the {@link Warehouse} objects from 1000000. Now the
   * {@link Location} objects are also exported but not imported as they already exist in 1000001.
   */
  public void test3Warehouse() {
    setUserContext("1000000");
    addReadWriteAccess(Warehouse.class);
    final String xml = getXML(Warehouse.class);
    setUserContext("1000019");
    final ImportResult ir = DataImportService.getInstance().importDataFromXML(
        OBDal.getInstance().get(Client.class, "1000001"),
        OBDal.getInstance().get(Organization.class, "1000001"), xml);
    if (ir.getException() != null) {
      ir.getException().printStackTrace(System.err);
      fail(ir.getException().getMessage());
    } else if (ir.getErrorMessages() != null) {
      fail(ir.getErrorMessages());
    }
    assertEquals(2, ir.getInsertedObjects().size());
    for (final BaseOBObject bob : ir.getInsertedObjects()) {
      assertTrue(bob instanceof Warehouse);
    }
    assertEquals(0, ir.getUpdatedObjects().size());
    if (ir.hasErrorOccured()) {
      fail(ir.getErrorMessages());
    }
  }

  /**
   * Clean up by removing both the imported {@link Warehouse} and {@link Location} objects from
   * 100001 client.
   */
  public void test4Warehouse() {
    setUserContext("1000019");
    // a warehouse is not deletable, but as we are cleaning up, they should be
    // deleted, force this by being admin
    OBContext.getOBContext().setInAdministratorMode(true);
    removeAll(Warehouse.class, 2, Expression.ne("id", "1000002"));
    removeAll(Location.class, 2, Expression.not(Expression.in("id", new String[] { "1000048",
        "1000049", "1000050" })));
  }

  private <T extends BaseOBObject> void removeAll(Class<T> clz, int expectCount, Criterion c) {
    final Criteria criteria = SessionHandler.getInstance().getSession().createCriteria(clz);
    if (c != null) {
      criteria.add(c);
    }
    criteria.add(Expression.eq("client.id", "1000001"));

    @SuppressWarnings("unchecked")
    final List<T> list = criteria.list();
    if (expectCount != -1) {
      assertEquals(expectCount, list.size());
    }
    for (final T t : list) {
      SessionHandler.getInstance().getSession().delete(t);
    }
  }
}