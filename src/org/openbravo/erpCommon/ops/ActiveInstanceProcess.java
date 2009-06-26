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
 * All portions are Copyright (C) 2009 Openbravo SL 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.ops;

import java.net.URL;
import java.net.URLEncoder;

import org.apache.log4j.Logger;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.HttpsUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.ad.system.System;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

public class ActiveInstanceProcess implements Process {

  private static final Logger log = Logger.getLogger(ActiveInstanceProcess.class);
  private static final String BUTLER_URL = "https://butler.openbravo.com:443/heartbeat-server/activate";
  private static final String CERT_ALIAS = "openbravo-butler";

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    String publicKey = (String) bundle.getParams().get("publicKey");
    String purpose = (String) bundle.getParams().get("purpose");
    String instanceNo = (String) bundle.getParams().get("instanceNo");
    OBError msg = new OBError();

    bundle.setResult(msg);

    String[] result = send(publicKey, purpose, instanceNo);

    if (result.length == 2 && result[0] != null && result[1] != null
        && result[0].equals("@Success@")) {
      // now we have the activation key, lets save it
      System sys = OBDal.getInstance().get(System.class, "0");
      sys.setActivationKey(result[1]);
      sys.setInstanceKey(publicKey);
      ActivationKey ak = new ActivationKey();
      if (ak.isActive()) {
        msg.setType("Success");
        msg.setMessage(result[0]);
      } else {
        msg.setType("Error");
        msg.setMessage(ak.getErrorMessage());
      }

    } else {
      // If there is error do not save keys, thus we maitain previous ones in case they were valid
      msg.setType("Error");
      msg.setMessage(result[0]);
    }

  }

  /**
   * Sends the request for the activation key.
   * 
   * @param publickey
   *          Instance's public key
   * @param purpose
   *          Instance's purpose
   * @param instanceNo
   *          current instance number (for reactivation purposes)
   * @return returns a String[] with 2 elements, the first one in the message (@Success@ in case of
   *         success) and the second one the activation key
   * @throws Exception
   */
  private String[] send(String publickey, String purpose, String instanceNo) throws Exception {
    log.debug("Sending request");
    String content = "publickey=" + URLEncoder.encode(publickey, "utf-8");
    content += "&purpose=" + purpose;
    if (instanceNo != null && !instanceNo.equals(""))
      content += "&instanceNo=" + instanceNo;

    URL url = new URL(BUTLER_URL);
    String result = HttpsUtils.sendSecure(url, content, CERT_ALIAS, "changeit");
    java.lang.System.out.println(result);

    return result.split("\n");

  }
}
