package org.fusesource.fabric.jolokia.facade.facades;

import org.codehaus.jackson.map.ObjectMapper;
import org.fusesource.fabric.api.*;
import org.fusesource.fabric.jolokia.facade.utils.Helpers;
import org.jolokia.client.J4pClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Stan Lewis
 */
public class ContainerFacade implements Container, HasId {

    private J4pClient j4p;
    private String id;

    public ContainerFacade(J4pClient j4p, String id) {
        this.j4p = j4p;
        this.id = id;
    }


    public J4pClient getJ4p() {
        return j4p;
    }

    public void setJ4p(J4pClient j4p) {
        this.j4p = j4p;
    }


    private <T extends Object> T getFieldValue(String field) {
        return Helpers.getFieldValue(j4p, "getContainer(java.lang.String, java.util.List)", id, field);
    }

    @Override
    public String getType() {
        return getFieldValue("type");
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public Container getParent() {
        String parentId = (String) getFieldValue("parentId");
        if (parentId == null) {
            return null;
        }
        return new ContainerFacade(j4p, parentId);
    }

    @Override
    public boolean isAlive() {
        return getFieldValue("alive");
    }

    @Override
    public boolean isEnsembleServer() {
        return getFieldValue("ensembleServer");
    }

    @Override
    public boolean isRoot() {
        return getFieldValue("root");
    }

    @Override
    public String getSshUrl() {
        return getFieldValue("sshUrl");
    }

    @Override
    public String getJmxUrl() {
        return getFieldValue("jmxUrl");
    }

    @Override
    public String getHttpUrl() {
        return getFieldValue("httpUrl");
    }

    @Override
    public String getJolokiaUrl() {
        return getFieldValue("jolokiaUrl");
    }

    @Override
    public void setJolokiaUrl(String s) {
        throw new UnsupportedOperationException("This cannot be set from a remote process");
    }

    @Override
    public boolean isManaged() {
        return getFieldValue("managed");
    }

    @Override
    public Version getVersion() {
        String version = getFieldValue("versionId");
        return new VersionFacade(j4p, version);
    }

    @Override
    public void setVersion(Version version) {
        Void v = Helpers.exec(j4p, "applyVersionToContainers(java.lang.String, java.util.List)", version.getId(), Helpers.toList(id));
    }

    @Override
    public Profile[] getProfiles() {
        List<String> profileIds = getFieldValue("profileIds");
        if (profileIds == null || profileIds.size() == 0) {
            return new Profile[0];
        }

        String version = getFieldValue("versionId");
        List<Profile> answer = new ArrayList<Profile>();

        for (String id : profileIds) {
            answer.add(new ProfileFacade(j4p, version, id));
        }

        return answer.toArray(new Profile[answer.size()]);
    }

    @Override
    public void setProfiles(Profile[] profiles) {
        List<String> ids = Helpers.extractIds(profiles);
        Helpers.exec(j4p, "applyProfilesToContainers(java.lang.String, java.util.List, java.util.List", getVersion().getId(), ids, Helpers.toList(id) );
    }

    @Override
    public void addProfiles(Profile... profiles) {
        List<String> ids = Helpers.extractIds(profiles);
        Helpers.exec(j4p, "addProfilesToContainer(java.lang.String, java.util.List", id, ids);
    }

    @Override
    public void removeProfiles(Profile... profiles) {
        List<String> ids = Helpers.extractIds(profiles);
        Helpers.exec(j4p, "removeProfilesFromContainer(java.lang.String, java.util.List", id, ids);
    }

    @Override
    public Profile getOverlayProfile() {
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    @Override
    public String getLocation() {
        return getFieldValue("location");
    }

    @Override
    public void setLocation(String s) {
        Helpers.exec(j4p, "setContainerProperty(java.lang.String, java.lang.String, java.lang.Object)", id, "location", s);
    }

    @Override
    public String getGeoLocation() {
        return getFieldValue("geoLocation");
    }

    @Override
    public void setGeoLocation(String s) {
        Helpers.exec(j4p, "setContainerProperty(java.lang.String, java.lang.String, java.lang.Object)", id, "geoLocation", s);
    }

    @Override
    public String getResolver() {
        return getFieldValue("resolver");
    }

    @Override
    public void setResolver(String s) {
        Helpers.exec(j4p, "setContainerProperty(java.lang.String, java.lang.String, java.lang.Object)", id, "resolver", s);
    }

    @Override
    public String getIp() {
        return getFieldValue("ip");
    }

    @Override
    public String getLocalIp() {
        return getFieldValue("ip");
    }

    @Override
    public void setLocalIp(String s) {
        Helpers.exec(j4p, "setContainerProperty(java.lang.String, java.lang.String, java.lang.Object)", id, "localIp", s);
    }

    @Override
    public String getLocalHostname() {
        return getFieldValue("localHostname");
    }

    @Override
    public void setLocalHostname(String s) {
        Helpers.exec(j4p, "setContainerProperty(java.lang.String, java.lang.String, java.lang.Object)", id, "localHostname", s);
    }

    @Override
    public String getPublicIp() {
        return getFieldValue("publicIp");
    }

    @Override
    public void setPublicIp(String s) {
        Helpers.exec(j4p, "setContainerProperty(java.lang.String, java.lang.String, java.lang.Object)", id, "publicIp", s);
    }

    @Override
    public String getPublicHostname() {
        return getFieldValue("publicHostname");
    }

    @Override
    public void setPublicHostname(String s) {
        Helpers.exec(j4p, "setContainerProperty(java.lang.String, java.lang.String, java.lang.Object)", id, "publicHostname", s);
    }

    @Override
    public String getManualIp() {
        return getFieldValue("manualip");
    }

    @Override
    public void setManualIp(String s) {
        Helpers.exec(j4p, "setContainerProperty(java.lang.String, java.lang.String, java.lang.Object)", id, "manualIp", s);
    }

    @Override
    public int getMinimumPort() {
        return getFieldValue("minimumPort");
    }

    @Override
    public void setMinimumPort(int i) {
        Helpers.exec(j4p, "setContainerProperty(java.lang.String, java.lang.String, java.lang.Object)", id, "minimumPort", i);
    }

    @Override
    public int getMaximumPort() {
        return getFieldValue("maximumPort");
    }

    @Override
    public void setMaximumPort(int i) {
        Helpers.exec(j4p, "setContainerProperty(java.lang.String, java.lang.String, java.lang.Object)", id, "maximumPort", i);
    }

    @Override
    public void start() {
        Helpers.doContainerAction(j4p, "start", id);
    }

    @Override
    public void stop() {
        Helpers.doContainerAction(j4p, "stop", id);
    }

    @Override
    public void destroy() {
        Helpers.doContainerAction(j4p, "destroy", id);
    }

    @Override
    public Container[] getChildren() {
        List<String> childIds = getFieldValue("children");
        if (childIds == null || childIds.size() == 0) {
            return new Container[0];
        }
        List<Container> answer = new ArrayList<Container>();
        for (String childId : childIds) {
            answer.add(new ContainerFacade(j4p, childId));
        }
        return answer.toArray(new Container[answer.size()]);
    }

    @Override
    public List<String> getJmxDomains() {
        return getFieldValue("jmxDomains");
    }

    @Override
    public boolean isProvisioningComplete() {
        return getFieldValue("provisioningComplete");
    }

    @Override
    public boolean isProvisioningPending() {
        return getFieldValue("provisioningPending");
    }

    @Override
    public String getProvisionResult() {
        return getFieldValue("provisionResult");
    }

    @Override
    public void setProvisionResult(String s) {
        throw new UnsupportedOperationException("This cannot be set from a remote process");
    }

    @Override
    public String getProvisionException() {
        return getFieldValue("provisionException");
    }

    @Override
    public void setProvisionException(String s) {
        throw new UnsupportedOperationException("This cannot be set from a remote process");
    }

    @Override
    public List<String> getProvisionList() {
        return getFieldValue("provisionList");
    }

    @Override
    public void setProvisionList(List<String> strings) {
        throw new UnsupportedOperationException("This cannot be set from a remote process");
    }

    @Override
    public String getProvisionStatus() {
        return getFieldValue("provisionStatus");
    }

    @Override
    public CreateContainerMetadata<?> getMetadata() {
        Map<Object, Object> obj = getFieldValue("metadata");

        if (obj == null || obj.size() == 0) {
            return null;
        }

        Map<Object, Object> createOptions = (Map<Object, Object>) obj.get("createOptions");
        String providerType = (String) createOptions.get("providerType");

        if (providerType == null) {
            // we can't figure what this is
            return null;
        }

        CreateContainerMetadata answer = null;
        ObjectMapper mapper = Helpers.getObjectMapper();

        if (providerType.equals("child")) {
            answer = mapper.convertValue(obj, CreateChildContainerMetadata.class);
        } else if (providerType.equals("ssh")) {
            answer = mapper.convertValue(obj, CreateSshContainerMetadata.class);
        } else if (providerType.equals("jclouds")) {
            answer = mapper.convertValue(obj, CreateJCloudsContainerMetadata.class);
        }
        return answer;
    }

    @Override
    public boolean isAliveAndOK() {
        return getFieldValue("aliveAndOK");
    }
}
