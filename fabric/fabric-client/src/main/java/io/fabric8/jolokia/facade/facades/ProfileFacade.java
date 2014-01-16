package io.fabric8.jolokia.facade.facades;

import io.fabric8.api.Container;
import io.fabric8.api.HasId;
import io.fabric8.api.Profile;
import io.fabric8.jolokia.facade.utils.Helpers;
import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pExecResponse;

import javax.management.MalformedObjectNameException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Stan Lewis
 */
public class ProfileFacade implements Profile, HasId {

    J4pClient j4p;
    String id;
    String versionId;

    public ProfileFacade(J4pClient j4p, String versionId, String id) {
        this.j4p = j4p;
        this.versionId = versionId;
        this.id = id;
    }

    private static <T extends Object> T getFieldValue(J4pClient j4p, String operation, String versionId, String id, String field) {
        T rc = null;
        try {
            J4pExecRequest request = Helpers.createExecRequest(operation, versionId, id, Helpers.toList(field));
            J4pExecResponse response = j4p.execute(request);
            Map<String, Object> value = response.getValue();
            rc = (T)value.get(field);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("Failed to get container field", e);
        } catch (J4pException e) {
            throw new RuntimeException("Failed to get container field", e);
        }
        return rc;
    }

    private <T extends Object> T getFieldValue(String field) {
        return getFieldValue(j4p, "getProfile(java.lang.String, java.lang.String, java.util.List)", versionId, id, field);
    }

    @Override
    public String getVersion() {
        return versionId;
    }

    @Override
    public Map<String, String> getAttributes() {
        return getFieldValue("attributes");
    }

    @Override
    public void setAttribute(String s, String s2) {
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    @Override
    public Profile[] getParents() {
        List<String> profiles = getFieldValue("parents");
//        JSONArray profiles = getFieldValue("parents");
        if (profiles == null || profiles.size() == 0) {
            return new Profile[0];
        }
        List<Profile> answer = new ArrayList<Profile>();
        for (Object profile : profiles) {
            answer.add(new ProfileFacade(j4p, versionId, (String)profile));
        }
        return answer.toArray(new Profile[answer.size()]);
    }

    @Override
    public void setParents(Profile[] profiles) {
        List<String> parentIds = new ArrayList<String>();
        for (Profile profile : profiles) {
            parentIds.add(profile.getId());
        }
        Helpers.exec(j4p, "changeProfileParents(java.lang.String, java.lang.String, java.util.List)", versionId, id, parentIds);
    }

    @Override
    public void refresh() {
        Helpers.exec(j4p, "refreshProfile", versionId, id);
    }

    @Override
    public Container[] getAssociatedContainers() {
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    @Override
    public List<String> getLibraries() {
        return getFieldValue("libraries");
    }

    @Override
    public List<String> getEndorsedLibraries() {
        return getFieldValue("endorsedLibraries");
    }

    @Override
    public List<String> getExtensionLibraries() {
        return getFieldValue("extensionLibraries");
    }

    @Override
    public List<String> getBundles() {
        return getFieldValue("bundles");
    }

    @Override
    public List<String> getFabs() {
        return getFieldValue("fabs");
    }

    @Override
    public List<String> getFeatures() {
        return getFieldValue("features");
    }

    @Override
    public List<String> getRepositories() {
        return getFieldValue("repositories");
    }

    @Override
    public List<String> getOverrides() {
        return getFieldValue("overrides");
    }

    @Override
    public List<String> getConfigurationFileNames() {
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    @Override
    public Map<String, byte[]> getFileConfigurations() {
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    @Override
    public byte[] getFileConfiguration(String fileName) {
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    @Override
    public void setFileConfigurations(Map<String, byte[]> stringMap) {
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    @Override
    public Map<String, Map<String, String>> getConfigurations() {
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    @Override
    public Map<String, String> getContainerConfiguration() {
        return getFieldValue("containerConfiguration");
    }

    @Override
    public void setConfigurations(Map<String, Map<String, String>> stringMapMap) {
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    @Override
    public Profile getOverlay() {
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    @Override
    public Profile getOverlay(boolean substitute) {
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    @Override
    public boolean isOverlay() {
        return (Boolean)getFieldValue("overlay");
    }

    @Override
    public void delete() {
        Void v = Helpers.exec(j4p, "deleteProfile(java.lang.String, java.lang.String)", versionId, id);
    }

    @Override
    public void delete(boolean b) {
        Void v = Helpers.exec(j4p, "deleteProfile(java.lang.String, java.lang.String)", versionId, id);
    }

    @Override
    public void setBundles(List<String> strings) {
        Void v = Helpers.exec(j4p, "setProfileBundles(java.lang.String, java.lang.String, java.util.List)", versionId, id, strings);
    }

    @Override
    public void setFabs(List<String> strings) {
        Void v = Helpers.exec(j4p, "setProfileFabs(java.lang.String, java.lang.String, java.util.List)", versionId, id, strings);
    }

    @Override
    public void setFeatures(List<String> strings) {
        Void v = Helpers.exec(j4p, "setProfileFeatures(java.lang.String, java.lang.String, java.util.List)", versionId, id, strings);
    }

    @Override
    public void setRepositories(List<String> strings) {
        Void v = Helpers.exec(j4p, "setProfileRepositories(java.lang.String, java.lang.String, java.util.List)", versionId, id, strings);
    }

    @Override
    public void setOverrides(List<String> strings) {
        Void v = Helpers.exec(j4p, "setProfileOverrides(java.lang.String, java.lang.String, java.util.List)", versionId, id, strings);
    }

    @Override
    public boolean configurationEquals(Profile profile) {
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    @Override
    public boolean agentConfigurationEquals(Profile profile) {
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    @Override
    public boolean isAbstract() {
        return (Boolean)getFieldValue("abstract");
    }

    @Override
    public boolean isLocked() {
        return (Boolean)getFieldValue("locked");
    }

    @Override
    public boolean isHidden() {
        return (Boolean)getFieldValue("hidden");
    }

    @Override
    public String getProfileHash() {
        return getFieldValue("lastModified");
    }

    @Override
    public int compareTo(Profile profile) {
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    @Override
    public boolean exists() {
        throw new UnsupportedOperationException("The method is not yet implemented.");
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Map<String, String> getConfiguration(String pid) {
        Map<String, Map<String, String>> configurations = getConfigurations();
        if (configurations != null) {
            return configurations.get(pid);
        }
        return null;
    }

    @Override
    public void setConfiguration(String pid, Map<String, String> configuration) {
        Map<String, Map<String, String>> configurations = getConfigurations();
        if (configurations != null) {
            configurations.put(pid, configuration);
            setConfigurations(configurations);
        }
    }
}
