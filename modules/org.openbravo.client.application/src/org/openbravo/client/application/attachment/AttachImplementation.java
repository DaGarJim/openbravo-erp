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
 * All portions are Copyright (C) 2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.application.attachment;

import java.io.File;
import java.util.Map;

import org.openbravo.base.exception.OBException;
import org.openbravo.model.ad.utility.Attachment;

/**
 * Public class to allow extend the functionality
 */

public abstract class AttachImplementation {

  /**
   * Abstract method to upload files
   * 
   * @param attachment
   *          The attachment created in c_file with empty metadata
   * @param strDataType
   *          DataType of the attachment
   * @param parameters
   *          A map with the metadata and its values to be updated in the corresponding file
   *          management system and in the attachment
   * @param file
   *          The file to be uploaded
   * @param strTab
   *          The tabID where the file is attached
   * @param parameterValues
   *          List of metadata saved in database
   * @throws OBException
   *           Thrown when any error occurs during the upload
   */
  public abstract void uploadFile(Attachment attachment, String strDataType,
      Map<String, Object> parameters, File file, String strTab) throws OBException;

  /**
   * Abstract method to download a single file
   * 
   * @param attachment
   *          The attachment that will be downloaded
   * @return The file being to download
   * @throws OBException
   *           Thrown when any error occurs during the download
   */
  public abstract File downloadFile(Attachment attachment) throws OBException;

  /**
   * Abstract method to delete a file
   * 
   * @param attachment
   *          The attachment that want to be removed
   * @throws OBException
   *           Thrown when any error occurs when deleting the file
   */
  public abstract void deleteFile(Attachment attachment) throws OBException;

  /**
   * Abstract method to update file's metadata
   * 
   * @param attachment
   *          The attachment to be modified
   * @param strTab
   *          The tabID where the file was attached
   * @param parameters
   *          The metadata to be modified
   * @param parameterValues
   *          List of metadata saved in database
   * @throws OBException
   *           Thrown when any error occurs when updating the file
   */
  public abstract void updateFile(Attachment attachment, String strTab,
      Map<String, Object> parameters) throws OBException;

  /**
   * This method is used to know whether the attach method is creating a temporary file in the temp
   * directory of Openbravo server when downloading a file. If it is true, the process will remove
   * the temporary file. If it s false, the process will not remove the file
   * 
   * @return true if the attachment method creates a temporary file in Openbravo server.
   */
  public abstract boolean isTempFile();

}