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

public class CreateFlightConnectionGetDetailRequest {
	
	private static final Logger LOG = LoggerFactory.getLogger(CreateFlightConnectionGetDetailRequest.class);
	
	public void create(Exchange exchange) throws Exception {
		Structure flightConnectionGetListResponse = exchange.getIn().getBody(Structure.class);
		
		if (flightConnectionGetListResponse == null) {
			throw new Exception("No Flight Connection Get List Response.");
		}
		
		@SuppressWarnings("unchecked")
		Table<? extends Structure> connectionList = flightConnectionGetListResponse.get("FLIGHT_CONNECTION_LIST", Table.class);
		if (connectionList == null || connectionList.size() == 0) {
			throw new Exception("No Flight Connections");
		}
		
		// Select first connection
		Structure connection = connectionList.get(0);

		SAPEndpoint endpoint = exchange.getContext().getEndpoint("sap:destination:nplDest:BAPI_FLCONN_GETDETAIL", SAPEndpoint.class);
		Structure request = endpoint.getRequest();
		
		String connectionNumber = connection.get("FLIGHTCONN", String.class);
		if (connectionNumber != null) {
			request.put("CONNECTIONNUMBER", connectionNumber);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Added CONNECTIONNUMBER = '{}' to request", connectionNumber);
			}
		} else {
			throw new Exception("No Flight Connection Number");
		}
		
		String travelAgencyNumber = connection.get("AGENCYNUM", String.class);
		if (travelAgencyNumber != null) {
			request.put("TRAVELAGENCYNUMBER", travelAgencyNumber);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Added TRAVELAGENCYNUMBER = '{}' to request", travelAgencyNumber);
			}
		} else {
			throw new Exception("No Agency Number");
		}
		
		Date flightDate = connection.get("FLIGHTDATE", Date.class);
		if (flightDate != null) {
			request.put("FLIGHTDATE", flightDate);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Added FLIGHTDATE = '{}' to request", flightDate);
			}
		} else {
			throw new Exception("No Flight Date");
		}
		
		request.put("NO_AVAILIBILITY", "");
		
		exchange.getIn().setBody(request);

	}

}
