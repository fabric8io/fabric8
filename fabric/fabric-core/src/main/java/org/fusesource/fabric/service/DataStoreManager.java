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
import org.fusesource.fabric.service.support.AbstractComponent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@Component(name = DataStore.DATASTORE_TYPE_PID, description = "Configured DataStore Factory", immediate = true)
@Service(DataStoreRegistrationHandler.class)
public class DataStoreManager extends AbstractComponent implements DataStoreRegistrationHandler {

    private static final transient Logger LOG = LoggerFactory.getLogger(DataStoreManager.class);

    @Reference(cardinality = OPTIONAL_MULTIPLE, referenceInterface = DataStorePlugin.class, bind = "bindDataStore", unbind = "unbindDataStore", policy = ReferencePolicy.DYNAMIC)
    private final Map<String, DataStorePlugin> dataStorePlugins = new HashMap<String, DataStorePlugin>();

    private Map<String,String> configuration;

    private String type;
    private DataStore dataStore;
    private Dictionary<String, String> properties = new Hashtable<String, String>();
    private ServiceRegistration<DataStore> registration;

    private final List<DataStoreTemplate> registrationCallbacks = new CopyOnWriteArrayList<DataStoreTemplate>();

    @Activate
    synchronized void activate(ComponentContext context, Map<String,String> configuration) {
        activateComponent(context);
        try {
            update(configuration);
        } catch (RuntimeException rte) {
            deactivateComponent();
            throw rte;
        }
    }

    @Modified
    synchronized void update(Map<String,String> configuration) {
        this.configuration = new HashMap<String, String>(configuration);
        this.type = readType(configuration);
        updateServiceRegistration();
    }

    @Deactivate
    synchronized void deactivate() {
        try {
            unregister();
        } finally {
            deactivateComponent();
        }
    }

    public void updateServiceRegistration() {
        unregister();
        if (dataStorePlugins.containsKey(type)) {
            dataStore = dataStorePlugins.get(type).getDataStore();
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
            properties.put(DATASTORE_TYPE_PROPERTY, type);
            BundleContext bundleContext = getComponentContext().getBundleContext();
            registration = bundleContext.registerService(DataStore.class, dataStore, properties);
            LOG.info("Registered DataStore " + dataStore + " with " + properties);
        }
    }

    /**
     * Unregisters the {@link DataStore}.
     */
    private void unregister() {
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
        if (dataStore != null) {
            dataStore.stop();
        }
    }

    /**
     * Extracts the type from the specified map or System configuration.
     * @param configuration The map to use as a source.
     * @return
     */
    private static String readType(Map<String, String> configuration) {
        if (configuration.containsKey(DATASTORE_TYPE_PROPERTY) && configuration.get(DATASTORE_TYPE_PROPERTY) != null) {
            return configuration.get(DATASTORE_TYPE_PROPERTY);
        } else {
            return System.getProperty(DATASTORE_TYPE_PID + "." + DATASTORE_TYPE_PROPERTY, DEFAULT_DATASTORE_TYPE);
        }
    }

    public synchronized void bindDataStore(DataStorePlugin dataStorePlugin) {
        if (dataStorePlugin != null) {
            dataStorePlugins.put(dataStorePlugin.getType(), dataStorePlugin);
            if (dataStorePlugin.getType().equals(type)) {
                updateServiceRegistration();
            }
        }
    }

    public synchronized void unbindDataStore(DataStorePlugin dataStorePlugin) {
        if (dataStorePlugin != null) {
            dataStorePlugins.remove(dataStorePlugin.getType());
            if (dataStorePlugin.getType().equals(type)) {
                updateServiceRegistration();
            }
        }
    }

    public Map<String, String> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, String> configuration) {
        this.configuration = configuration;
    }

    @Override
    public void addRegistrationCallback(DataStoreTemplate template) {
        this.registrationCallbacks.add(template);
    }

    @Override
    public void removeRegistrationCallback(DataStoreTemplate template) {
        this.registrationCallbacks.remove(template);
    }
}
