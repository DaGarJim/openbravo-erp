/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use. this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

// == OB.EventHandlerRegistry ==
// A registry which can be used to register actions for a certain
// tab and event type combination. Multiple actions can be registered
// for one tab.
isc.ClassFactory.defineClass('OBEventHandlerRegistry', isc.OBFunctionRegistry);

isc.OBEventHandlerRegistry.addProperties({

  actionTypes: ['PRESAVE', 'POSTSAVE'],

  isValidElement: function (actionType) {
    var findType;
    findType = function (type) {
      return type === actionType;
    };
    return this.actionTypes.find(findType);
  },

  hasAction: function (tabId, actionType) {
    return this.getEntries(tabId, actionType);
  },

  getSortDirection: function () {
    // actions ordered descending by sort property
    return false;
  },

  call: function (tabId, actionType, view, form, grid, extraParameters, callback) {
    var callResult, entries = this.getEntries(tabId, actionType),
        actions, i;

    if (callback && !isc.isA.Function(callback)) {
      return;
    }
    actions = isc.clone(entries) || [];
    if (callback) {
      actions.unshift(callback);
    }
    this.callbackExecutor(view, form, grid, actions);
  },

  callbackExecutor: function (view, form, grid, extraParameters, actions) {
    var func;
    func = actions.pop();
    if (func) {
      func(view, form, grid, extraParameters, actions);
    }
  }
});

OB.EventHandlerRegistry = isc.OBEventHandlerRegistry.create();