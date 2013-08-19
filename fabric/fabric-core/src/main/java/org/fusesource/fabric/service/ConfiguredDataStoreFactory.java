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

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import com.google.common.base.Strings;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.fabric.api.DataStore;
import org.fusesource.fabric.api.DataStoreFactory;
import org.fusesource.fabric.api.PlaceholderResolver;
import org.fusesource.fabric.internal.Objects;
import org.fusesource.fabric.service.git.GitDataStore;
import org.fusesource.fabric.service.git.GitService;
import org.fusesource.fabric.service.git.LocalGitService;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY;
import static org.apache.felix.scr.annotations.ReferenceCardinality.OPTIONAL_MULTIPLE;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getProperties;

/**
 * Factory of {@link DataStore} using configuration to decide which
 * implementation to use
 */
@Component(name = "org.fusesource.fabric.datastore.factory",
        description = "Configured DataStore Factory",
        immediate = true)
@Service(DataStoreFactory.class)
public class ConfiguredDataStoreFactory implements DataStoreFactory {
    private static final transient Logger LOG = LoggerFactory.getLogger(ConfiguredDataStoreFactory.class);

    public static final String DATASTORE_KIND = "org.fusesource.fabric.datastore";


    @Reference(cardinality = MANDATORY_UNARY)
    private CuratorFramework curator;

    @Reference(cardinality = OPTIONAL_MULTIPLE,
            referenceInterface = PlaceholderResolver.class,
            bind = "bindPlaceholderResolver", unbind = "unbindPlaceholderResolver",
            policy = ReferencePolicy.DYNAMIC)
    private final Map<String, PlaceholderResolver>
            placeholderResolvers = new HashMap<String, PlaceholderResolver>();

    @Reference(cardinality = MANDATORY_UNARY)
    private GitService gitService;

    private BundleContext bundleContext;
    private DataStoreSupport instance;
    private Dictionary<String, String> properties = new Hashtable<String, String>();
    private ServiceRegistration<DataStore> registration;
    private boolean bootstrap;
    private Map<String,String> configuration;

    @Activate
    public synchronized void init(BundleContext bundleContext, Map<String,String> configuration) throws Exception {
        setConfiguration(configuration);
        this.bundleContext = bundleContext;
        if (instance == null) {
            DataStore dataStore = createDataStore();
            if (dataStore instanceof DataStoreSupport) {
                instance = (DataStoreSupport)dataStore;
            }
        }
    }

    @Deactivate
    public synchronized void destroy() {
        if (registration != null) {
            registration.unregister();
        }
        if (instance != null) {
            instance.destroy();
        }
    }

    public DataStore createDataStore() throws Exception {
        final DataStoreSupport instance;
        // let try find the kind from zookeeper, then
        // the osgi configuration and finally from
        // system properties

        String kind = configuration.get("kind");
        Properties clusterProperties = new Properties();
        if (curator.checkExists().forPath(ZkPath.BOOTSTRAP.getPath()) != null) {
            clusterProperties = getProperties(curator, ZkPath.BOOTSTRAP.getPath());
        }
        clusterProperties.putAll(configuration);
        if (Strings.isNullOrEmpty(kind)) {
            kind = clusterProperties.getProperty("kind", System.getProperty(DATASTORE_KIND, "zookeeper"));
        }
        kind = kind.toLowerCase();
        if (kind.startsWith("z")) {
            properties.put("kind", "org.fusesource.datastore.zookeeper");
            instance = new ZooKeeperDataStore();
        } else {
            properties.put("kind", "org.fusesource.datastore.git");
            instance = new GitDataStore();
        }
        LOG.info("DataStore kind=" + kind + " so created DataStore: " + instance + " from clusterProperties "
                + clusterProperties + " and configuration " + configuration);
        Objects.notNull(curator, "curator");
        instance.setCurator(curator);
        instance.setPlaceholderResolvers(placeholderResolvers);
        instance.setDataStoreProperties(clusterProperties);

        Runnable onInitialised = new Runnable() {
            public void run() {
                Objects.notNull(bundleContext, "bundleContext");
                registration = bundleContext.registerService(DataStore.class, instance, properties);
                LOG.info("Registered DataStore " + instance + " with " + properties);
            }
        };
        if (isBootstrap()) {
            // no need to register during bootstrap as we start it up
            // normally after the bootstrap phase
        } else {
            instance.setOnInitialised(onInitialised);
        }
        if (instance instanceof GitDataStore) {
            GitDataStore gitDataStore = (GitDataStore)instance;
            if (isBootstrap()) {
                if (gitService == null) {
                    LOG.info("Creating bootstrap GitService");
                    gitService = createBootstrapGitService();
                }
            } else {
                Objects.notNull(gitService, "gitService");
            }
            LOG.info("Using GitService: " + gitService);
            gitDataStore.setGitService(gitService);
        }
        instance.init();
        return instance;
    }

    /**
     * Lets create a GitService for use in bootstrap when there's no fabric yet, so we just make a local git repo
     */
    protected GitService createBootstrapGitService() {
        return new LocalGitService();
    }


    // Properties
    //-------------------------------------------------------------------------

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void bindCurator(CuratorFramework curator) throws Exception {
        this.setCurator(curator);
    }

    public void unbindCurator(CuratorFramework curator) throws IOException {
        this.setCurator(null);
    }

    public Map<String, PlaceholderResolver> getPlaceholderResolvers() {
        return placeholderResolvers;
    }

    public synchronized void bindPlaceholderResolver(PlaceholderResolver resolver) {
        if (resolver != null) {
            getPlaceholderResolvers().put(resolver.getScheme(), resolver);
        }
    }

    public synchronized void unbindPlaceholderResolver(PlaceholderResolver resolver) {
        if (resolver != null) {
            getPlaceholderResolvers().remove(resolver.getScheme());
        }
    }

    public Map<String, String> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, String> configuration) {
        this.configuration = configuration;
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }

    public GitService getGitService() {
        return gitService;
    }

    public void setGitService(GitService gitService) {
        this.gitService = gitService;
    }

    /**
     * Called when using this factory when bootstrapping a Fabric
     */
    public void setBootstrap(boolean bootstrap) {
        this.bootstrap = bootstrap;
    }

    public boolean isBootstrap() {
        return bootstrap;
    }
}
