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


package org.openbravo.task;

import java.io.File;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.openbravo.utils.ServerConnection;

public class CheckTomcatAntExecution extends Task{
    static Logger log4j = Logger.getLogger(CheckTomcatAntExecution.class);
    
    @Override
    public void execute() throws BuildException {
        final File f = new File("src-diagnostics/build.xml");
        final String fileName = f.getAbsolutePath();
        System.out.println(fileName);
        log4j.info("Checking tomcat executing ant tasks...");
        String result = new ServerConnection().getCheck("ant", "&file="+fileName+"&task=test1");
        if (result.equals("OK")) 
            log4j.info("Possible to execute simple tasks. OK");
        else 
            throw new BuildException("Tomcat cannot execute simple ant tasks: "+result);
        
        result = new ServerConnection().getCheck("ant", "&file="+fileName+"&task=test2");
        if (result.equals("OK")) 
            log4j.info("Possible to execute ant javac task. OK");
        else 
            throw new BuildException("Tomcat cannot execute ant javac task: "+result);
    }
    
    

}
