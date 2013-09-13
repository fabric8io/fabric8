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
import org.fusesource.camel.component.sap.SAPEndpoint;
import org.fusesource.camel.component.sap.model.rfc.Structure;
import org.fusesource.camel.component.sap.model.rfc.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateFlightTripRequest {

	private static final Logger LOG = LoggerFactory.getLogger(CreateFlightTripRequest.class);

	private String destinationName;

	public String getDestinationName() {
		return destinationName;
	}

	public void setDestinationName(String destinationName) {
		this.destinationName = destinationName;
	}

	public void create(Exchange exchange) throws Exception {
		FlightConnectionInfo flightConnectionInfo = exchange.getIn().getHeader("flightConnectionInfo", FlightConnectionInfo.class);
		FlightCustomerInfo flightCustomerInfo = exchange.getIn().getHeader("flightCustomerInfo", FlightCustomerInfo.class);
		PassengerInfo passengerInfo = (PassengerInfo) exchange.getIn().getHeader("passengerInfo");
		
		SAPEndpoint endpoint = exchange.getContext().getEndpoint("sap:destination:nplDest:BAPI_FLTRIP_CREATE", SAPEndpoint.class);
		Structure request = endpoint.getRequest();
		
		//
		// Add Flight Trip Data
		//
		Structure flightTripData = request.get("FLIGHT_TRIP_DATA", Structure.class);
		
		String travelAgencyNumber = flightConnectionInfo.getTravelAgencyNumber();
		if (travelAgencyNumber != null && travelAgencyNumber.length() != 0) {
			flightTripData.put("AGENCYNUM", travelAgencyNumber);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Added AGENCYNUM = '{}' to FLIGHT_TRIP_DATA", travelAgencyNumber);
			}
			
		}
		
		String flightCustomerNumber = flightCustomerInfo.getCustomerNumber();
		if (flightCustomerNumber != null && flightCustomerNumber.length() != 0) {
			flightTripData.put("CUSTOMERID", flightCustomerNumber);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Added CUSTOMERID = '{}' to FLIGHT_TRIP_DATA", flightCustomerNumber);
			}
			
		}
		
		String flightConnectionNumber = flightConnectionInfo.getFlightConnectionNumber();
		if (flightConnectionNumber != null && flightConnectionNumber.length() != 0) {
			flightTripData.put("FLCONN1", flightConnectionNumber);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Added FLCONN1 = '{}' to FLIGHT_TRIP_DATA", flightConnectionNumber);
			}
			
		}
		
		Date flightConnectionDepartureData = flightConnectionInfo.getDepartureDate();
		if (flightConnectionDepartureData != null) {
			flightTripData.put("FLDATE1", flightConnectionDepartureData);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Added FLDATE1 = '{}' to FLIGHT_TRIP_DATA", flightConnectionDepartureData);
			}
			
		}
		
		// C : Business Class
		// Y : Economy Class
		// F : First Class
		String flightConnectionClass = "Y";
		if (flightConnectionClass != null && flightConnectionClass.length() != 0) {
			flightTripData.put("CLASS", flightConnectionClass);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Added CLASS = '{}' to FLIGHT_TRIP_DATA", flightConnectionClass);
			}
			
		}
		
		
		//
		// Add Passenger List
		//
		@SuppressWarnings("unchecked")
		Table<Structure> passengerList = request.get("PASSENGER_LIST", Table.class);
		Structure passengerListEntry = passengerList.add();
		
		String passengerFormOfAddress = passengerInfo.getFormOfAddress();
		if (passengerFormOfAddress != null && passengerFormOfAddress.length() != 0) {
			passengerListEntry.put("PASSFORM", passengerFormOfAddress);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Added PASSFORM = '{}' to PASSENGER_LIST", passengerFormOfAddress);
			}
			
		}
		
		String passengerName = passengerInfo.getName();
		if (passengerName != null && passengerName.length() != 0) {
			passengerListEntry.put("PASSNAME", passengerName);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Added PASSNAME = '{}' to PASSENGER_LIST", passengerName);
			}
			
		}
		
		Date passengerDateOfBirth = passengerInfo.getDateOfBirth();
		if (passengerDateOfBirth != null) {
			passengerListEntry.put("PASSBIRTH", passengerDateOfBirth);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Added PASSBIRTH = '{}' to PASSENGER_LIST", passengerDateOfBirth);
			}
			
		}
		

		exchange.getIn().setBody(request);
		
	}

}
