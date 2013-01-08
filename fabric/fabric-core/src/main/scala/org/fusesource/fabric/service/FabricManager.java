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

import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.ContainerProvider;
import org.fusesource.fabric.api.CreateContainerMetadata;
import org.fusesource.fabric.api.CreateContainerOptions;
import org.fusesource.fabric.api.FabricRequirements;
import org.fusesource.fabric.api.FabricStatus;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 */
public class FabricManager implements FabricManagerMBean {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabricManager.class);

    private final FabricServiceImpl fabricService;
    private ObjectName objectName;

    public FabricManager(FabricServiceImpl fabricService) {
        this.fabricService = fabricService;
    }

    public ObjectName getObjectName() throws MalformedObjectNameException {
        if (objectName == null) {
            // TODO to avoid mbean clashes if ever a JVM had multiple FabricService instances, we may
            // want to add a parameter of the fabric ID here...
            objectName = new ObjectName("org.fusesource.fabric:type=Fabric");
        }
        return objectName;
    }

    public void setObjectName(ObjectName objectName) {
        this.objectName = objectName;
    }

    public void registerMBeanServer(MBeanServer mbeanServer) {
        try {
            ObjectName name = getObjectName();
            ObjectInstance objectInstance = mbeanServer.registerMBean(this, name);
        } catch (Exception e) {
            LOG.warn("An error occured during mbean server registration: " + e, e);
        }
    }

    public void unregisterMBeanServer(MBeanServer mbeanServer) {
        if (mbeanServer != null) {
            try {
                ObjectName name = getObjectName();
                mbeanServer.unregisterMBean(name);
            } catch (Exception e) {
                LOG.warn("An error occured during mbean server registration: " + e, e);
            }
        }
    }

    protected FabricServiceImpl getFabricService() {
        return fabricService;
    }


    // Management API
    //-------------------------------------------------------------------------


    @Override
    public CreateContainerMetadata[] createContainers(CreateContainerOptions options) {
        return getFabricService().createContainers(options);
    }

    @Override
    public Profile createProfile(String version, String name) {
        return getFabricService().createProfile(version, name);
    }

    @Override
    public Version createVersion(Version parent, String toVersion) {
        return getFabricService().createVersion(parent, toVersion);
    }

    @Override
    public Version createVersion(String version) {
        return getFabricService().createVersion(version);
    }

    @Override
    public void deleteProfile(Profile profile) {
        getFabricService().deleteProfile(profile);
    }

    @Override
    public void deleteVersion(String version) {
        getFabricService().deleteVersion(version);
    }

    @Override
    public void destroyContainer(Container container) {
        getFabricService().destroyContainer(container);
    }

    @Override
    public Container getContainer(String name) {
        return getFabricService().getContainer(name);
    }

    @Override
    public Container[] containers() {
        return getFabricService().getContainers();
    }

    @Override
    public ContainerTemplate getContainerTemplate(Container container, String jmxUser, String jmxPassword) {
        return getFabricService().getContainerTemplate(container, jmxUser, jmxPassword);
    }


    @Override
    public Container currentContainer() {
        return getFabricService().getCurrentContainer();
    }


    @Override
    public String getCurrentContainerName() {
        return getFabricService().getCurrentContainerName();
    }


    @Override
    public String getDefaultJvmOptions() {
        return getFabricService().getDefaultJvmOptions();
    }

    @Override
    public String getDefaultRepo() {
        return getFabricService().getDefaultRepo();
    }


    @Override
    public Version defaultVersion() {
        return getFabricService().getDefaultVersion();
    }


    @Override
    public FabricStatus fabricStatus() {
        return getFabricService().getFabricStatus();
    }


    @Override
    public String getMavenRepoUploadURI() {
        URI answer = getFabricService().getMavenRepoUploadURI();
        return (answer != null) ? answer.toString() : null;
    }


    @Override
    public String getMavenRepoURI() {
        URI answer = getFabricService().getMavenRepoURI();
        return (answer != null) ? answer.toString() : null;
    }

/*
    
    public PatchService patchService() {
        return getFabricService().getPatchService();
    }
*/


    @Override
    public Profile getProfile(String version, String name) {
        return getFabricService().getProfile(version, name);
    }


    @Override
    public Profile[] getProfiles(String version) {
        return getFabricService().getProfiles(version);
    }

    @Override
    public ContainerProvider getProvider(Container container) {
        return getFabricService().getProvider(container);
    }

    @Override
    public ContainerProvider getProvider(String scheme) {
        return getFabricService().getProvider(scheme);
    }

    @Override
    public Map<String, ContainerProvider> providers() {
        return getFabricService().getProviders();
    }


    @Override
    public FabricRequirements requirements() {
        return getFabricService().getRequirements();
    }

    @Override
    public Version getVersion(String name) {
        return getFabricService().getVersion(name);
    }

    @Override
    public Version[] versions() {
        return getFabricService().getVersions();
    }

/*
    public IZKClient getZooKeeper() {
        return getFabricService().getZooKeeper();
    }
*/

    @Override
    public String getZookeeperInfo(String name) {
        return getFabricService().getZookeeperInfo(name);
    }

/*
    public String getZookeeperPassword() {
        return getFabricService().getZookeeperPassword();
    }
*/

    @Override
    public String getZookeeperUrl() {
        return getFabricService().getZookeeperUrl();
    }

    @Override
    public void registerProvider(ContainerProvider provider, Map<String, Object> properties) {
        getFabricService().registerProvider(provider, properties);
    }

    @Override
    public void registerProvider(String scheme, ContainerProvider provider) {
        getFabricService().registerProvider(scheme, provider);
    }

    @Override
    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        getFabricService().setConfigurationAdmin(configurationAdmin);
    }


    @Override
    public void setDefaultJvmOptions(String jvmOptions) {
        getFabricService().setDefaultJvmOptions(jvmOptions);
    }

    @Override
    public void setDefaultRepo(String defaultRepo) {
        getFabricService().setDefaultRepo(defaultRepo);
    }


    @Override
    public void setDefaultVersion(Version version) {
        getFabricService().setDefaultVersion(version);
    }


    @Override
    public void setRequirements(FabricRequirements requirements) throws IOException {
        getFabricService().setRequirements(requirements);
    }

    @Override
    public void setZooKeeper(IZKClient zooKeeper) {
        getFabricService().setZooKeeper(zooKeeper);
    }

    @Override
    public void startContainer(Container container) {
        getFabricService().startContainer(container);
    }

    @Override
    public void stopContainer(Container container) {
        getFabricService().stopContainer(container);
    }

    @Override
    public void unregisterProvider(ContainerProvider provider, Map<String, Object> properties) {
        getFabricService().unregisterProvider(provider, properties);
    }

    @Override
    public void unregisterProvider(String scheme) {
        getFabricService().unregisterProvider(scheme);
    }
}
