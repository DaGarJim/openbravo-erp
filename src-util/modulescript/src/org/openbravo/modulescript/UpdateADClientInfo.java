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
 * All portions are Copyright (C) 2012 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.modulescript;

import org.openbravo.database.ConnectionProvider;
import javax.servlet.ServletException;

public class UpdateADClientInfo extends ModuleScript {

  //This module script has ben created due to issue 18407 and related to issue 19697
  @Override
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      UpdateADClientInfoData[] clientsID = UpdateADClientInfoData.selectClientsID(cp);
      // MC tree
      for (UpdateADClientInfoData clientID : clientsID) {
        UpdateADClientInfoData.update(cp,clientID.adClientId);
      }
      // Asset tree
      createTreeAndUpdateClientInfo(cp, "Asset", "AS", "AD_TREE_ASSET_ID");
    } catch (Exception e) {
      handleError(e);
    }
  }

    private void createTreeAndUpdateClientInfo(final ConnectionProvider cp, final String treeTypeName, final String treeTypeValue, final String columnName)
	throws ServletException {
       UpdateADClientInfoData[] clientsID = UpdateADClientInfoData.selectClientsMissingTree(cp, columnName);
      for (UpdateADClientInfoData clientID: clientsID) {
	final String treeId = UpdateADClientInfoData.getUUID(cp);
	final String nameAndDesc = clientID.clientname + " " + treeTypeName;
        UpdateADClientInfoData.createTree(cp, treeId, clientID.adClientId, nameAndDesc, treeTypeValue);	
	UpdateADClientInfoData.updateClientTree(cp, columnName, treeId, clientID.adClientId);
      }
    }
}