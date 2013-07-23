/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.fusesource.fabric.jolokia.facade.facades;

import org.codehaus.jackson.map.type.TypeFactory;
import org.fusesource.fabric.api.*;
import org.fusesource.fabric.jolokia.facade.JolokiaFabricConnector;
import org.fusesource.fabric.jolokia.facade.dto.ProfileDTO;
import org.fusesource.fabric.jolokia.facade.utils.Helpers;
import org.jolokia.client.J4pClient;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pExecResponse;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.fusesource.fabric.jolokia.facade.utils.Helpers.toList;

public class FabricServiceFacade implements FabricService {

    private JolokiaFabricConnector connector;

    public FabricServiceFacade(JolokiaFabricConnector connector) {
        this.connector = connector;
    }

    @Override
    public void trackConfiguration(Runnable runnable) {
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    @Override
    public void unTrackConfiguration(Runnable runnable) {
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    @Override
    public Container[] getContainers() {

        List<Container> containers = new ArrayList<Container>();

        try {
            J4pExecRequest request = Helpers.createExecRequest("containers(java.util.List)", toList("id"));
            J4pExecResponse response = getJolokiaClient().execute(request);
            List<Map<String, Object>> values = response.getValue();

            for (Map<String, Object> value : values) {
                containers.add(new ContainerFacade(getJolokiaClient(), (String)value.get("id")));
            }
        } catch (Exception e) {
            throw new RuntimeException ("Failed to fetch container list", e);
        }
        return containers.toArray(new Container[containers.size()]);
    }

    @Override
    public Container getContainer(String containerId) {
        return new ContainerFacade(getJolokiaClient(), containerId);
    }

    @Override
    public void startContainer(String containerId) {
        Helpers.doContainerAction(getJolokiaClient(), "start", containerId);
    }

    @Override
    public void startContainer(Container container) {
        Helpers.doContainerAction(getJolokiaClient(), "start", container.getId());
    }

    @Override
    public void stopContainer(String containerId) {
        Helpers.doContainerAction(getJolokiaClient(), "stop", containerId);
    }

    @Override
    public void stopContainer(Container container) {
        Helpers.doContainerAction(getJolokiaClient(), "stop", container.getId());
    }

    @Override
    public void destroyContainer(String containerId) {
        Helpers.doContainerAction(getJolokiaClient(), "destroy", containerId);
    }

    @Override
    public void destroyContainer(Container container) {
        Helpers.doContainerAction(getJolokiaClient(), "destroy", container.getId());
    }

    @Override
    public CreateContainerMetadata[] createContainers(CreateContainerOptions createContainerOptions) {

        Map options = Helpers.getObjectMapper().convertValue(createContainerOptions, Map.class);

        Helpers.exec(getJolokiaClient(), "createContainers(java.util.Map)", options);

        // JMX API doesn't currently return this...
        return new CreateContainerMetadata[0];
    }

    @Override
    public Version getDefaultVersion() {
        String id = Helpers.read(getJolokiaClient(), "DefaultVersion");
        return new VersionFacade(getJolokiaClient(), id);
    }

    @Override
    public void setDefaultVersion(Version version) {
        Helpers.write(getJolokiaClient(), "DefaultVersion", version.getId());
    }

    @Override
    public Version[] getVersions() {
        List<Map<String, Object>> results = Helpers.exec(getJolokiaClient(), "versions(java.util.List)", Helpers.toList("id"));
        List<Version> answer = new ArrayList<Version>();
        for (Map<String, Object> result : results) {
            answer.add(new VersionFacade(getJolokiaClient(), (String)result.get("id")));
        }
        return answer.toArray(new Version[answer.size()]);
    }

    @Override
    public Version getVersion(String versionKey) {
        return new VersionFacade(getJolokiaClient(), versionKey);
    }

    @Override
    public Version createVersion(String versionKey) {
        JSONObject obj = Helpers.exec(getJolokiaClient(), "createVersion(java.lang.String)", versionKey);
        return new VersionFacade(getJolokiaClient(), (String)obj.get("id"));
    }

    @Override
    public Version createVersion(Version version, String versionKey) {
        JSONObject obj = Helpers.exec(getJolokiaClient(), "createVersion(java.lang.String, java.lang.String)", version.getId(), versionKey);
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
        throw new UnsupportedOperationException("The method is not yet implemented.");
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
    public String getZookeeperUrl() {
        return Helpers.read(getJolokiaClient(), "ZookeeperUrl");
    }

    /* not exposed, does it really need to be? */
    @Override
    public String getZookeeperPassword() {
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    /* Not going to implement these deprecated methods */
    /**
     * @deprecated
     */
    @Override
    public Profile[] getProfiles(String s) {
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    /**
     * @deprecated
     */
    @Override
    public Profile getProfile(String s, String s2) {
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    /**
     * @deprecated
     */
    @Override
    public Profile createProfile(String s, String s2) {
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    /**
     * @deprecated
     */
    @Override
    public void deleteProfile(Profile profile) {
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    @Override
    public Container getCurrentContainer() {
        JSONObject obj = Helpers.exec(getJolokiaClient(), "currentContainer()");
        return new ContainerFacade(getJolokiaClient(), (String)obj.get("id"));
    }

    @Override
    public String getCurrentContainerName() {
        return Helpers.read(getJolokiaClient(), "CurrentContainerName");
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
    public void setRequirements(FabricRequirements fabricRequirements) throws IOException {
        throw new UnsupportedOperationException("The method is not yet implemented.");
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
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    @Override
    public PortService getPortService() {
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    @Override
    public DataStore getDataStore() {
        throw new UnsupportedOperationException("The method is not yet implemented.");
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
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    private J4pClient getJolokiaClient() {
        return connector.getJolokiaClient();
    }
}
