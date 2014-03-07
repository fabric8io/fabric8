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
package io.fabric8.internal;

import io.fabric8.api.BootstrapComplete;
import io.fabric8.api.Container;
import io.fabric8.api.CreateEnsembleOptions;
import io.fabric8.api.DataStoreRegistrationHandler;
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceLocator;
import io.fabric8.api.ZooKeeperClusterBootstrap;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.Configurer;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.utils.BundleUtils;
import io.fabric8.zookeeper.bootstrap.BootstrapConfiguration;
import io.fabric8.zookeeper.bootstrap.DataStoreBootstrapTemplate;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

/**
 * ZooKeeperClusterBootstrap
 * |_ ConfigurationAdmin
 * |_ DataStoreRegistrationHandler (@see DataStoreManager)
 * |_ BootstrapConfiguration (@see BootstrapConfiguration)
 */
@ThreadSafe
@Component(name = "io.fabric8.zookeeper.cluster.bootstrap", label = "Fabric8 ZooKeeper Cluster Bootstrap", immediate = true, metatype = false)
@Service(ZooKeeperClusterBootstrap.class)
public final class ZooKeeperClusterBootstrapImpl extends AbstractComponent implements ZooKeeperClusterBootstrap {

    @Reference
    private Configurer configurer;

    @Reference(referenceInterface = ConfigurationAdmin.class)
    private final ValidatingReference<ConfigurationAdmin> configAdmin = new ValidatingReference<ConfigurationAdmin>();
    @Reference(referenceInterface = DataStoreRegistrationHandler.class)
    private final ValidatingReference<DataStoreRegistrationHandler> registrationHandler = new ValidatingReference<DataStoreRegistrationHandler>();
    @Reference(referenceInterface = BootstrapConfiguration.class)
    private final ValidatingReference<BootstrapConfiguration> bootstrapConfiguration = new ValidatingReference<BootstrapConfiguration>();

    @Property(name = "name", label = "Container Name", description = "The name of the container", value = "${karaf.name}")
    private String name;
    @Property(name = "home", label = "Container Home", description = "The home directory of the container", value = "${karaf.home}")
    private String home;
    @Property(name = "data", label = "Container Data", description = "The data directory of the container", value = "${karaf.data}")
    private String data;

    private BundleContext bundleContext;

