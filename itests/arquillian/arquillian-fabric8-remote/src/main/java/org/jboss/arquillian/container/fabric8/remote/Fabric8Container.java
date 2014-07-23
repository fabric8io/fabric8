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
package org.jboss.arquillian.container.fabric8.remote;

import io.fabric8.testkit.FabricAssertions;
import io.fabric8.testkit.FabricController;
import io.fabric8.testkit.support.CommandLineFabricControllerManager;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

import java.io.File;

/**
 */
public class Fabric8Container implements DeployableContainer<Fabric8ContainerConfiguration> {
    static FabricController dirtyHack;

    @Inject
    @ContainerScoped
    private InstanceProducer<Fabric8ContainerConfiguration> configuration;

    @Inject
    @ApplicationScoped
    private InstanceProducer<FabricController> controller;

    private CommandLineFabricControllerManager fabricControllerManager;

    @Override
    public Class<Fabric8ContainerConfiguration> getConfigurationClass() {
        return Fabric8ContainerConfiguration.class;
    }

    @Override
    public void setup(Fabric8ContainerConfiguration configuration) {
        this.configuration.set(configuration);
    }

    @Override
    public void start() throws LifecycleException {
        fabricControllerManager = new CommandLineFabricControllerManager();

        // TODO use the test case name to determine where to install...
/*
        File baseDir = getBaseDir();
        String canonicalName = getClass().getCanonicalName();
        File workDir = new File(baseDir, "target/fabricInstall/" + canonicalName);
        fabricController.setWorkDirectory(workDir);
*/
        try {
            FabricController fabricController = FabricAssertions.assertFabricCreate(fabricControllerManager);
            dirtyHack = fabricController;
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

    public static File getBaseDir() {
        return new File(System.getProperty("basedir", "."));
    }

    public FabricController getController() {
        return controller.get();
    }
}
