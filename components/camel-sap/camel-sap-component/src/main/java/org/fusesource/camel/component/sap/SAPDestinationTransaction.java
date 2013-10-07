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
package org.fusesource.camel.component.sap;

import org.apache.camel.Exchange;
import org.apache.camel.support.SynchronizationAdapter;
import org.fusesource.camel.component.sap.util.RfcUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.conn.jco.JCoDestination;

/**
 * A Camel synchronization object which commits or rolls back an SAP transaction
 * upon successful completion or failure of an exchange.
 * 
 * @author William Collins <punkhornsw@gmail.com>
 * 
 */
public class SAPDestinationTransaction extends SynchronizationAdapter {

	private static final Logger LOG = LoggerFactory
			.getLogger(SAPDestinationTransaction.class);

	private String destinationName;

	private JCoDestination destination;

	public SAPDestinationTransaction(String destinationName,
			JCoDestination destination) {
		this.destinationName = destinationName;
		this.destination = destination;
	}

	@Override
	public int hashCode() {
		return destinationName.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return destinationName.equals(obj);
	}

	public void begin() {
		// Begin stateful session with destination SAP instance
		RfcUtil.beginTransaction(destination);
		LOG.debug("Began SAP Transaction for destination '{}'", destinationName);
	}

	public void commit() throws Exception {
		// Invoke BAPI_TRANSACTION_COMMIT
		RfcUtil.commitTransaction(destination);
		LOG.debug("Committed SAP Transaction for destination '{}'",
				destinationName);
	}

	public void rollback() throws Exception {
		// Invoke BAPI_TRANSACTION_ROLLBACK
		RfcUtil.rollbackTransaction(destination);
		LOG.debug("Rolledback SAP Transaction for destination '{}'",
				destinationName);
	}

	@Override
	public void onComplete(Exchange exchange) {
		try {
			commit();
		} catch (Exception e) {
			LOG.warn(
					"Failed to commit SAP Transaction. This exception will be ignored.",
					e);
		}
	}

	@Override
	public void onFailure(Exchange exchange) {
		try {
			rollback();
		} catch (Exception e) {
			LOG.warn(
					"Failed to rollback SAP Transaction. This exception will be ignored.",
					e);
		}
	}
}
