/**
 * Copyright 2013 Red Hat, Inc.
 * 
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 */
package org.fusesource.sap.example.processor;

import org.apache.camel.Exchange;
import org.fusesource.camel.component.sap.model.rfc.Structure;
import org.fusesource.camel.component.sap.model.rfc.Table;
import org.fusesource.sap.example.bean.FlightCustomerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processor that builds Flight Customer Info object.
 * 
 * @author William Collins <punkhornsw@gmail.com>
 *
 */
public class ReturnFlightCustomerInfo {
	
	private static final Logger LOG = LoggerFactory.getLogger(ReturnFlightCustomerInfo.class);

	/**
	 * Builds Flight Customer Info bean from BAPI_FLCUST_GETLIST response in
	 * exchange message body and adds to exchange message's header.
	 * 
	 * @param exchange
	 * @throws Exception
	 */
	public void createFlightCustomerInfo(Exchange exchange) throws Exception {
		
		// Retrieve SAP response object from body of exchange message.
		Structure flightCustomerGetListResponse = exchange.getIn().getBody(Structure.class);
		
		if (flightCustomerGetListResponse == null) {
			throw new Exception("No Flight Customer Get List Response");
		}
		
		// Check BAPI return parameter for errors 
		@SuppressWarnings("unchecked")
		Table<Structure> bapiReturn = flightCustomerGetListResponse.get("RETURN", Table.class);
		Structure bapiReturnEntry = bapiReturn.get(0);
		if (!bapiReturnEntry.get("TYPE", String.class).equals("S")) {
			String message = bapiReturnEntry.get("MESSAGE", String.class);
			throw new Exception("BAPI call failed: " + message);
		}
		
		// Get customer list table from response object.
		@SuppressWarnings("unchecked")
		Table<? extends Structure> customerList = flightCustomerGetListResponse.get("CUSTOMER_LIST", Table.class);
		
		if (customerList == null || customerList.size() == 0) {
			throw new Exception("No Customer Info.");
		}
		
		// Get Flight Customer data from first row of table.
		Structure customer = customerList.get(0);
		
		// Create bean to hold Flight Customer data.
		FlightCustomerInfo flightCustomerInfo = new FlightCustomerInfo();
		
		// Get customer id from Flight Customer data and add to bean.
		String customerId = customer.get("CUSTOMERID", String.class);
		if (customerId != null) {
			flightCustomerInfo.setCustomerNumber(customerId);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set customer number = '{}' in flight customer info", customerId);
			}
		}
		
		// Get customer name from Flight Customer data and add to bean.
		String customerName = customer.get("CUSTNAME", String.class);
		if (customerName != null) {
			flightCustomerInfo.setName(customerName);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set customer name = '{}' in flight customer info", customerName);
			}
		}
		
		// Get customer form of address from Flight Customer data and add to bean.
		String formOfAddress = customer.get("FORM", String.class);
		if (formOfAddress != null) {
			flightCustomerInfo.setFormOfAddress(formOfAddress);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set form of address = '{}' in flight customer info", formOfAddress);
			}
		}
		
		// Get customer street name from Flight Customer data and add to bean.
		String street = customer.get("STREET", String.class);
		if (street != null) {
			flightCustomerInfo.setStreet(street);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set street = '{}' in flight customer info", street);
			}
		}
		
		// Get customer PO box from Flight Customer data and add to bean.
		String poBox = customer.get("POBOX", String.class);
		if (poBox != null) {
			flightCustomerInfo.setPoBox(poBox);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set PO box = '{}' in flight customer info", poBox);
			}
		}
		
		// Get customer postal code from Flight Customer data and add to bean.
		String postalCode = customer.get("POSTCODE", String.class);
		if (postalCode != null) {
			flightCustomerInfo.setPostalCode(postalCode);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set postal code = '{}' in flight customer info", postalCode);
			}
		}
		
		// Get customer city name from Flight Customer data and add to bean.
		String city = customer.get("CITY", String.class);
		if (city != null) {
			flightCustomerInfo.setCity(city);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set city = '{}' in flight customer info", city);
			}
		}
		
		// Get customer country name from Flight Customer data and add to bean.
		String country = customer.get("COUNTR", String.class);
		if (country != null) {
			flightCustomerInfo.setCountry(country);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set country = '{}' in flight customer info", country);
			}
		}
		
		// Get customer country ISO code from Flight Customer data and add to bean.
		String countryIso = customer.get("COUNTR_ISO", String.class);
		if (countryIso != null) {
			flightCustomerInfo.setCountryIso(countryIso);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set iso country code = '{}' in flight customer info", countryIso);
			}
		}
		
		// Get customer region name from Flight Customer data and add to bean.
		String region = customer.get("REGION", String.class);
		if (region != null) {
			flightCustomerInfo.setRegion(region);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set region = '{}' in flight customer info", region);
			}
		}
		
		// Get customer phone number from Flight Customer data and add to bean.
		String phone = customer.get("PHONE", String.class);
		if (phone != null) {
			flightCustomerInfo.setPhone(phone);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set phone = '{}' in flight customer info", phone);
			}
		}
		
		// Get customer email from Flight Customer data and add to bean.
		String email = customer.get("EMAIL", String.class);
		if (email != null) {
			flightCustomerInfo.setEmail(email);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set email = '{}' in flight customer info", email);
			}
		}
		
		// Put flight customer info bean into header of exchange message.
		exchange.getIn().setHeader("flightCustomerInfo", flightCustomerInfo);
		
	}

}
