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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.fabric.api.DataStore;
import org.fusesource.fabric.api.DataStorePlugin;
import org.fusesource.fabric.api.DataStoreRegistrationHandler;
import org.fusesource.fabric.api.DataStoreTemplate;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.PlaceholderResolver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.felix.scr.annotations.ReferenceCardinality.OPTIONAL_MULTIPLE;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getProperties;

/**
 * Manager of {@link DataStore} using configuration to decide which
 * implementation to export.
 */
@Component(name = "org.fusesource.fabric.datastore.manager",
        description = "Configured DataStore Factory",
        immediate = true)
@Service(DataStoreRegistrationHandler.class)
public class DataStoreManager implements DataStoreRegistrationHandler {
    private static final transient Logger LOG = LoggerFactory.getLogger(DataStoreManager.class);

    public static final String DATASTORE_TYPE_PID = "org.fusesource.fabric.datastore";
    public static final String DATASTORE_TYPE_PROPERTY = "type";
    public static final String DEFAULT_DATASTORE_TYPE = "git";


    @Reference(cardinality = OPTIONAL_MULTIPLE,
            referenceInterface = DataStorePlugin.class,
            bind = "bindDataStore", unbind = "unbindDataStore",
            policy = ReferencePolicy.DYNAMIC)
    private final Map<String, DataStorePlugin> dataStorePlugins = new HashMap<String, DataStorePlugin>();

    private BundleContext bundleContext;
    private Map<String,String> configuration;

    private String type;
    private DataStore dataStore;
    private Dictionary<String, String> properties = new Hashtable<String, String>();
    private ServiceRegistration<DataStore> registration;

    private final List<DataStoreTemplate> registrationCallbacks = new CopyOnWriteArrayList<DataStoreTemplate>();


    @Activate
    public synchronized void init(BundleContext bundleContext, Map<String,String> configuration) throws Exception {
        this.bundleContext = bundleContext;
        this.configuration = configuration;
        this.type = readType(configuration);
        updateServiceRegistration();
    }

    @Deactivate
    public synchronized void destroy() {
        unregister();
    }

    public void updateServiceRegistration() {
        unregister();
        if (dataStorePlugins.containsKey(type)) {
            dataStore = dataStorePlugins.get(type).getDataStore();
            Properties dataStoreProperties = new Properties();
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
        if (configuration.containsKey(DATASTORE_TYPE_PROPERTY)) {
            return configuration.get(DATASTORE_TYPE_PROPERTY);
        } else {
            return System.getProperty(DATASTORE_TYPE_PID + "." + DATASTORE_TYPE_PROPERTY, DEFAULT_DATASTORE_TYPE);
        }
    }


    // Properties
    //-------------------------------------------------------------------------

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }


    public synchronized void bindDataStore(DataStorePlugin dataStorePlugin) {
        if (dataStorePlugin != null) {
            dataStorePlugins.put(dataStorePlugin.getName(), dataStorePlugin);
            if (type != null && type.equals(dataStorePlugin.getName())) {
                updateServiceRegistration();
            }
        }
    }

    public synchronized void unbindDataStore(DataStorePlugin dataStorePlugin) {
        if (dataStorePlugin != null) {
            dataStorePlugins.remove(dataStorePlugin.getName());
            if (type != null && type.equals(dataStorePlugin.getName())) {
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
