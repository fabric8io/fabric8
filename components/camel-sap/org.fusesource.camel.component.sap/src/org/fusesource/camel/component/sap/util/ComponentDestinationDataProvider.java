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
package org.fusesource.camel.component.sap.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.fusesource.camel.component.sap.model.rfc.DestinationData;
import org.fusesource.camel.component.sap.model.rfc.DestinationDataStore;
import org.fusesource.camel.component.sap.model.rfc.RfcPackage;
import org.fusesource.camel.component.sap.model.rfc.impl.DestinationDataStoreEntryImpl;

import com.sap.conn.jco.ext.DestinationDataEventListener;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.Environment;

public class ComponentDestinationDataProvider implements
		DestinationDataProvider {

	public static final ComponentDestinationDataProvider INSTANCE = new ComponentDestinationDataProvider();

	/**
	 * Forwards change notifications from destination data store to any
	 * destination data listener.
	 * 
	 * @author William Collins
	 */
	public class DestinationDataStoreListener extends EContentAdapter {
		@Override
		public void notifyChanged(Notification msg) {
			if (msg.getFeature() == RfcPackage.Literals.DESTINATION_DATA_STORE__ENTRIES) {
				switch (msg.getEventType()) {
				case Notification.ADD: {
					DestinationDataStoreEntryImpl entry = (DestinationDataStoreEntryImpl) msg
							.getNewValue();
					addDestinationDataListener(entry.getKey(),
							entry.getValue());
					if (destinationDataEventListener != null)
						destinationDataEventListener.updated(entry.getKey());
					break;
				}
				case Notification.ADD_MANY: {
					@SuppressWarnings("unchecked")
					Collection<DestinationDataStoreEntryImpl> entries = (Collection<DestinationDataStoreEntryImpl>) msg
							.getNewValue();
					for (DestinationDataStoreEntryImpl entry : entries) {
						addDestinationDataListener(entry.getKey(),
								entry.getValue());
						if (destinationDataEventListener != null)
							destinationDataEventListener
									.updated(entry.getKey());
					}
					break;
				}
				case Notification.REMOVE: {
					DestinationDataStoreEntryImpl entry = (DestinationDataStoreEntryImpl) msg
							.getOldValue();
					removeDestinationDataListener(entry.getValue());
					if (destinationDataEventListener != null)
						destinationDataEventListener.deleted(entry.getKey());
					break;
				}
				case Notification.REMOVE_MANY: {
					@SuppressWarnings("unchecked")
					Collection<DestinationDataStoreEntryImpl> entries = (Collection<DestinationDataStoreEntryImpl>) msg
							.getOldValue();
					for (DestinationDataStoreEntryImpl entry : entries) {
						removeDestinationDataListener(entry.getValue());
						if (destinationDataEventListener != null)
							destinationDataEventListener
									.deleted(entry.getKey());
					}
					break;
				}
				}
			}
			super.notifyChanged(msg);
		}

		protected void addDestinationDataListener(String connectionName,
				Notifier notifier) {
			notifier.eAdapters().add(
					new DestinationDataListener(connectionName));
		}

		protected void removeDestinationDataListener(Notifier notifier) {
			Set<Adapter> listeners = new HashSet<Adapter>();
			for (Adapter adapter : notifier.eAdapters()) {
				if (adapter instanceof DestinationDataListener) {
					listeners.add(adapter);
				}
			}
			notifier.eAdapters().removeAll(listeners);
		}
	}

	private final DestinationDataStoreListener destinationDataStoreListener = new DestinationDataStoreListener();

	/**
	 * Forwards change notifications from destination data to any
	 * destination data listener.
	 * 
	 * @author William Collins
	 */
	public class DestinationDataListener extends EContentAdapter {

		private String destinationName;

		public DestinationDataListener(String destinationName) {
			this.destinationName = destinationName;
		}

		@Override
		public void notifyChanged(Notification msg) {
			switch (msg.getEventType()) {
			case Notification.SET:
			case Notification.ADD:
			case Notification.ADD_MANY: {
				if (destinationDataEventListener != null)
					destinationDataEventListener.updated(destinationName);
				break;
			}
			case Notification.REMOVE:
			case Notification.REMOVE_MANY: {
				if (destinationDataEventListener != null)
					destinationDataEventListener.deleted(destinationName);
				break;
			}
			}
			super.notifyChanged(msg);
		}
	}

	/**
	 * The cached value of the '{@link #getDestinationDataListener()
	 * <em>Destination Data Listener</em>}' attribute.
	 */
	protected DestinationDataEventListener destinationDataEventListener;

	Set<DestinationDataStore> stores = new HashSet<DestinationDataStore>();

	private ComponentDestinationDataProvider() {
		Environment.registerDestinationDataProvider(this);
	}

	public void addDestinationDataStore(DestinationDataStore store) {

		// Remember store
		stores.add(store);

		// listen for any changes in added store.
		store.eAdapters().add(destinationDataStoreListener);

		if (destinationDataEventListener == null) {
			return;
		}

		// Update DestinationDataEventListener for added configurations.
		for (String destinationName : store.getEntries().keySet()) {
			destinationDataEventListener.updated(destinationName);
		}
	}

	public void removeDestinationDataStore(
			DestinationDataStore store) {

		// Forget store
		stores.remove(store);

		// stop listening for changes in removed store.
		store.eAdapters().remove(destinationDataStoreListener);

		// Update DestinationDataEventListener for removed configurations.
		for (String destinationName : store.getEntries().keySet()) {
			destinationDataEventListener.deleted(destinationName);
		}
	}

	@Override
	public Properties getDestinationProperties(String destinationName) {
		for (DestinationDataStore store : stores) {
			DestinationData destinationData = store.getEntries().get(destinationName);
			if (destinationData != null) {
				Properties properties = new Properties();
				properties.putAll(destinationData.getEntries().map());
				return properties;
			}
		}
		return null;
	}

	@Override
	public void setDestinationDataEventListener(
			DestinationDataEventListener destinationDataEventListener) {
		this.destinationDataEventListener = destinationDataEventListener;
	}

	@Override
	public boolean supportsEvents() {
		return true;
	}

}
