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
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author William Collins <punkhornsw@gmail.com>
 *
 */
public class AggregateFlightBookingStrategy implements AggregationStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(AggregateFlightBookingStrategy.class);

	/**
	 * Merges the message headers of sub-routes.
	 * <p>{@inheritDoc}
	 */
	@Override
	public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        String to = newExchange.getProperty(Exchange.TO_ENDPOINT, String.class);
        if (LOG.isDebugEnabled()) {
        	LOG.debug("To endpoint = {}", to);
        }
        if (to.contains("FlightConnectionInfo")) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Adding Flight Connection Info to exchange.");
			}
        	return mergeHeaderIntoOldExchange("flightConnectionInfo", oldExchange, newExchange);
        } else if (to.contains("FlightCustomerInfo")){
			if (LOG.isDebugEnabled()) {
				LOG.debug("Adding Flight Customer Info to exchange.");
			}
        	return mergeHeaderIntoOldExchange("flightCustomerInfo", oldExchange, newExchange);
        } else {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Adding Passenger Info to exchange.");
			}
        	return mergeHeaderIntoOldExchange("passengerInfo", oldExchange, newExchange);
        }
	}
	
	/**
	 * Merges the message header of input message in new exchange with message
	 * headers of input message in old exchange.
	 * 
	 * @param messageHeader
	 *            - the name of message header to merge.
	 * @param oldExchange
	 *            - the old exchange.
	 * @param newExchange
	 *            - the new exchange.
	 * @return The merged exchange.
	 */
    public Exchange mergeHeaderIntoOldExchange(String messageHeader, Exchange oldExchange, Exchange newExchange) {

        Exchange answer = oldExchange == null ? newExchange : oldExchange;
        
        answer.getIn().setHeader(messageHeader, newExchange.getIn().getHeader(messageHeader));

        return answer;
    }

}
