/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.service;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.fabric.api.DataStore;
import org.fusesource.fabric.api.DataStorePlugin;
import org.fusesource.fabric.api.DataStoreRegistrationHandler;
import org.fusesource.fabric.api.DataStoreTemplate;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.jcip.GuardedBy;
import org.fusesource.fabric.api.jcip.ThreadSafe;
import org.fusesource.fabric.api.scr.AbstractComponent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.apache.felix.scr.annotations.ReferenceCardinality.OPTIONAL_MULTIPLE;
import static org.fusesource.fabric.api.DataStore.DATASTORE_TYPE_PID;
import static org.fusesource.fabric.api.DataStore.DATASTORE_TYPE_PROPERTY;
import static org.fusesource.fabric.api.DataStore.DEFAULT_DATASTORE_TYPE;

/**
 * Manager of {@link DataStore} using configuration to decide which
 * implementation to export.
 */
@ThreadSafe
@Component(name = DataStore.DATASTORE_TYPE_PID, description = "Configured DataStore Factory", immediate = true) // Done
@Service(DataStoreRegistrationHandler.class)
public final class DataStoreManager extends AbstractComponent implements DataStoreRegistrationHandler {

    private static final transient Logger LOG = LoggerFactory.getLogger(DataStoreManager.class);

    @Reference(referenceInterface = DataStorePlugin.class, bind = "bindDataStore", unbind = "unbindDataStore", cardinality = OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    @GuardedBy("this") private final Map<String, DataStorePlugin> dataStorePlugins = new HashMap<String, DataStorePlugin>();

    @GuardedBy("this") private final List<DataStoreTemplate> registrationCallbacks = new CopyOnWriteArrayList<DataStoreTemplate>();

    @GuardedBy("this") private BundleContext bundleContext;
    @GuardedBy("this") private Map<String,String> configuration;
    @GuardedBy("this") private String type;

    @GuardedBy("this") private DataStore dataStore;
    @GuardedBy("this") private ServiceRegistration<DataStore> registration;

    @Activate
    synchronized void activate(BundleContext bundleContext, Map<String,String> configuration) {
        this.bundleContext = bundleContext;
        this.configuration = Collections.unmodifiableMap(configuration);
        this.type = readType(this.configuration);
        updateServiceRegistration();
        activateComponent();
    }

    @Modified
    synchronized void update(Map<String,String> configuration) {
        this.configuration = Collections.unmodifiableMap(configuration);
        this.type = readType(this.configuration);
        updateServiceRegistration();
    }

    @Deactivate
    synchronized void deactivate() {
        deactivateComponent();
        unregisterDataStore();
    }

    @Override
    public synchronized void addRegistrationCallback(DataStoreTemplate template) {
        if (isValid()) {
            // [TODO] What should happen when the callback is registered after the DataStore?
            registrationCallbacks.add(template);
        }
    }

    @Override
    public synchronized void removeRegistrationCallback(DataStoreTemplate template) {
        registrationCallbacks.remove(template);
    }

    private synchronized void updateServiceRegistration() {

        // Always unregister the previous {@link DataStore}
        unregisterDataStore();

        DataStorePlugin pluginDataStore = dataStorePlugins.get(type);
        if (pluginDataStore != null) {
            dataStore = pluginDataStore.getDataStore();
            Map<String, String> dataStoreProperties = new HashMap<String, String>();
            dataStoreProperties.putAll(configuration);
            dataStore.setDataStoreProperties(dataStoreProperties);
            dataStore.start();
            for(DataStoreTemplate callback : registrationCallbacks) {
                registrationCallbacks.remove(callback);
                try {
                    callback.doWith(dataStore);
                } catch (Exception e) {
                    throw new FabricException(e);
                }
            }
            Dictionary<String, String> properties = new Hashtable<String, String>();
            properties.put(DATASTORE_TYPE_PROPERTY, type);
            registration = bundleContext.registerService(DataStore.class, dataStore, properties);
            LOG.info("Registered DataStore " + dataStore + " with " + properties);
        }
    }

    private synchronized void unregisterDataStore() {
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
        if (dataStore != null) {
            dataStore.stop();
            dataStore = null;
        }
    }

    /**
     * Extracts the type from the specified map or System configuration.
     * @param configuration The map to use as a source.
     */
    private static String readType(Map<String, String> configuration) {
        if (configuration.containsKey(DATASTORE_TYPE_PROPERTY) && configuration.get(DATASTORE_TYPE_PROPERTY) != null) {
            return configuration.get(DATASTORE_TYPE_PROPERTY);
        } else {
            return System.getProperty(DATASTORE_TYPE_PID + "." + DATASTORE_TYPE_PROPERTY, DEFAULT_DATASTORE_TYPE);
        }
    }

    synchronized void bindDataStore(DataStorePlugin dataStorePlugin) {
        dataStorePlugins.put(dataStorePlugin.getType(), dataStorePlugin);
        if (dataStorePlugin.getType().equals(type)) {
            updateServiceRegistration();
        }
    }

    synchronized void unbindDataStore(DataStorePlugin dataStorePlugin) {
        dataStorePlugins.remove(dataStorePlugin.getType());
        if (dataStorePlugin.getType().equals(type)) {
            updateServiceRegistration();
        }
    }

}
