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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.fusesource.camel.component.sap.model.rfc.Structure;
import org.fusesource.camel.component.sap.model.rfc.Table;
import org.fusesource.sap.example.bean.FlightConnectionInfo;
import org.fusesource.sap.example.bean.FlightHop;
import org.fusesource.sap.example.bean.PassengerInfo;
import org.fusesource.sap.example.jaxb.BookFlightResponse;
import org.fusesource.sap.example.jaxb.ConnectionInfo;
import org.fusesource.sap.example.jaxb.ConnectionInfoTable;
import org.fusesource.sap.example.jaxb.FlightInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processor that builds Flight Trip Response bean.
 * 
 * @author William Collins <punkhornsw@gmail.com>
 *
 */
public class ReturnFlightTripResponse {

	private static final Logger LOG = LoggerFactory.getLogger(ReturnFlightTripResponse.class);
	
	/**
	 * Builds Flight Trip Response bean from BAPI_FLTRIP_CREATE response in
	 * exchange message body and flight connection info and passenger info beans
	 * in exchange message headers and adds to exchange message body. 
	 * 
	 * @param exchange
	 * @throws Exception
	 */
	public void createFlightBookingResponse(Exchange exchange) throws Exception {

		// Retrieve flight connection and passenger info from exchange message headers. 
		FlightConnectionInfo flightConnectionInfo = exchange.getIn().getHeader("flightConnectionInfo", FlightConnectionInfo.class);
		PassengerInfo passengerInfo = exchange.getIn().getHeader("passengerInfo", PassengerInfo.class);

		// Retrieve SAP response object from body of exchange message.
		Structure flightTripCreateResponse = exchange.getIn().getBody(Structure.class);
		
		if (flightTripCreateResponse == null) {
			throw new Exception("No Flight Trip Create Response");
		}
		
		// Check BAPI return parameter for errors 
		@SuppressWarnings("unchecked")
		Table<Structure> bapiReturn = flightTripCreateResponse.get("RETURN", Table.class);
		Structure bapiReturnEntry = bapiReturn.get(0);
		if (!bapiReturnEntry.get("TYPE", String.class).equals("S")) {
			String message = bapiReturnEntry.get("MESSAGE", String.class);
			throw new Exception("BAPI call failed: " + message);
		}
		
		// Create bean to hold Flight Booking data.
		BookFlightResponse response = new BookFlightResponse();
		
		// Trip Number
		String tripNumber = flightTripCreateResponse.get("TRIPNUMBER", String.class);
		if (tripNumber != null) {
			response.setTripNumber(tripNumber);
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
			response.setTicketPrice(tripPrice);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Added TICKET_PRICE = '{}' to request", tripPrice);
			}
			// Ticket Tax
			BigDecimal tripTax = ticketPrice.get("TRIPTAX", BigDecimal.class);
			response.setTicketTax(tripTax);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Added TICKET_TAX = '{}' to request", tripTax);
			}
			// Currency
			String currency = ticketPrice.get("CURR", String.class); 
			response.setCurrency(currency);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Added CURRENCY = '{}' to request", currency);
			}
		} else {
			throw new Exception("No Flight Booking Ticket Price");
		}

		// Passenger Info
		// 	Form
		response.setPassengerFormOfAddress(passengerInfo.getFormOfAddress());
		//  Name
		response.setPassengerName(passengerInfo.getName());
		//  DOB
		response.setPassengerDateOfBirth(passengerInfo.getDateOfBirth());
		
		// Flight Info
		FlightInfo flightInfo = new FlightInfo();
		//  Flight Time
		flightInfo.setFlightTime(flightConnectionInfo.getFlightTime());
		//  Departure City
		flightInfo.setCityFrom(flightConnectionInfo.getDepartureCity());
		//  Departure Date
		flightInfo.setDepartureDate(flightConnectionInfo.getDepartureDate());
		//  Departure Time
		flightInfo.setDepartureTime(flightConnectionInfo.getDepartureTime());
		//  Arrival City
		flightInfo.setCityTo(flightConnectionInfo.getArrivalCity());
		//  Arrival Date
		flightInfo.setArrivalDate(flightConnectionInfo.getArrivalDate());
		//  Arrival Time
		flightInfo.setArrivalTime(flightConnectionInfo.getArrivalTime());
		response.setFlightInfo(flightInfo);

		ConnectionInfoTable connectionInfoTable = new ConnectionInfoTable();
		List<ConnectionInfo> rows = new ArrayList<ConnectionInfo>();
		for (FlightHop flightHop: flightConnectionInfo.getFlightHopList()) {
			// Connection Info
			ConnectionInfo connection = new ConnectionInfo();
			//  Connection ID
			connection.setConnectionId(flightHop.getHopNumber());
			//  Airline
			connection.setAirline(flightHop.getAirlineName());
			//  Plane Type
			connection.setPlaneType(flightHop.getAircraftType());
			//  Departure City
			connection.setCityFrom(flightHop.getDepatureCity());
			//  Departure Date
			connection.setDepartureDate(flightHop.getDepatureDate());
			//  Departure Time
			connection.setDepartureTime(flightHop.getDepatureTime());
			//  Arrival City
			connection.setCityTo(flightHop.getArrivalCity());
			//  Arrival Date
			connection.setArrivalDate(flightHop.getArrivalDate());
			//  Arrival Time
			connection.setArrivalTime(flightHop.getArrivalTime());
			rows.add(connection);
		}
		connectionInfoTable.setRows(rows);
		response.setConnectionInfo(connectionInfoTable);
		
		exchange.getIn().setBody(response);

	}
}
