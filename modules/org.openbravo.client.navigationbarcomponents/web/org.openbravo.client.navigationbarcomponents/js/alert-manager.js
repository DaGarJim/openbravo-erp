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
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2010 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
*/

// = Alert Manager =
//
// The Alert manager calls the server at preset intervals to obtain the current list 
// of alerts and to update the server side session administration.
//
(function(OB, isc) {

  if (!OB || !isc) {
    throw {name: "ReferenceError",
        message: "openbravo and isc objects are required"};
  }

  // cache object references locally
  var ISC = isc,
     alertmgr; // Local reference to RemoveCallManager instance

  function AlertManager() {}

  AlertManager.prototype = {
  
      listeners: [],
      
      delay: 50000,

      // ** {{{ AlertManager.addListener(listener) }}} **
      //
      // Register a new listener which will be called when a new alert result is received
      // from the server.
      //
      // Parameters:
      // * {{{listener}}}: a function which is called when a new alert result is received.
      addListener: function(/*function*/ listener) {
        this.listeners[this.listeners.length] = listener;
      },
  
      _notify : function(rpcResponse, data, rpcRequest) {
        for (var i = 0; i < OB.AlertManager.listeners.length; i++) {
          OB.AlertManager.listeners[i](rpcResponse, data, rpcRequest);
        }
        isc.Timer.setTimeout(OB.AlertManager.call, OB.AlertManager.delay);
      },
  
    call: function () {
        OB.RemoteCallManager.call('org.openbravo.client.application.AlertActionHandler', {}, {}, OB.AlertManager._notify);
      }
  };

  // Initialize AlertManager object and let it call the system every so-many secs.
  alertmgr = OB.AlertManager = new AlertManager();  
  
  // call it ones to update the pings and start the timer
  OB.AlertManager.call();
})(OB, isc);


isc.ClassFactory.defineClass("OBAlertIcon", isc.ImgButton);

// = OBAlertIcon =
// The OBAlertIcon widget creates a button which notifies the user of any alerts
// present in the system. When an alert is found it will change appearance and prompt.
// The OBAlertIcon extends from the Smartclient Button.
isc.OBAlertIcon.addProperties({
  initWidget: function() {
    var instance = this;
    var listener = function(rpcResponse, data, rpcRequest) {
      if (data.cnt > 0) {
        instance.setTitle(OB.I18N.getLabel(instance.alertLabel, [data.cnt]));
        instance.setIcon(instance.alertIcon);
      } else {
        instance.setTitle(OB.I18N.getLabel(instance.alertLabel, [0]));
        instance.setIcon({}); 
      }
      instance.markForRedraw();
    };
    
    this.Super("initWidget", arguments);
    
    // call it to update the number of alerts directly after login
    OB.AlertManager.addListener(listener);
  },
  alertIcon: {src: "[SKINIMG]../../org.openbravo.client.navigationbarcomponents/images/ico-red-triangle.gif"},
  alertLabel: 'UINAVBA_Alerts',
  iconWidth: 11,
  iconHeight: 13,
  baseStyle: 'navBarButton',
  showTitle: true,
  iconOrientation: "left",
  src: "",
  overflow: "visible",
  title: OB.I18N.getLabel('UINAVBA_Alerts', [0]),
  click: function() {
    var viewDefinition = {obManualURL: "/ad_forms/AlertManagement.html", command: "DEFAULT", formId: '800016', tabTitle: OB.I18N.getLabel('UINAVBA_AlertManagement')};
    OB.Layout.ViewManager.openView("ClassicOBWindow", viewDefinition);
  }

});  
