/*
 * Copyright 2010 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.fabric8.activemq.facade;

import java.util.Collection;
import java.util.Collections;

/**
 * Query for a single connection.
 */
public class ConnectionQuery {

	private final BrokerFacade mBrokerFacade;
	private String mConnectionID;

	public ConnectionQuery(BrokerFacade brokerFacade) {
		mBrokerFacade = brokerFacade;
	}

	public void destroy() {
		// empty
	}

	public void setConnectionID(String connectionID) {
		mConnectionID = connectionID;
	}

	public String getConnectionID() {
		return mConnectionID;
	}

	public ConnectionViewFacade getConnection() throws Exception {
		String connectionID = getConnectionID();
		if (connectionID == null)
			return null;
		return mBrokerFacade.getConnection(connectionID);
	}

	public Collection<SubscriptionViewFacade> getConsumers() throws Exception {
		String connectionID = getConnectionID();
		if (connectionID == null)
			return Collections.emptyList();
		return mBrokerFacade.getConsumersOnConnection(connectionID);
	}

}
