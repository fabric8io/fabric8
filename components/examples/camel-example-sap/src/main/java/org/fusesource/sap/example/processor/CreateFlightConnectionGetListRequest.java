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
import org.fusesource.camel.component.sap.SAPEndpoint;
import org.fusesource.camel.component.sap.model.rfc.Structure;
import org.fusesource.camel.component.sap.model.rfc.Table;
import org.fusesource.camel.component.sap.util.RfcUtil;
import org.fusesource.sap.example.jaxb.BookFlightRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processor that builds SAP Request object for BAPI_FLCONN_GETLIST RFC call. 
 * 
 * @author William Collins <punkhornsw@gmail.com>
 *
 */
public class CreateFlightConnectionGetListRequest {

	private static final Logger LOG = LoggerFactory.getLogger(CreateFlightConnectionGetListRequest.class);

	/**
	 * Builds SAP Request Object for BAPI_FLCONN_GETLIST call using data from
	 * the BOOK_FLIGHT request.
	 * 
	 * @param exchange
	 * @throws Exception
	 */
	public void create(Exchange exchange) throws Exception {

		// Get BOOK_FLIGHT Request JAXB Bean object.
		BookFlightRequest bookFlightRequest = exchange.getIn().getBody(BookFlightRequest.class);

		// Create SAP Request object from target endpoint.
		SAPEndpoint endpoint = exchange.getContext().getEndpoint("sap:destination:nplDest:BAPI_FLCONN_GETLIST", SAPEndpoint.class);
		Structure request = endpoint.getRequest();

		// Add Travel Agency Number to request if set
		if (bookFlightRequest.getTravelAgencyNumber() != null && bookFlightRequest.getTravelAgencyNumber().length() > 0) {
			request.put("TRAVELAGENCY", bookFlightRequest.getTravelAgencyNumber());
			if (LOG.isDebugEnabled()) {
				LOG.debug("Added TRAVELAGENCY = '{}' to request", bookFlightRequest.getTravelAgencyNumber());
			}
		} else {
			throw new Exception("No Travel Agency Number");
		}

		// Add Flight Date to request if set
		if (bookFlightRequest.getFlightDate() != null) {
			@SuppressWarnings("unchecked")
			Table<Structure> table = request.get("DATE_RANGE", Table.class);
			Structure date_range = table.add();
			date_range.put("SIGN", "I");
			date_range.put("OPTION", "EQ");
			Date date = bookFlightRequest.getFlightDate();
			date_range.put("LOW", date);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Added DATE_RANGE = '{}' to request", RfcUtil.marshal(table));
			}
		} else {
			throw new Exception("No Flight Date");
		}

		// Add Start Destination if set
		if (bookFlightRequest.getStartAirportCode() != null && bookFlightRequest.getStartAirportCode().length() > 0) {
			Structure destination_from = request.get("DESTINATION_FROM", Structure.class);
			destination_from.put("AIRPORTID", bookFlightRequest.getStartAirportCode());
			if (LOG.isDebugEnabled()) {
				LOG.debug("Added DESTINATION_FROM = '{}' to request", RfcUtil.marshal(destination_from));
			}
		} else {
			throw new Exception("No Start Destination");
		}

		// Add End Destination if set
		if (bookFlightRequest.getEndAirportCode() != null && bookFlightRequest.getEndAirportCode().length() > 0) {
			Structure destination_to = request.get("DESTINATION_TO", Structure.class);
			destination_to.put("AIRPORTID", bookFlightRequest.getEndAirportCode());
			if (LOG.isDebugEnabled()) {
				LOG.debug("Added DESTINATION_TO = '{}' to request", RfcUtil.marshal(destination_to));
			}
		} else {
			throw new Exception("No End Destination");
		}

		// Put request object into body of exchange message.
		exchange.getIn().setBody(request);

	}

}
