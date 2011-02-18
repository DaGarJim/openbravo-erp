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

// = Property Store =
//
// The Property Store maintains properties. A property can be anything from the width of a column to 
// the last menu selections of a user. If a component sets a property then the property is also set 
// on the server. If a component requests a certain property then the local cache (OB.Properties) is checked. 
// If no value can be found there then an undefined value is returned. 
//
(function(OB, isc) {

  if (!OB || !isc) {
    throw {name: "ReferenceError",
        message: "openbravo and isc objects are required"};
  }

  // cache object references locally
  var ISC = isc,
     pstore; // Local reference to RemoveCallManager instance

  function PropertyStore() {}

  PropertyStore.prototype = {
      
// ** {{{ PropertyStore.get(propertyName) }}} **
//
// Retrieves the property from the local cache. If not found then null 
// is returned.
//
// Parameters:
// * {{{propertyName}}}: the name of the property
//
    get: function (/*String*/ propertyName) {
      if (!OB.Properties[propertyName]) {
        return null;
      }
      return OB.Properties[propertyName];
    },
    
 // ** {{{ PropertyStore.set(propertyName, value) }}} **
 //
 // Sets the property in the local cache. Also performs a server call to persist the 
 // property in the database.
 //
 // Parameters:
 // * {{{propertyName}}}: the name of the property
 // * {{{value}}}: the value of the property
 //
     set: function (/*String*/ propertyName, /*Object*/ value) {
       // set it locally
       OB.Properties[propertyName] = value;
       
       // and set it in the server also
       var callback = new function() {};       
       OB.RemoteCallManager.call('org.openbravo.client.application.StorePropertyActionHandler', value, {property: propertyName}, callback);
     },        
  };

  // Initialize PropertyStore object
  pstore = OB.PropertyStore = new PropertyStore();
})(OB, isc);