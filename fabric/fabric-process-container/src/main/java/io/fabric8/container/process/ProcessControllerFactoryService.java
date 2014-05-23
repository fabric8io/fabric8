/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.container.process;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.fabric8.api.Container;
import io.fabric8.api.CreateChildContainerOptions;
import io.fabric8.api.CreateContainerBasicOptions;
import io.fabric8.api.DataStore;
import io.fabric8.api.FabricService;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.Configurer;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.common.util.Objects;
import io.fabric8.process.manager.Installation;
import io.fabric8.process.manager.ProcessManager;
import io.fabric8.service.child.ChildConstants;
import io.fabric8.service.child.ChildContainerController;
import io.fabric8.service.child.ChildContainers;
import io.fabric8.service.child.ProcessControllerFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 */
@ThreadSafe
@Component(name = "io.fabric8.container.process.controller", label = "Fabric8 Child Process Container Controller",
        policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = true)
@Service(ProcessControllerFactory.class)
public class ProcessControllerFactoryService extends AbstractComponent implements ProcessControllerFactory {
    private static final transient Logger LOG = LoggerFactory.getLogger(ProcessControllerFactoryService.class);

    private static final int DEFAULT_EXTERNAL_PORT = 9000;

    @Reference
    private Configurer configurer;

    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    @Reference(referenceInterface = CuratorFramework.class, bind = "bindCurator", unbind = "unbindCurator")
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();

    @Reference(referenceInterface = ProcessManager.class)
    private final ValidatingReference<ProcessManager> processManager = new ValidatingReference<ProcessManager>();

    @Property(name = "monitorPollTime", longValue = 1500,
            label = "Monitor poll period",
            description = "The number of milliseconds after which the processes will be polled to check they are started and still alive.")
    private long monitorPollTime = 1500;

    private int externalJolokiaPort;
    private int externalPortCounter;
    private int[] containerLocalIp4Address = {127, 0, 0, 0};

    private Timer keepAliveTimer;

    protected final Runnable configurationChangeHandler = new Runnable() {
        @Override
        public void run() {
            onConfigurationChanged();
        }
    };

