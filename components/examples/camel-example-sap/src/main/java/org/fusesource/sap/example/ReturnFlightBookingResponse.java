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

import java.math.BigDecimal;

import org.apache.camel.Exchange;
import org.fusesource.camel.component.sap.SAPEndpoint;
import org.fusesource.camel.component.sap.model.rfc.Structure;
import org.fusesource.camel.component.sap.model.rfc.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReturnFlightBookingResponse {

	private static final Logger LOG = LoggerFactory.getLogger(ReturnFlightBookingResponse.class);
	
	public void createFlightBookingResponse(Exchange exchange) throws Exception {

		FlightConnectionInfo flightConnectionInfo = exchange.getIn().getHeader("flightConnectionInfo", FlightConnectionInfo.class);
		PassengerInfo passengerInfo = exchange.getIn().getHeader("passengerInfo", PassengerInfo.class);

		Structure flightTripCreateResponse = exchange.getIn().getBody(Structure.class);
		
		if (flightTripCreateResponse == null) {
			throw new Exception("No Flight Trip Create Response");
		}
		
		SAPEndpoint endpoint = exchange.getContext().getEndpoint("sap:server:nplServer:BOOK_FLIGHT", SAPEndpoint.class);
		Structure response = endpoint.getResponse();
		
		// Trip Number
		String tripNumber = flightTripCreateResponse.get("TRIPNUMBER", String.class);
		if (tripNumber != null) {
			response.put("TRIPNUMBER", tripNumber);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Added TRIPNUMBER = '{}' to request", tripNumber);
			}
		} else {
			throw new Exception("No Flight Booking Trip Number");
		}

		// Pricing Info
		Structure ticketPrice = flightTripCreateResponse.get("TICKET_PRICE", Structure.class);
		if (ticketPrice != null) {
			// Ticket Price
			BigDecimal tripPrice = ticketPrice.get("TRIPPRICE", BigDecimal.class);
			response.put("TICKET_PRICE", tripPrice);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Added TICKET_PRICE = '{}' to request", tripPrice);
			}
			// Ticket Tax
			BigDecimal tripTax = ticketPrice.get("TRIPTAX", BigDecimal.class);
			response.put("TICKET_TAX", tripTax);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Added TICKET_TAX = '{}' to request", tripTax);
			}
			// Currency
			String currency = ticketPrice.get("CURR", String.class); 
			response.put("CURRENCY", currency);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Added CURRENCY = '{}' to request", currency);
			}
		} else {
			throw new Exception("No Flight Booking Ticket Price");
		}

		// Passenger Info
		// 	Form
		response.put("PASSFORM", passengerInfo.getFormOfAddress());
		//  Name
		response.put("PASSNAME", passengerInfo.getName());
		//  DOB
		response.put("PASSBIRTH", passengerInfo.getDateOfBirth());
		
		// Flight Info
		Structure flightInfo = (Structure) response.get("FLTINFO");
		//  Flight Time
		flightInfo.put("FLIGHTTIME", flightConnectionInfo.getFlightTime());
		//  Departure City
		flightInfo.put("CITYFROM", flightConnectionInfo.getDepartureCity());
		//  Departure Date
		flightInfo.put("DEPDATE", flightConnectionInfo.getDepartureDate());
		//  Departure Time
		flightInfo.put("DEPTIME", flightConnectionInfo.getDepartureTime());
		//  Arrival City
		flightInfo.put("CITYTO", flightConnectionInfo.getArrivalCity());
		//  Arrival Date
		flightInfo.put("ARRDATE", flightConnectionInfo.getArrivalDate());
		//  Arrival Time
		flightInfo.put("ARRTIME", flightConnectionInfo.getArrivalTime());
		

		@SuppressWarnings("unchecked")
		Table<Structure> connectionInfo = response.get("CONNINFO", Table.class);
		for (FlightHop flightHop: flightConnectionInfo.getFlightHopList()) {
			// Connection Info
			Structure connection = connectionInfo.add();
			//  Connection ID
			connection.put("CONNID", flightHop.getHopNumber());
			//  Airline
			connection.put("AIRLINE", flightHop.getAirlineName());
			//  Plane Type
			connection.put("PLANETYPE", flightHop.getAircraftType());
			//  Departure City
			connection.put("CITYFROM", flightHop.getDepatureCity());
			//  Departure Date
			connection.put("DEPDATE", flightHop.getDepatureDate());
			//  Departure Time
			connection.put("DEPTIME", flightHop.getDepatureTime());
			//  Arrival City
			connection.put("CITYTO", flightHop.getArrivalCity());
			//  Arrival Date
			connection.put("ARRDATE", flightHop.getArrivalDate());
			//  Arrival Time
			connection.put("ARRTIME", flightHop.getArrivalTime());
		}
		
		exchange.getIn().setBody(response);

	}
}