    @Activate
    void activate(BundleContext bundleContext, Map<String, ?> configuration) throws Exception {
        this.bundleContext = bundleContext;
        this.configurer.configure(configuration, this);
        BootstrapConfiguration bootConfig = bootstrapConfiguration.get();
        CreateEnsembleOptions options = bootConfig.getBootstrapOptions();
        if (options.isEnsembleStart()) {
            startBundles(options);
        }
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public void create(CreateEnsembleOptions options) {
        assertValid();
        try {
            // Wait for bootstrap to be complete
            ServiceLocator.awaitService(bundleContext, BootstrapComplete.class);

            stopBundles();

            DataStoreRegistrationHandler regHandler = registrationHandler.get();
            BootstrapConfiguration bootConfig = bootstrapConfiguration.get();
            BundleContext syscontext = bundleContext.getBundle(0).getBundleContext();
            if (options.isClean()) {
                bootConfig = cleanInternal(syscontext, bootConfig, regHandler);
            }

            BootstrapCreateHandler createHandler = new BootstrapCreateHandler(bootConfig, regHandler);
            createHandler.bootstrapFabric(name, home, options);

            startBundles(options);

            long startTime = System.currentTimeMillis();
            createHandler.waitForContainerAlive(name, syscontext, options.getBootstrapTimeout());

            if (options.isWaitForProvision() && options.isAgentEnabled()) {
                long currentTime = System.currentTimeMillis();
                createHandler.waitForSuccessfulDeploymentOf(name, syscontext, options.getBootstrapTimeout() - (currentTime - startTime));
            }
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception ex) {
            throw new FabricException("Unable to create zookeeper server configuration", ex);
        }
    }

    private BootstrapConfiguration cleanInternal(final BundleContext syscontext, BootstrapConfiguration bootConfig, DataStoreRegistrationHandler registrationHandler) throws TimeoutException {
        try {
            Configuration[] configs = configAdmin.get().listConfigurations("(|(service.factoryPid=io.fabric8.zookeeper.server)(service.pid=io.fabric8.zookeeper))");
            File karafData = new File(data);

            // Setup the listener for unregistration of {@link BootstrapConfiguration}
            final CountDownLatch unregisterLatch = new CountDownLatch(1);
            ServiceListener listener = new ServiceListener() {
                @Override
                public void serviceChanged(ServiceEvent event) {
                    if (event.getType() == ServiceEvent.UNREGISTERING) {
                        syscontext.removeServiceListener(this);
                        unregisterLatch.countDown();
                    }
                }
            };
            syscontext.addServiceListener(listener, "(objectClass=" + BootstrapConfiguration.class.getName() + ")");

            // Disable the BootstrapConfiguration component
            ComponentContext componentContext = bootConfig.getComponentContext();
            componentContext.disableComponent(BootstrapConfiguration.COMPONENT_NAME);

            if (!unregisterLatch.await(30, TimeUnit.SECONDS))
                throw new TimeoutException("Timeout for unregistering BootstrapConfiguration service");

            // Do the cleanup
            registrationHandler.removeRegistrationCallback();
            cleanConfigurations(configs);
            cleanZookeeperDirectory(karafData);
            cleanGitDirectory(karafData);

            // Setup the registration listener for the new {@link BootstrapConfiguration}
            final CountDownLatch registerLatch = new CountDownLatch(1);
            final AtomicReference<ServiceReference<?>> sref = new AtomicReference<ServiceReference<?>>();
            listener = new ServiceListener() {
                @Override
                public void serviceChanged(ServiceEvent event) {
                    if (event.getType() == ServiceEvent.REGISTERED) {
                        syscontext.removeServiceListener(this);
                        sref.set(event.getServiceReference());
                        registerLatch.countDown();
                    }
                }
            };
            syscontext.addServiceListener(listener, "(objectClass=" + BootstrapConfiguration.class.getName() + ")");

            // Enable the {@link BootstrapConfiguration} component and await the registration of the respective service
            componentContext.enableComponent(BootstrapConfiguration.COMPONENT_NAME);
            if (!registerLatch.await(30, TimeUnit.SECONDS))
                throw new TimeoutException("Timeout for registering BootstrapConfiguration service");

            return (BootstrapConfiguration) syscontext.getService(sref.get());

        } catch (RuntimeException rte) {
            throw rte;
        } catch (TimeoutException toe) {
            throw toe;
        } catch (Exception ex) {
            throw new FabricException("Unable to delete zookeeper configuration", ex);
        }
    }

    private void cleanConfigurations(Configuration[] configs) throws IOException, InvalidSyntaxException {
        if (configs != null && configs.length > 0) {
            for (Configuration config : configs) {
                config.delete();
            }
        }
    }

    private void cleanZookeeperDirectory(File karafData) throws IOException {
        File zkdir = new File(karafData, "zookeeper");
        if (zkdir.isDirectory()) {
            File renamed = new File(karafData, "zookeeper." + System.currentTimeMillis());
            if (!zkdir.renameTo(renamed)) {
                throw new IOException("Cannot rename zookeeper data dir for removal: " + zkdir);
            }
            delete(renamed);
        }
    }

    private void cleanGitDirectory(File karafData) throws IOException {
        File gitdir = new File(karafData, "git");
        if (gitdir.isDirectory()) {
            File renamed = new File(karafData, "git." + System.currentTimeMillis());
            if (!gitdir.renameTo(renamed)) {
                throw new IOException("Cannot rename git data dir for removal: " + gitdir);
            }
            delete(renamed);
        }
    }

    private void stopBundles() throws BundleException {
        BundleUtils bundleUtils = new BundleUtils(bundleContext);
        bundleUtils.findAndStopBundle("io.fabric8.fabric-agent");
    }

    private void startBundles(CreateEnsembleOptions options) throws BundleException {
        BundleUtils bundleUtils = new BundleUtils(bundleContext);
        Bundle agentBundle = bundleUtils.findBundle("io.fabric8.fabric-agent");
        if (agentBundle != null && options.isAgentEnabled()) {
            agentBundle.start();
        }
    }

    private static void delete(File dir) {
        if (dir.isDirectory()) {
            for (File child : dir.listFiles()) {
                delete(child);
            }
        }
        if (dir.exists()) {
            dir.delete();
        }
    }

    void bindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.bind(service);
    }