    @Activate
    void activate() {
        activateComponent();
        keepAliveTimer = new Timer("fabric8-process-container-monitor");

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                checkProcessesStatus();
            }
        };
        keepAliveTimer.schedule(timerTask, monitorPollTime, monitorPollTime);


        DataStore dataStore = getDataStore();
        if (dataStore != null) {
            dataStore.trackConfiguration(configurationChangeHandler);
        }
    }

    @Deactivate
    void deactivate() {
        DataStore dataStore = getDataStore();
        if (dataStore != null) {
            dataStore.untrackConfiguration(configurationChangeHandler);
        }
        if (keepAliveTimer != null) {
            keepAliveTimer.cancel();
            keepAliveTimer = null;
        }
        deactivateComponent();
    }


    @Override
    public ChildContainerController createController(CreateChildContainerOptions options) {
        FabricService fabric = getFabricService();
        boolean isJavaOrProcessContainer = ChildContainers.isJavaOrProcessContainer(fabric, options);
        if (isJavaOrProcessContainer) {
            return createProcessManagerController();
        }
        return null;
    }

    @Override
    public ChildContainerController getControllerForContainer(Container container) {
        Installation installation = getProcessManager().getInstallation(container.getId());
        ChildContainerController answer = null;
        if (installation != null) {
            answer = createProcessManagerController();
        }
        return answer;
    }

    /**
     * Allocates a new jolokia port for the given container ID
     *
     * @param containerId
     * @return
     */
    public synchronized int createJolokiaPort(String containerId) {
        FabricService fabricService = getFabricService();
        Container currentContainer = fabricService.getCurrentContainer();
        Set<Integer> usedPortByHost = fabricService.getPortService().findUsedPortByHost(currentContainer);

        while (true) {
            if (externalJolokiaPort <= 0) {
                externalJolokiaPort = JolokiaAgentHelper.DEFAULT_JOLOKIA_PORT;
            } else {
                externalJolokiaPort++;
            }
            if (!usedPortByHost.contains(externalJolokiaPort)) {
                Container container = fabricService.getCurrentContainer();
                String pid = JolokiaAgentHelper.JOLOKIA_PORTS_PID;
                String key = containerId;
                fabricService.getPortService().registerPort(container, pid, key, externalJolokiaPort);
                return externalJolokiaPort;
            }
        }
    }

    /**
     * Allocates a new external port for the given containerId and portKey
     */
    public synchronized int createExternalPort(String containerId, String portKey, Set<Integer> usedPortByHost, CreateContainerBasicOptions options) {
        while (true) {
            if (externalPortCounter <= 0) {
                externalPortCounter = options.getMinimumPort();
                if (externalPortCounter == 0) {
                    externalPortCounter = DEFAULT_EXTERNAL_PORT;
                }
            } else {
                externalPortCounter++;
            }
            if (!usedPortByHost.contains(externalPortCounter)) {
                Container container = getFabricService().getCurrentContainer();
                String pid = ChildConstants.PORTS_PID;
                String key = containerId + "-" + portKey;
                getFabricService().getPortService().registerPort(container, pid, key, externalPortCounter);
                return externalPortCounter;
            }
        }
    }

    /**
     * Creates a new local container address such as 127.0.0.1, 127.0.0.2, 127.0.0.3 etc so each container can have its own local address
     * for example when working with Cassandra; it allow the same ports to be used but on different addresses.
     */
    public synchronized String createContainerLocalAddress(String containerId, CreateContainerBasicOptions options) {
        for (int i = containerLocalIp4Address.length - 1; i >= 0; i--) {
            int counter = ++containerLocalIp4Address[i];
            if (counter > 255) {
                containerLocalIp4Address[i] = 0;
            } else {
                break;
            }
        }
        StringBuilder builder = new StringBuilder();
        for (int counter : containerLocalIp4Address) {
            if (builder.length() > 0) {
                builder.append(".");
            }
            builder.append("" + counter);
        }
        return builder.toString();
    }

    protected void onConfigurationChanged() {
        ProcessManager manager = getProcessManager();
        FabricService fabric = getFabricService();
        if (manager != null && fabric != null) {
            ImmutableMap<String, Installation> map = manager.listInstallationMap();
            ImmutableSet<Map.Entry<String, Installation>> entries = map.entrySet();
            for (Map.Entry<String, Installation> entry : entries) {
                String id = entry.getKey();
                Installation installation = entry.getValue();
                try {
                    Container container = null;
                    try {
                        container = fabric.getContainer(id);
                    } catch (Exception e) {
                        LOG.debug("No container for id: " + id + ". " + e, e);
                    }
                    if (container != null && installation != null) {
                        ChildContainerController controllerForContainer = getControllerForContainer(container);
                        if (controllerForContainer instanceof ProcessManagerController) {
                            ProcessManagerController processManagerController = (ProcessManagerController) controllerForContainer;
                            processManagerController.updateInstallation(container, installation);
                        }
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to get PID for process " + id + ". " + e, e);
                }
            }
        }

    }

    protected ProcessManagerController createProcessManagerController() {
        return new ProcessManagerController(this, configurer, getProcessManager(), getFabricService(), getCuratorFramework());
    }

    CuratorFramework getCuratorFramework() {
        return curator.get();
    }

    protected ProcessManager getProcessManager() {
        return processManager.get();
    }

    FabricService getFabricService() {
        return fabricService.get();
    }

    protected DataStore getDataStore() {
        FabricService service = getFabricService();
        if (service != null) {
            return service.getDataStore();
        }
        return null;
    }

    void bindConfigurer(Configurer configurer) {
        this.configurer = configurer;
    }

    void unbindConfigurer(Configurer configurer) {
        this.configurer = null;
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }

    void bindProcessManager(ProcessManager processManager) {
        this.processManager.bind(processManager);
    }

    void unbindProcessManager(ProcessManager processManager) {
        this.processManager.unbind(processManager);
    }


    protected void checkProcessesStatus() {
        ProcessManager manager = getProcessManager();
        FabricService fabric = getFabricService();
        if (manager != null && fabric != null) {
            ImmutableMap<String, Installation> map = manager.listInstallationMap();
            ImmutableSet<Map.Entry<String, Installation>> entries = map.entrySet();
            for (Map.Entry<String, Installation> entry : entries) {
                String id = entry.getKey();
                Installation installation = entry.getValue();
                try {
                    Container container = null;
                    try {
                        container = fabric.getContainer(id);
                    } catch (Exception e) {
                        LOG.debug("No container for id: " + id + ". " + e, e);
                    }
                    if (container != null) {
                        Long pid = installation.getActivePid();
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Polling container " + id + " for its PID");
                        }
                        if (pid == null) {
                            if (container.isAlive()) {
                                container.setAlive(false);
                            }
                        } else if (pid != null && pid != 0) {
                            if (!container.isAlive()) {
                                container.setAlive(true);
                            }
                            if (!Objects.equal(container.getProvisionResult(), Container.PROVISION_SUCCESS)) {
                                container.setProvisionResult(Container.PROVISION_SUCCESS);
                            }

                            JolokiaAgentHelper.jolokiaKeepAliveCheck(fabric, container);
                        }
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to get PID for process " + id + ". " + e, e);
                }
            }
        }
    }
}
