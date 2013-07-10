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
package org.fusesource.fabric.jolokia.facade;

import org.fusesource.fabric.api.*;
import org.jolokia.client.J4pClient;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pExecResponse;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.fusesource.fabric.jolokia.facade.Helpers.toList;
import static org.fusesource.fabric.jolokia.facade.JolokiaFabricConnector.createExecRequest;

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
            J4pExecRequest request = createExecRequest("containers(java.util.List)", toList("id"));
            J4pExecResponse response = getJ4p().execute(request);
            List<Map<String, Object>> values = response.getValue();

            for (Map<String, Object> value : values) {
                containers.add(new ContainerFacade(getJ4p(), (String)value.get("id")));
            }
        } catch (Exception e) {
            throw new RuntimeException ("Failed to fetch container list", e);
        }
        return containers.toArray(new Container[containers.size()]);
    }

    @Override
    public Container getContainer(String containerId) {
        return new ContainerFacade(getJ4p(), containerId);
    }

    @Override
    public void startContainer(String containerId) {
        Helpers.doContainerAction(getJ4p(), "start", containerId);
    }

    @Override
    public void startContainer(Container container) {
        Helpers.doContainerAction(getJ4p(), "start", container.getId());
    }

    @Override
    public void stopContainer(String containerId) {
        Helpers.doContainerAction(getJ4p(), "stop", containerId);
    }

    @Override
    public void stopContainer(Container container) {
        Helpers.doContainerAction(getJ4p(), "stop", container.getId());
    }

    @Override
    public void destroyContainer(String containerId) {
        Helpers.doContainerAction(getJ4p(), "destroy", containerId);
    }

    @Override
    public void destroyContainer(Container container) {
        Helpers.doContainerAction(getJ4p(), "destroy", container.getId());
    }

    @Override
    public CreateContainerMetadata[] createContainers(CreateContainerOptions createContainerOptions) {

        Map options = Helpers.getObjectMapper().convertValue(createContainerOptions, Map.class);

        Helpers.exec(getJ4p(), "createContainers(java.util.Map)", options);

        // JMX API doesn't currently return this...
        return new CreateContainerMetadata[0];
    }

    @Override
    public Version getDefaultVersion() {
        String id = Helpers.read(getJ4p(), "DefaultVersion");
        return new VersionFacade(getJ4p(), id);
    }

    @Override
    public void setDefaultVersion(Version version) {
        Helpers.write(getJ4p(), "DefaultVersion", version.getId());
    }

    @Override
    public Version[] getVersions() {
        List<Map<String, Object>> results = Helpers.exec(getJ4p(), "versions(java.util.List)", Helpers.toList("id"));
        List<Version> answer = new ArrayList<Version>();
        for (Map<String, Object> result : results) {
            answer.add(new VersionFacade(getJ4p(), (String)result.get("id")));
        }
        return answer.toArray(new Version[answer.size()]);
    }

    @Override
    public Version getVersion(String versionKey) {
        return new VersionFacade(getJ4p(), versionKey);
    }

    @Override
    public Version createVersion(String versionKey) {
        JSONObject obj = Helpers.exec(getJ4p(), "createVersion(java.lang.String)", versionKey);
        return new VersionFacade(getJ4p(), (String)obj.get("id"));
    }

    @Override
    public Version createVersion(Version version, String versionKey) {
        JSONObject obj = Helpers.exec(getJ4p(), "createVersion(java.lang.String, java.lang.String)", version.getId(), versionKey);
        return new VersionFacade(getJ4p(), versionKey);
    }

    @Override
    public URI getMavenRepoURI() {
        String uri = Helpers.read(getJ4p(), "MavenRepoURI");
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
        String uri = Helpers.read(getJ4p(), "MavenRepoUploadURI");
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getZookeeperUrl() {
        return Helpers.read(getJ4p(), "ZookeeperUrl");
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
        JSONObject obj = Helpers.exec(getJ4p(), "currentContainer()");
        return new ContainerFacade(getJ4p(), (String)obj.get("id"));
    }

    @Override
    public String getCurrentContainerName() {
        return Helpers.read(getJ4p(), "CurrentContainerName");
    }

    @Override
    public FabricRequirements getRequirements() {
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    @Override
    public void setRequirements(FabricRequirements fabricRequirements) throws IOException {
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    @Override
    public FabricStatus getFabricStatus() {
        throw new UnsupportedOperationException("The method is not yet implemented.");
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
        return Helpers.read(getJ4p(), "DefaultJvmOptions");
    }

    @Override
    public void setDefaultJvmOptions(String s) {
        Helpers.write(getJ4p(), "DefaultJvmOptions", s);
    }

    @Override
    public String containerWebAppURL(String s, String s2) {
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    public J4pClient getJ4p() {
        return connector.getJ4p();
    }
}
