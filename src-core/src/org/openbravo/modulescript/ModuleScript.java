/*
 ************************************************************************************
 * Copyright (C) 2010-2016 Openbravo S.L.U.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to  in writing,  software  distributed
 * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
 * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */
package org.openbravo.modulescript;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.openbravo.base.ExecutionLimitBaseProcess;
import org.openbravo.base.ExecutionLimits;
import org.openbravo.database.ConnectionProvider;

/**
 * Classes extending ModuleScript can be included in Openbravo Core or a module and will be
 * automatically executed when the system is rebuilt (technically in: update.database and
 * update.database.mod)
 * 
 */
public abstract class ModuleScript extends ExecutionLimitBaseProcess {

  private static final Logger log4j = Logger.getLogger(ModuleScript.class);
  private static final String MODULE_SCRIPT = "ModuleScript";

  /**
   * This method must be implemented by the ModuleScripts, and is used to define the actions that
   * the script itself will take. This method will be automatically called by the
   * ModuleScriptHandler when the update.database or the update.database.mod tasks are being
   * executed
   */
  public abstract void execute();

  /**
   * This method prints some log information before calling the execute() method
   */
  @Override
  protected void doExecute() {
    log4j.info("Executing moduleScript: " + this.getClass().getName());
    execute();
  }

  /**
   * This method checks whether the ModuleScript can be executed before invoke the doExecute()
   * method
   * 
   * @param modulesVersionMap
   *          A data structure that contains module versions mapped by module id
   */
  public final void preExecute(Map<String, OpenbravoVersion> modulesVersionMap) {
    super.preExecute(modulesVersionMap);
  }

  /**
   * This method returns the name of the class.
   */
  protected String getTypeName() {
    return MODULE_SCRIPT;
  }

  /**
   * This method can be overridden by the ModuleScript subclasses, to specify the module and the
   * limit versions to define whether the ModuleScript should be executed or not.
   * 
   * @return a ModuleScriptExecutionLimits object which contains the dependent module id and the
   *         first and last versions of the module that define the execution logic.
   */
  protected ModuleScriptExecutionLimits getModuleScriptExecutionLimits() {
    return null;
  }

  @Override
  protected ExecutionLimits getExecutionLimits() {
    return getModuleScriptExecutionLimits();
  }

  /**
   * This method can be overridden by the ModuleScript subclasses, to specify if the ModuleScript
   * should be executed when installing the dependent module.
   * 
   * @return a boolean that indicates if the ModuleScript should be executed when installing the
   *         dependent module.
   */
  protected boolean executeOnInstall() {
    return super.executeOnInstall();
  }

  /**
   * This method returns a connection provider, which can be used to execute statements in the
   * database
   * 
   * @return a ConnectionProvider
   */
  protected ConnectionProvider getConnectionProvider() {
    return super.getConnectionProvider();
  }

  protected File getPropertiesFile() {
    return super.getPropertiesFile();
  }

  protected void handleError(Throwable t) {
    log4j.error(
        "Error executing moduleScript " + this.getClass().getName() + ": " + t.getMessage(), t);
    throw new BuildException("Execution of moduleScript " + this.getClass().getName() + "failed.");
  }
}