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

package org.openbravo.erpCommon.ws.services;

public class BusinessPartner {
	private int id;
	private int clientId;
	private String name;
	private String searchKey;	
	private String description;
	private Boolean complete;
	private Boolean customer;
	private Boolean vendor;
	private Location[] locations;
	private Contact[] contacts;
	
	public BusinessPartner() {}
	
	public int getId() {
		return id;
	}
	
	public void setId(int value) {
		id = value;
	}
	
	public int getClientId() {
		return clientId;
	}
	
	public void setClientId(int value) {
		clientId = value;
	}
	
	public String getName() {
		return name;
	}
	
	public void setDescription(String value) {
		description = value;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setName(String value) {
		name = value;
	}
	
	public String getSearchKey() {
		return searchKey;
	}
	
	public void setSearchKey(String value) {
		searchKey = value;
	}
	
	public Location[] getLocations() {
		return locations;
	} 
	
	public void setLocations(Location[] value) {
		locations = value;
	}
	
	public Contact[] getContacts() {
		return contacts;
	}
	
	public void setContacts(Contact[] value) {
		contacts = value;
	}
	
	public Boolean isComplete() {
		return complete;
	}
	
	public void setComplete(Boolean value) {
		complete = value;
	}
	
	public Boolean isCustomer() {
		return customer;
	}
	
	public void setCustomer(Boolean value) {
		customer = value;
	}
	
	public Boolean isVendor() {
		return vendor;
	}
	
	public void setVendor(Boolean value) {
		vendor = value;
	}	
}
