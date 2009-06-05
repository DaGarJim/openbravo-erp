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

package org.openbravo.erpCommon.utility;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.utility.Image;

/**
 * 
 * This utility class implements a servlet that shows an image stored in database
 * 
 */
public class ShowImage extends HttpSecureAppServlet {

  private static final long serialVersionUID = 1L;

  /**
   * Receiving an id parameter it looks in database for the image with that id and displais it
   */
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {

    VariablesSecureApp vars = new VariablesSecureApp(request);

    String id = vars.getStringParameter("id");

    Image img = OBDal.getInstance().get(Image.class, id);
    if (img != null) {
      byte[] imageBytes = img.getBindaryData();
      if (imageBytes != null) {
        response.setContentLength(imageBytes.length);
        OutputStream out = response.getOutputStream();
        out.write(imageBytes);
        out.close();
      }
    }

  }
}
