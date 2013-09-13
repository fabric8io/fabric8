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

import java.util.Date;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReturnPassengerInfo {

	private static final Logger LOG = LoggerFactory.getLogger(ReturnPassengerInfo.class);

	
	public void createPassengerInfo(Exchange exchange) {
		BookFlightRequest bookFlightRequest = exchange.getIn().getBody(BookFlightRequest.class);
		
		PassengerInfo passengerInfo = new PassengerInfo();
		
		String passengerFormOfAddress = bookFlightRequest.getPassengerFormOfAddress();
		if (passengerFormOfAddress != null) {
			passengerInfo.setFormOfAddress(passengerFormOfAddress);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set passenger form of address = '{}' in passenger info", passengerFormOfAddress);
			}
		}
		
		String passengerName = bookFlightRequest.getPassengerName();
		if (passengerName != null) {
			passengerInfo.setName(passengerName);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set passenger name = '{}' in passenger info", passengerName);
			}
		}
		
		Date passengerDateOfBirth = bookFlightRequest.getPassengerDateOfBirth();
		if (passengerDateOfBirth != null) {
			passengerInfo.setDateOfBirth(passengerDateOfBirth);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Set passenger date of birth = '{}' in passenger info", passengerDateOfBirth);
			}
		}
		
		exchange.getIn().setHeader("passengerInfo", passengerInfo);
	}

}
