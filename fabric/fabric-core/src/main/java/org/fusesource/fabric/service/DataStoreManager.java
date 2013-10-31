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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import static org.apache.felix.scr.annotations.ReferenceCardinality.OPTIONAL_MULTIPLE;
import static org.fusesource.fabric.api.DataStore.DATASTORE_TYPE_PID;
import static org.fusesource.fabric.api.DataStore.DATASTORE_TYPE_PROPERTY;
import static org.fusesource.fabric.api.DataStore.DEFAULT_DATASTORE_TYPE;

/**
 * Manager of {@link DataStore} using configuration to decide which
 * implementation to export.
 */
@ThreadSafe
@Component(name = DataStore.DATASTORE_TYPE_PID, description = "Configured DataStore Factory", immediate = true)
@Service(DataStoreRegistrationHandler.class)
public final class DataStoreManager extends AbstractComponent implements DataStoreRegistrationHandler {

    private static final transient Logger LOG = LoggerFactory.getLogger(DataStoreManager.class);

    @Reference(referenceInterface = DataStorePlugin.class, bind = "bindDataStore", unbind = "unbindDataStore", cardinality = OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    @GuardedBy("this") private final Map<String, DataStorePlugin> dataStorePlugins = new HashMap<String, DataStorePlugin>();

    @GuardedBy("this") private final List<DataStoreTemplate> registrationCallbacks = new ArrayList<DataStoreTemplate>();

    @GuardedBy("this") private BundleContext bundleContext;
    @GuardedBy("this") private Map<String, ?> configuration;
    @GuardedBy("this") private String type;

    @GuardedBy("this") private DataStore dataStore;
    @GuardedBy("this") private ServiceRegistration<DataStore> registration;

    @Activate
    void activate(BundleContext bundleContext, Map<String, ?> configuration) {
        synchronized (this) {
            this.bundleContext = bundleContext;
            this.configuration = Collections.unmodifiableMap(configuration);
            this.type = readType(this.configuration);
        }
        updateServiceRegistration();
        activateComponent();
    }

    @Modified
    void update(Map<String, ?> configuration) {
        synchronized (this) {
            this.configuration = Collections.unmodifiableMap(configuration);
            this.type = readType(this.configuration);
        }
        updateServiceRegistration();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        unregisterDataStore();
    }

    @Override
    public void addRegistrationCallback(DataStoreTemplate template) {
        if (isValid()) {
            // [TODO] What should happen when the callback is registered after the DataStore?
            synchronized (this) {
                registrationCallbacks.add(template);
            }
        }
    }

    @Override
    public void removeRegistrationCallback(DataStoreTemplate template) {
        synchronized (this) {
            registrationCallbacks.remove(template);
        }
    }

    private void updateServiceRegistration() {
        //
        // Do not hold any lock while calling external services or the OSGi API
        // so we only synchronize on property access.
        //

        // Always unregister the previous {@link DataStore}
        unregisterDataStore();

        DataStore auxStore = null;
        DataStorePlugin pluginDataStore;
        synchronized (this) {
            pluginDataStore = dataStorePlugins.get(type);
        }
        if (pluginDataStore != null) {
            Map<String, String> dataStoreProperties;
            synchronized (this) {
                dataStoreProperties = new HashMap<String, String>();
                for(Map.Entry<String, ?> entry : configuration.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        dataStoreProperties.put(key, (String) value);
                    }
                }
            }
            auxStore = pluginDataStore.getDataStore();
            auxStore.setDataStoreProperties(dataStoreProperties);
            auxStore.start();
        }
        if (auxStore != null) {
            List<DataStoreTemplate> callbacks = null;
            synchronized (this) {
                if (dataStore == null) {
                    dataStore = auxStore;
                    callbacks = new ArrayList<DataStoreTemplate>(registrationCallbacks);
                }
            }
            if (callbacks != null) {
                for(DataStoreTemplate callback : callbacks) {
                    registrationCallbacks.remove(callback);
                    try {
                        callback.doWith(auxStore);
                    } catch (Exception e) {
                        throw FabricException.launderThrowable(e);
                    }
                }
                Dictionary<String, String> properties = new Hashtable<String, String>();
                properties.put(DATASTORE_TYPE_PROPERTY, auxStore.getType());
                ServiceRegistration<DataStore> reg = bundleContext.registerService(DataStore.class, auxStore, properties);
                LOG.info("Registered DataStore " + auxStore + " with " + properties);
                synchronized (this) {
                    registration = reg;
                    registrationCallbacks.removeAll(callbacks);
                }
            }
        }
    }

    private void unregisterDataStore() {
        ServiceRegistration<DataStore> reg;
        DataStore ds;
        synchronized (this) {
            reg = registration;
            registration = null;
            ds = dataStore;
            dataStore = null;
        }
        if (reg != null) {
            reg.unregister();
        }
        if (ds != null) {
            ds.stop();
        }
    }

    /**
     * Extracts the type from the specified map or System configuration.
     * @param configuration The map to use as a source.
     */
    private static String readType(Map<String, ?> configuration) {
        if (configuration.containsKey(DATASTORE_TYPE_PROPERTY) && configuration.get(DATASTORE_TYPE_PROPERTY) != null) {
            return configuration.get(DATASTORE_TYPE_PROPERTY).toString();
        } else {
            return System.getProperty(DATASTORE_TYPE_PID + "." + DATASTORE_TYPE_PROPERTY, DEFAULT_DATASTORE_TYPE);
        }
    }

    void bindDataStore(DataStorePlugin dataStorePlugin) {
        boolean update;
        synchronized (this) {
            dataStorePlugins.put(dataStorePlugin.getType(), dataStorePlugin);
            update = dataStorePlugin.getType().equals(type);
        }
        if (update) {
            updateServiceRegistration();
        }
    }

    void unbindDataStore(DataStorePlugin dataStorePlugin) {
        boolean update;
        synchronized (this) {
            dataStorePlugins.remove(dataStorePlugin.getType());
            update = dataStorePlugin.getType().equals(type);
        }
        if (update) {
            updateServiceRegistration();
        }
    }

}
