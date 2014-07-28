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
package io.fabric8.jolokia.facade.facades;

import io.fabric8.api.*;
import io.fabric8.jolokia.facade.JolokiaFabricConnector;
import io.fabric8.jolokia.facade.utils.Helpers;

import org.jolokia.client.J4pClient;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pExecResponse;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.lang.Override;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.fabric8.jolokia.facade.utils.Helpers.toList;

public class FabricServiceFacade implements FabricService {

    private JolokiaFabricConnector connector;

    public FabricServiceFacade(JolokiaFabricConnector connector) {
        this.connector = connector;
    }

    @Override
    public <T> T adapt(Class<T> type) {
        return null;
    }

    @Override
    public void trackConfiguration(Runnable runnable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void untrackConfiguration(Runnable callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Container[] getContainers() {
        List<Container> containers = new ArrayList<Container>();
        try {
            J4pExecRequest request = Helpers.createExecRequest("containers(java.util.List)", toList("id"));
            J4pExecResponse response = getJolokiaClient().execute(request);
            List<Map<String, Object>> values = response.getValue();

            for (Map<String, Object> value : values) {
                containers.add(new ContainerFacade(this, getJolokiaClient(), (String)value.get("id")));
            }
        } catch (Exception e) {
            throw new RuntimeException ("Failed to fetch container list", e);
        }
        return containers.toArray(new Container[containers.size()]);
    }

    @Override
	public Container[] getAssociatedContainers(String versionId, String profileId) {
        throw new UnsupportedOperationException();
	}

	@Override
    public Container getContainer(String containerId) {
        return new ContainerFacade(this, getJolokiaClient(), containerId);
    }

    @Override
    public void startContainer(String containerId) {
        Helpers.doContainerAction(getJolokiaClient(), "start", containerId);
    }

    @Override
    public void startContainer(String containerId, boolean force) {
        startContainer(containerId);
    }

    @Override
    public void startContainer(Container container) {
        startContainer(container.getId());
    }

    @Override
    public void startContainer(Container container, boolean force) {
        startContainer(container.getId(), force);
    }

    @Override
    public void stopContainer(String containerId) {
        Helpers.doContainerAction(getJolokiaClient(), "stop", containerId);
    }

    @Override
    public void stopContainer(String containerId, boolean force) {
        stopContainer(containerId);
    }

    @Override
    public void stopContainer(Container container) {
        stopContainer(container.getId());
    }

    @Override
    public void stopContainer(Container container, boolean force) {
        stopContainer(container.getId());
    }

    @Override
    public void destroyContainer(String containerId) {
        Helpers.doContainerAction(getJolokiaClient(), "destroy", containerId);
    }

    @Override
    public void destroyContainer(String containerId, boolean force) {
        destroyContainer(containerId);
    }

    @Override
    public void destroyContainer(Container container) {
        destroyContainer(container.getId());
    }

    @Override
    public void destroyContainer(Container container, boolean force) {
        destroyContainer(container.getId());
    }

    @Override
    public CreateContainerMetadata[] createContainers(CreateContainerOptions createContainerOptions) {
        Map options = Helpers.getObjectMapper().convertValue(createContainerOptions, Map.class);

        Helpers.exec(getJolokiaClient(), "createContainers(java.util.Map)", options);

        // JMX API doesn't currently return this...
        return new CreateContainerMetadata[0];
    }

    @Override
    public CreateContainerMetadata[] createContainers(CreateContainerOptions createContainerOptions, CreationStateListener listener) {
        return createContainers(createContainerOptions);
    }

    @Override
    public Set<Class<? extends CreateContainerBasicOptions>> getSupportedCreateContainerOptionTypes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Class<? extends CreateContainerBasicMetadata>> getSupportedCreateContainerMetadataTypes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDefaultVersionId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Version getRequiredDefaultVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Version getDefaultVersion() {
        String id = Helpers.read(getJolokiaClient(), "DefaultVersion");
        return new VersionFacade(getJolokiaClient(), id);
    }

    @Override
    public void setDefaultVersionId(String versionId) {
        Helpers.write(getJolokiaClient(), "DefaultVersion", versionId);
    }

    public Version getVersion(String versionKey) {
        return new VersionFacade(getJolokiaClient(), versionKey);
    }

    @Override
    public URI getMavenRepoURI() {
        String uri = Helpers.read(getJolokiaClient(), "MavenRepoURI");
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<URI> getMavenRepoURIs() {
        throw new UnsupportedOperationException();
    }

    @Override
    public URI getMavenRepoUploadURI() {
        String uri = Helpers.read(getJolokiaClient(), "MavenRepoUploadURI");
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getRestAPI() {
        return Helpers.read(getJolokiaClient(), "RestAPI");
    }

    @Override
    public String getGitUrl() {
        return Helpers.read(getJolokiaClient(), "GitUrl");
    }

    @Override
    public String getWebConsoleUrl() {
        return Helpers.read(getJolokiaClient(), "WebConsoleUrl");
    }

    @Override
    public String getZookeeperUrl() {
        return Helpers.read(getJolokiaClient(), "ZookeeperUrl");
    }

    /* not exposed, does it really need to be? */
    @Override
    public String getZookeeperPassword() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Container getCurrentContainer() {
        JSONObject obj = Helpers.exec(getJolokiaClient(), "currentContainer()");
        return new ContainerFacade(this, getJolokiaClient(), (String)obj.get("id"));
    }

    @Override
    public String getCurrentContainerName() {
        return Helpers.read(getJolokiaClient(), "CurrentContainerName");
    }

    @Override
    public String getEnvironment() {
        return Helpers.read(getJolokiaClient(), "Environment");
    }

    @Override
    public FabricRequirements getRequirements() {
        JSONObject obj = Helpers.exec(getJolokiaClient(), "requirements()");
        FabricRequirements requirements = null;
        try {
            requirements = Helpers.getObjectMapper().readValue(obj.toJSONString(), FabricRequirements.class);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return requirements;
    }

    @Override
    public AutoScaleStatus getAutoScaleStatus() {
        JSONObject obj = Helpers.exec(getJolokiaClient(), "autoScaleStatus()");
        AutoScaleStatus answer = null;
        try {
            answer = Helpers.getObjectMapper().readValue(obj.toJSONString(), AutoScaleStatus.class);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return answer;
    }

    @Override
    public void setRequirements(FabricRequirements fabricRequirements) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FabricStatus getFabricStatus() {
        JSONObject obj = Helpers.exec(getJolokiaClient(), "fabricStatus()");
        FabricStatus status = null;
        try {
            Map<String, ProfileStatus> profStats = Helpers.getObjectMapper().readValue(obj.toJSONString(), Map.class);
            status = new FabricStatus();
            status.setProfileStatusMap(profStats);
            status.setService(this);
            status.init();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return status;
    }

    /* these are separate OSGi services, don't think we need to worry about these */
    @Override
    public PatchService getPatchService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PortService getPortService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDefaultJvmOptions() {
        return Helpers.read(getJolokiaClient(), "DefaultJvmOptions");
    }

    @Override
    public void setDefaultJvmOptions(String s) {
        Helpers.write(getJolokiaClient(), "DefaultJvmOptions", s);
    }

    @Override
    public String containerWebAppURL(String s, String s2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String profileWebAppURL(String webAppId, String profileId, String versionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getConfigurationValue(String versionId, String profileId, String pid, String key) {
        return Helpers.exec(getJolokiaClient(), "getConfigurationValue(java.lang.String,java.lang.String,java.lang.String,java.lang.String)", versionId, profileId, pid, key);
    }

    @Override
    public void setConfigurationValue(String versionId, String profileId, String pid, String key, String value) {
        Helpers.exec(getJolokiaClient(), "setConfigurationValue(java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String)", versionId, profileId, pid, key, value);
    }

    private J4pClient getJolokiaClient() {
        return connector.getJolokiaClient();
    }

    @Override
    public boolean scaleProfile(String profile, int numberOfInstances) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ContainerAutoScaler createContainerAutoScaler(FabricRequirements requirements, ProfileRequirements profileRequirements) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ContainerProvider getProvider(String scheme) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getZooKeeperUser() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Map<String, String>> substituteConfigurations(Map<String, Map<String, String>> configurations) {
        throw new UnsupportedOperationException();
    }
}
