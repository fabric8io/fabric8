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

import java.util.Date;

import org.apache.camel.Exchange;
import org.fusesource.sap.example.bean.PassengerInfo;
import org.fusesource.sap.example.jaxb.BookFlightRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processor that builds Passenger Info object.
 * 
 * @author William Collins <punkhornsw@gmail.com>
 *
 */
public class ReturnPassengerInfo {

	private static final Logger LOG = LoggerFactory.getLogger(ReturnPassengerInfo.class);

	
	/**
	 * Builds Passenger Info bean from BOOK_FLIGHT request in exchange message
	 * body and adds to exchange message's header.
	 * 
	 * @param exchange
	 * @throws Exception
	 */
	public void createPassengerInfo(Exchange exchange) {
		
		// Retrieve SAP request object from body of exchange message.
		BookFlightRequest bookFlightRequest = exchange.getIn().getBody(BookFlightRequest.class);
		
		// Create passenger info bean.
		PassengerInfo passengerInfo = new PassengerInfo();
		
		// Add passenger form of address to passenger info if set.
		String passengerFormOfAddress = bookFlightRequest.getPassengerFormOfAddress();
		if (passengerFormOfAddress != null) {
			passengerInfo.setFormOfAddress(passengerFormOfAddress);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set passenger form of address = '{}' in passenger info", passengerFormOfAddress);
			}
		}
		
		// Add passenger name to passenger info if set.
		String passengerName = bookFlightRequest.getPassengerName();
		if (passengerName != null) {
			passengerInfo.setName(passengerName);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set passenger name = '{}' in passenger info", passengerName);
			}
		}
		
		// Add passenger date of birth to passenger info if set.
		Date passengerDateOfBirth = bookFlightRequest.getPassengerDateOfBirth();
		if (passengerDateOfBirth != null) {
			passengerInfo.setDateOfBirth(passengerDateOfBirth);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set passenger date of birth = '{}' in passenger info", passengerDateOfBirth);
			}
		}
		
		// Put passenger info bean into header of exchange message. 
		exchange.getIn().setHeader("passengerInfo", passengerInfo);
	}

}
