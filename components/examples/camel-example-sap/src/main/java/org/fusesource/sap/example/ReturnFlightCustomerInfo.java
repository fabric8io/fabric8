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
package org.fusesource.sap.example;

import org.apache.camel.Exchange;
import org.fusesource.camel.component.sap.model.rfc.Structure;
import org.fusesource.camel.component.sap.model.rfc.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReturnFlightCustomerInfo {
	
	private static final Logger LOG = LoggerFactory.getLogger(ReturnFlightCustomerInfo.class);

	public void createFlightCustomerInfo(Exchange exchange) throws Exception {
		Structure flightCustomerGetListResponse = exchange.getIn().getBody(Structure.class);
		
		if (flightCustomerGetListResponse == null) {
			throw new Exception("No Flight Customer Get List Response");
		}
		
		@SuppressWarnings("unchecked")
		Table<? extends Structure> customerList = flightCustomerGetListResponse.get("CUSTOMER_LIST", Table.class);
		
		if (customerList == null || customerList.size() == 0) {
			throw new Exception("No Customer Info.");
		}
		
		Structure customer = customerList.get(0);
		
		FlightCustomerInfo flightCustomerInfo = new FlightCustomerInfo();
		
		String customerId = customer.get("CUSTOMERID", String.class);
		if (customerId != null) {
			flightCustomerInfo.setCustomerNumber(customerId);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set customer number = '{}' in flight customer info", customerId);
			}
		}
		
		String customerName = customer.get("CUSTNAME", String.class);
		if (customerName != null) {
			flightCustomerInfo.setName(customerName);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set customer name = '{}' in flight customer info", customerName);
			}
		}
		
		String formOfAddress = customer.get("FORM", String.class);
		if (formOfAddress != null) {
			flightCustomerInfo.setFormOfAddress(formOfAddress);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set form of address = '{}' in flight customer info", formOfAddress);
			}
		}
		
		String street = customer.get("STREET", String.class);
		if (street != null) {
			flightCustomerInfo.setStreet(street);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set street = '{}' in flight customer info", street);
			}
		}
		
		String poBox = customer.get("POBOX", String.class);
		if (poBox != null) {
			flightCustomerInfo.setPoBox(poBox);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set PO box = '{}' in flight customer info", poBox);
			}
		}
		
		String postalCode = customer.get("POSTCODE", String.class);
		if (postalCode != null) {
			flightCustomerInfo.setPostalCode(postalCode);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set postal code = '{}' in flight customer info", postalCode);
			}
		}
		
		String city = customer.get("CITY", String.class);
		if (city != null) {
			flightCustomerInfo.setCity(city);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set city = '{}' in flight customer info", city);
			}
		}
		
		String country = customer.get("COUNTR", String.class);
		if (country != null) {
			flightCustomerInfo.setCountry(country);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set country = '{}' in flight customer info", country);
			}
		}
		
		String countryIso = customer.get("COUNTR_ISO", String.class);
		if (countryIso != null) {
			flightCustomerInfo.setCountryIso(countryIso);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set iso country code = '{}' in flight customer info", countryIso);
			}
		}
		
		String region = customer.get("REGION", String.class);
		if (region != null) {
			flightCustomerInfo.setRegion(region);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set region = '{}' in flight customer info", region);
			}
		}
		
		String phone = customer.get("PHONE", String.class);
		if (phone != null) {
			flightCustomerInfo.setPhone(phone);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set phone = '{}' in flight customer info", phone);
			}
		}
		
		String email = customer.get("EMAIL", String.class);
		if (email != null) {
			flightCustomerInfo.setEmail(email);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set email = '{}' in flight customer info", email);
			}
		}
		
		exchange.getIn().setHeader("flightCustomerInfo", flightCustomerInfo);
		
	}

}
