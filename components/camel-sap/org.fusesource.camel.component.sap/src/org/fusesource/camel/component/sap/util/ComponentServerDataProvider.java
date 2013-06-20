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
import org.fusesource.camel.component.sap.model.rfc.RfcPackage;
import org.fusesource.camel.component.sap.model.rfc.ServerData;
import org.fusesource.camel.component.sap.model.rfc.ServerDataStore;
import org.fusesource.camel.component.sap.model.rfc.impl.ServerDataStoreEntryImpl;

import com.sap.conn.jco.ext.Environment;
import com.sap.conn.jco.ext.ServerDataEventListener;
import com.sap.conn.jco.ext.ServerDataProvider;

public class ComponentServerDataProvider implements
		ServerDataProvider {

	public static final ComponentServerDataProvider INSTANCE = new ComponentServerDataProvider();

	/**
	 * Forwards change notifications from server data store to any
	 * server data listener.
	 * 
	 * @author William Collins
	 */
	public class ServerDataStoreListener extends EContentAdapter {
		@Override
		public void notifyChanged(Notification msg) {
			if (msg.getFeature() == RfcPackage.Literals.SERVER_DATA_STORE__ENTRIES) {
				switch (msg.getEventType()) {
				case Notification.ADD: {
					ServerDataStoreEntryImpl entry = (ServerDataStoreEntryImpl) msg
							.getNewValue();
					addServerDataListener(entry.getKey(),
							entry.getValue());
					if (serverDataEventListener != null)
						serverDataEventListener.updated(entry.getKey());
					break;
				}
				case Notification.ADD_MANY: {
					@SuppressWarnings("unchecked")
					Collection<ServerDataStoreEntryImpl> entries = (Collection<ServerDataStoreEntryImpl>) msg
							.getNewValue();
					for (ServerDataStoreEntryImpl entry : entries) {
						addServerDataListener(entry.getKey(),
								entry.getValue());
						if (serverDataEventListener != null)
							serverDataEventListener
									.updated(entry.getKey());
					}
					break;
				}
				case Notification.REMOVE: {
					ServerDataStoreEntryImpl entry = (ServerDataStoreEntryImpl) msg
							.getOldValue();
					removeServerDataListener(entry.getValue());
					if (serverDataEventListener != null)
						serverDataEventListener.deleted(entry.getKey());
					break;
				}
				case Notification.REMOVE_MANY: {
					@SuppressWarnings("unchecked")
					Collection<ServerDataStoreEntryImpl> entries = (Collection<ServerDataStoreEntryImpl>) msg
							.getOldValue();
					for (ServerDataStoreEntryImpl entry : entries) {
						removeServerDataListener(entry.getValue());
						if (serverDataEventListener != null)
							serverDataEventListener
									.deleted(entry.getKey());
					}
					break;
				}
				}
			}
			super.notifyChanged(msg);
		}

		protected void addServerDataListener(String serverName,
				Notifier notifier) {
			notifier.eAdapters().add(
					new ServerDataListener(serverName));
		}

		protected void removeServerDataListener(Notifier notifier) {
			Set<Adapter> listeners = new HashSet<Adapter>();
			for (Adapter adapter : notifier.eAdapters()) {
				if (adapter instanceof ServerDataListener) {
					listeners.add(adapter);
				}
			}
			notifier.eAdapters().removeAll(listeners);
		}
	}

	private final ServerDataStoreListener serverDataStoreListener = new ServerDataStoreListener();

	/**
	 * Forwards change notifications from server data to any
	 * server data listener.
	 * 
	 * @author William Collins
	 */
	public class ServerDataListener extends EContentAdapter {

		private String serverName;

		public ServerDataListener(String serverName) {
			this.serverName = serverName;
		}

		@Override
		public void notifyChanged(Notification msg) {
			switch (msg.getEventType()) {
			case Notification.SET:
			case Notification.ADD:
			case Notification.ADD_MANY: {
				if (serverDataEventListener != null)
					serverDataEventListener.updated(serverName);
				break;
			}
			case Notification.REMOVE:
			case Notification.REMOVE_MANY: {
				if (serverDataEventListener != null)
					serverDataEventListener.deleted(serverName);
				break;
			}
			}
			super.notifyChanged(msg);
		}
	}

	/**
	 * The cached value of the '{@link #getServerDataListener()
	 * <em>Server Data Listener</em>}' attribute.
	 */
	protected ServerDataEventListener serverDataEventListener;

	Set<ServerDataStore> stores = new HashSet<ServerDataStore>();

	private ComponentServerDataProvider() {
		Environment.registerServerDataProvider(this);
	}

	public void addServerDataStore(ServerDataStore store) {

		// Remember store
		stores.add(store);

		// listen for any changes in added store.
		store.eAdapters().add(serverDataStoreListener);

		if (serverDataEventListener == null) {
			return;
		}

		// Update ServerDataEventListener for added configurations.
		for (String serverName : store.getEntries().keySet()) {
			serverDataEventListener.updated(serverName);
		}
	}

	public void removeServerDataStore(
			ServerDataStore store) {

		// Forget store
		stores.remove(store);

		// stop listening for changes in removed store.
		store.eAdapters().remove(serverDataStoreListener);

		// Update ServerDataEventListener for removed configurations.
		for (String serverName : store.getEntries().keySet()) {
			serverDataEventListener.deleted(serverName);
		}
	}

	@Override
	public boolean supportsEvents() {
		return true;
	}

	@Override
	public Properties getServerProperties(String serverName) {
		for (ServerDataStore store : stores) {
			ServerData serverData = store.getEntries().get(serverName);
			if (serverData != null) {
				Properties properties = new Properties();
				properties.putAll(serverData.getEntries().map());
				return properties;
			}
		}
		return null;
	}

	@Override
	public void setServerDataEventListener(ServerDataEventListener serverDataEventListener) {
		this.serverDataEventListener = serverDataEventListener;
	}

}
