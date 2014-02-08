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

import io.fabric8.api.Container;
import io.fabric8.api.CreateEnsembleOptions;
import io.fabric8.api.DataStoreRegistrationHandler;
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricService;
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
import org.osgi.util.tracker.ServiceTracker;

/**
 * ZooKeeperClusterBootstrap
 * |_ ConfigurationAdmin
 * |_ DataStoreRegistrationHandler (@see DataStoreManager)
 * |_ BootstrapConfiguration (@see BootstrapConfiguration)
 * |_ FabricService (optional,unary) (@see FabricServiceImpl)
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
    void activate(ComponentContext componentContext, Map<String, ?> configuration) throws Exception {
        bundleContext = componentContext.getBundleContext();
        configurer.configure(configuration, this);
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
            stopBundles();

            DataStoreRegistrationHandler regHandler = registrationHandler.get();
            BootstrapConfiguration bootConfig = bootstrapConfiguration.get();
            if (options.isClean()) {
                bootConfig = cleanInternal(bootConfig, regHandler);
            }

            BootstrapCreateHandler createHandler = new BootstrapCreateHandler(bootConfig, regHandler);
            createHandler.bootstrapFabric(name, home, options);

            startBundles(options);

            // Track the {@link FabricService} and wait for it to come up
            final CountDownLatch fabricServiceLatch = new CountDownLatch(1);
            final AtomicReference<FabricService> fabricServiceRef = new AtomicReference<FabricService>();
            ServiceTracker<?, ?> tracker = new ServiceTracker<FabricService, FabricService>(bundleContext, FabricService.class, null) {
                @Override
                public FabricService addingService(ServiceReference<FabricService> reference) {
                    FabricService fabricService = super.addingService(reference);
                    fabricServiceRef.set(fabricService);
                    fabricServiceLatch.countDown();
                    return fabricService;
                }
            };
            tracker.open();
            try {
                if (!fabricServiceLatch.await(60, TimeUnit.SECONDS))
                    throw new TimeoutException("Cannot obtain FabricService service");
            } finally {
                tracker.close();
            }

            FabricService fabricService = fabricServiceRef.get();
            if (!createHandler.waitForContainerAlive(name, fabricService, 30000L)) {
                throw new TimeoutException("Cannot create container in time");
            }

            if (options.isWaitForProvision() && options.isAgentEnabled()) {
                if (!createHandler.waitForSuccessfulDeploymentOf(name, fabricService, options.getProvisionTimeout())) {
                    throw new TimeoutException("Cannot provision container in time");
                }
            }
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception ex) {
            throw new FabricException("Unable to create zookeeper server configuration", ex);
        }
    }

    private BootstrapConfiguration cleanInternal(BootstrapConfiguration bootConfig, DataStoreRegistrationHandler registrationHandler) {
        try {
            Configuration[] configs = configAdmin.get().listConfigurations("(|(service.factoryPid=io.fabric8.zookeeper.server)(service.pid=io.fabric8.zookeeper))");
            File karafData = new File(data);

            // Setup the unregistration listener for {@link BootstrapConfiguration}
            final CountDownLatch unregisterLatch = new CountDownLatch(1);
            ServiceListener listener = new ServiceListener() {
                @Override
                public void serviceChanged(ServiceEvent event) {
                    if (event.getType() == ServiceEvent.UNREGISTERING) {
                        bundleContext.removeServiceListener(this);
                        unregisterLatch.countDown();
                    }
                }
            };
            bundleContext.addServiceListener(listener, "(objectClass=" + BootstrapConfiguration.class.getName() + ")");

            // Disable the {@link BootstrapConfiguration} component and await the unregistration of the respective service
            ComponentContext componentContext = bootConfig.getComponentContext();
            componentContext.disableComponent(BootstrapConfiguration.COMPONENT_NAME);
            if (!unregisterLatch.await(10, TimeUnit.SECONDS))
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
                        bundleContext.removeServiceListener(this);
                        sref.set(event.getServiceReference());
                        registerLatch.countDown();
                    }
                }
            };
            bundleContext.addServiceListener(listener, "(objectClass=" + BootstrapConfiguration.class.getName() + ")");

            // Enable the {@link BootstrapConfiguration} component and await the registration of the respective service
            componentContext.enableComponent(BootstrapConfiguration.COMPONENT_NAME);
            if (!registerLatch.await(10, TimeUnit.SECONDS))
                throw new TimeoutException("Timeout for registering BootstrapConfiguration service");

            return (BootstrapConfiguration) bundleContext.getService(sref.get());

        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception e) {
            throw new FabricException("Unable to delete zookeeper configuration", e);
        }
    }

    private void cleanConfigurations(Configuration[] configs) throws IOException, InvalidSyntaxException {
        if (configs != null && configs.length > 0) {
            for (Configuration config : configs) {
                config.delete();
            }
        }
    }

    private void cleanZookeeperDirectory(File karafData) throws IOException, InvalidSyntaxException {
        File zkDir = new File(karafData, "zookeeper");
        if (zkDir.isDirectory()) {
            File newZkDir = new File(karafData, "zookeeper." + System.currentTimeMillis());
            if (!zkDir.renameTo(newZkDir)) {
                newZkDir = zkDir;
            }
            delete(newZkDir);
        }
    }

    private void cleanGitDirectory(File karafData) {
        File gitDir = new File(karafData, "git");
        if (gitDir.isDirectory()) {
            delete(gitDir);
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

        private boolean waitForContainerAlive(String containerName, FabricService fabricService, long timeout) throws InterruptedException {
            System.out.println(String.format("Waiting for container: %s", containerName));

            boolean success = false;
            long startedAt = System.currentTimeMillis();
            while (!Thread.interrupted() && !success && System.currentTimeMillis() < startedAt + timeout) {
                Container container = fabricService.getContainer(containerName);
                success = container != null && container.isAlive();
                if (!success) {
                    Thread.sleep(500);
                }
            }
            return success;
        }

        private boolean waitForSuccessfulDeploymentOf(String containerName, FabricService fabricService, long timeout) throws InterruptedException {
            System.out.println(String.format("Waiting for container %s to provision.", containerName));

            boolean success = false;
            long startedAt = System.currentTimeMillis();
            while (!Thread.interrupted() && !success && System.currentTimeMillis() < startedAt + timeout) {
                Container container = fabricService.getContainer(containerName);
                if (container != null && container.isAlive()) {
                    String lastStatus = container.getProvisionStatus();
                    success = "success".equals(lastStatus);
                }
                if (!success) {
                    Thread.sleep(500);
                }
            }
            return success;
        }
    }
}
