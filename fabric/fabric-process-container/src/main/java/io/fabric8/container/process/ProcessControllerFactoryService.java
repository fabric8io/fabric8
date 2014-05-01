/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.container.process;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.fabric8.api.Container;
import io.fabric8.api.CreateChildContainerOptions;
import io.fabric8.api.FabricService;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.Configurer;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.common.util.Objects;
import io.fabric8.process.manager.Installation;
import io.fabric8.process.manager.ProcessManager;
import io.fabric8.service.child.ChildContainerController;
import io.fabric8.service.child.ChildContainers;
import io.fabric8.service.child.ProcessControllerFactory;
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

    @Reference
    private Configurer configurer;

    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    @Reference(referenceInterface = ProcessManager.class)
    private final ValidatingReference<ProcessManager> processManager = new ValidatingReference<ProcessManager>();

    @Property(name = "monitorPollTime", longValue = 1500,
            label = "Monitor poll period",
            description = "The number of milliseconds after which the processes will be polled to check they are started and still alive.")
    private long monitorPollTime = 1500;

    private int externalPortCounter;

    private Timer keepAliveTimer;

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
    }

    @Deactivate
    void deactivate() {
        if (keepAliveTimer != null) {
            keepAliveTimer.cancel();
            keepAliveTimer = null;
        }
        deactivateComponent();
    }


    @Override
    public ChildContainerController createController(CreateChildContainerOptions options) {
        boolean isJavaContainer = ChildContainers.isJavaContainer(getFabricService(), options);
        boolean isProcessContainer = ChildContainers.isProcessContainer(getFabricService(), options);
        if (isProcessContainer || isJavaContainer) {
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
     * @param containerId
     * @return
     */
    public synchronized int createJolokiaPort(String containerId) {
        FabricService fabricService = getFabricService();
        Container currentContainer = fabricService.getCurrentContainer();
        Set<Integer> usedPortByHost = fabricService.getPortService().findUsedPortByHost(currentContainer);

        while (true) {
            if (externalPortCounter <= 0) {
                externalPortCounter = JolokiaAgentHelper.DEFAULT_JOLOKIA_PORT;
            } else {
                externalPortCounter++;
            }
            if (!usedPortByHost.contains(externalPortCounter)) {
                Container container = fabricService.getCurrentContainer();
                String pid = JolokiaAgentHelper.JOLOKIA_PORTS_PID;
                String key = containerId;
                fabricService.getPortService().registerPort(container, pid, key, externalPortCounter);
                return externalPortCounter;
            }
        }
    }


    protected ProcessManagerController createProcessManagerController() {
        return new ProcessManagerController(this, configurer, getProcessManager(), getFabricService());
    }

    protected ProcessManager getProcessManager() {
        return processManager.get();
    }

    FabricService getFabricService() {
        return fabricService.get();
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
