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
package io.fabric8.container.java;

import io.fabric8.api.Container;
import io.fabric8.api.ContainerProvider;
import io.fabric8.api.CreateChildContainerOptions;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profiles;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.Configurer;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.process.manager.Installation;
import io.fabric8.process.manager.ProcessManager;
import io.fabric8.service.child.ChildConstants;
import io.fabric8.service.child.ChildContainerController;
import io.fabric8.service.child.ChildContainerControllerFactory;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;

import java.util.Map;

/**
 */
@ThreadSafe
@Component(name = "io.fabric8.container.java.controller", label = "Fabric8 Java Container Controller", immediate = true, metatype = false)
@Service(ChildContainerControllerFactory.class)
public class ChildContainerControllerFactoryService extends AbstractComponent implements ChildContainerControllerFactory {

    @Reference
    private Configurer configurer;
    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    @Reference(referenceInterface = ProcessManager.class, cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    private final ValidatingReference<ProcessManager> processManager = new ValidatingReference<ProcessManager>();

    private ContainerInstallations installations = new ContainerInstallations();

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }


    @Override
    public ChildContainerController createController(CreateChildContainerOptions options) {
        Map<String, String> containerTypeConfig = Profiles.getOverlayConfiguration(getFabricService(), options.getProfiles(), options.getVersion(), ChildConstants.CONTAINER_TYPE_PID);
        Map<String, ?> javaContainerConfig = Profiles.getOverlayConfiguration(getFabricService(), options.getProfiles(), options.getVersion(), ChildConstants.JAVA_CONTAINER_CONFIG_PID);
        String containerType = containerTypeConfig.get(ChildConstants.PROPERTIES.CONTAINER_TYPE);
        if ((containerType != null && !containerType.equals("karaf")) || !javaContainerConfig.isEmpty()) {
            return createProcessManagerController();
        }
        return null;
    }

    @Override
    public ChildContainerController getControllerForContainer(Container container) {
        Installation installation = installations.getInstallation(container);
        ChildContainerController answer = null;
        if (installation != null) {
            answer = createProcessManagerController();
        }
        return answer;

    }

    protected ProcessManagerController createProcessManagerController() {
        return new ProcessManagerController(configurer, getProcessManager(), getFabricService(), installations);
    }

    protected ProcessManager getProcessManager() {
        return processManager.get();
    }

    FabricService getFabricService() {
        return fabricService.get();
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


}
