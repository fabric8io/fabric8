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
package org.jboss.arquillian.container.fabric8.docker;

import io.fabric8.testkit.FabricAssertions;
import io.fabric8.testkit.FabricController;
import io.fabric8.testkit.support.CommandLineFabricControllerManager;
import io.fabric8.testkit.support.FabricControllerManagerSupport;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

/**
 * Creates a fabric8 container using a remote process using a distribution of fabric8 and running a shell
 * command to create it (so we then test the distribution actually works ;) then we create a {@link FabricController}
 * to be able to interact with the fabric in a test case.
 */
public class Fabric8DockerContainer implements DeployableContainer<Fabric8DockerContainerConfiguration> {
    @Inject
    @ApplicationScoped
    private InstanceProducer<Fabric8DockerContainerConfiguration> configuration;

    @Inject
    @ApplicationScoped
    private InstanceProducer<FabricController> controller;

    private FabricControllerManagerSupport fabricControllerManager;

    @Override
    public Class<Fabric8DockerContainerConfiguration> getConfigurationClass() {
        return Fabric8DockerContainerConfiguration.class;
    }

    @Override
    public void setup(Fabric8DockerContainerConfiguration configuration) {
        this.configuration.set(configuration);
    }

    @Override
    public void start() throws LifecycleException {
        // lets kill any containers that are running before we start
        FabricAssertions.killJavaAndDockerProcesses();

        Fabric8DockerContainerConfiguration config = configuration.get();

        fabricControllerManager = createFabricControllerManager(config);

        config.configure(fabricControllerManager);

        try {
            FabricController fabricController = FabricAssertions.assertFabricCreate(fabricControllerManager);
            controller.set(fabricController);
        } catch (Exception e) {
            throw new LifecycleException("Failed to create fabric: " + e, e);
        }

        System.out.println("Created a controller " + controller.get());
    }

    @Override
    public void stop() throws LifecycleException {
        if (fabricControllerManager != null) {
            try {
                fabricControllerManager.destroy();
            } catch (Exception e) {
                throw new LifecycleException("Failed to stop remote fabric: " + e, e);
            } finally {
                fabricControllerManager = null;

                // lets kill any containers that are running so we leave things in a nice clean state
                FabricAssertions.killJavaAndDockerProcesses();
            }
        }
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        // TODO
        return null;
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        // TODO
        return null;
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        // TODO

    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        // TODO

    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        // TODO

    }


    public FabricController getController() {
        return controller.get();
    }


    protected FabricControllerManagerSupport createFabricControllerManager(Fabric8DockerContainerConfiguration config) {
        return new DockerFabricControllerManager(config
        );
    }

}