    void unbindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.unbind(service);
    }

    void bindBootstrapConfiguration(BootstrapConfiguration service) {
        this.bootstrapConfiguration.bind(service);
    }

    void unbindBootstrapConfiguration(BootstrapConfiguration service) {
        this.bootstrapConfiguration.unbind(service);
    }

    void bindRegistrationHandler(DataStoreRegistrationHandler service) {
        this.registrationHandler.bind(service);
    }

    void unbindRegistrationHandler(DataStoreRegistrationHandler service) {
        this.registrationHandler.unbind(service);
    }

    /**
     * This static bootstrap create handler does not have access to the {@link ZooKeeperClusterBootstrap} state.
     * It operates on the state that it is given, which is unrelated to this component.
     */
    static class BootstrapCreateHandler {

        private final BootstrapConfiguration bootConfig;
        private final DataStoreRegistrationHandler registrationHandler;

        BootstrapCreateHandler(BootstrapConfiguration bootConfig, DataStoreRegistrationHandler registrationHandler) {
            this.bootConfig = bootConfig;
            this.registrationHandler = registrationHandler;
        }

        void bootstrapFabric(String karafName, String karafHome, CreateEnsembleOptions options) throws IOException {

            String connectionUrl = bootConfig.getConnectionUrl(options);
            registrationHandler.setRegistrationCallback(new DataStoreBootstrapTemplate(karafName, karafHome, connectionUrl, options));

            bootConfig.createOrUpdateDataStoreConfig(options);
            bootConfig.createZooKeeeperServerConfig(options);
            bootConfig.createZooKeeeperClientConfig(connectionUrl, options);
        }

        private void waitForContainerAlive(String containerName, BundleContext syscontext, long timeout) throws TimeoutException {
            System.out.println(String.format("Waiting for container: %s", containerName));

            Exception lastException = null;
            long startedAt = System.currentTimeMillis();
            while (!Thread.interrupted() && System.currentTimeMillis() < startedAt + timeout) {
                ServiceReference<FabricService> sref = syscontext.getServiceReference(FabricService.class);
                FabricService fabricService = sref != null ? syscontext.getService(sref) : null;
                try {
                    Container container = fabricService != null ? fabricService.getContainer(containerName) : null;
                    if (container != null && container.isAlive()) {
                        return;
                    } else {
                        Thread.sleep(500);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    lastException = ex;
                } catch (Exception ex) {
                    lastException = ex;
                }
            }
            TimeoutException toex = new TimeoutException("Cannot create container in time");
            if (lastException != null) {
                toex.initCause(lastException);
            }
            throw toex;
        }

        private void waitForSuccessfulDeploymentOf(String containerName, BundleContext syscontext, long timeout) throws TimeoutException {
            System.out.println(String.format("Waiting for container %s to provision.", containerName));

            Exception lastException = null;
            long startedAt = System.currentTimeMillis();
            while (!Thread.interrupted() && System.currentTimeMillis() < startedAt + timeout) {
                ServiceReference<FabricService> sref = syscontext.getServiceReference(FabricService.class);
                FabricService fabricService = sref != null ? syscontext.getService(sref) : null;
                try {
                    Container container = fabricService != null ? fabricService.getContainer(containerName) : null;
                    if (container != null && container.isAlive() && "success".equals(container.getProvisionStatus())) {
                        return;
                    } else {
                        Thread.sleep(500);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    lastException = ex;
                } catch (Exception ex) {
                    lastException = ex;
                }
            }
            TimeoutException toex = new TimeoutException("Cannot provision container in time");
            if (lastException != null) {
                toex.initCause(lastException);
            }
            throw toex;
        }
    }
}
