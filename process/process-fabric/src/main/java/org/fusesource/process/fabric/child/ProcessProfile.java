package org.fusesource.process.fabric.child;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.internal.ProfileImpl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ProcessProfile extends ProfileImpl {

    private final Container container;
    private final ProcessRequirements requirements;
    private final boolean includeContainerProfile;

    public ProcessProfile(Container container, ProcessRequirements requirements, FabricService fabricService,
                          boolean includeContainerProfile) {
        super(requirements.getId(), container.getVersion().getId(), fabricService);
        this.container = container;
        this.requirements = requirements;
        this.includeContainerProfile = includeContainerProfile;
    }

    @Override
    public String getVersion() {
        return container.getVersion().getId();
    }

    @Override
    public Map<String, String> getAttributes() {
        return Collections.emptyMap();
    }

    @Override
    public void setAttribute(String key, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Profile[] getParents() {
        List<String> parents = requirements.getProfiles();
        List<Profile> profiles = new LinkedList<Profile>();
        if (includeContainerProfile) {
            profiles.add(container.getOverlayProfile());
        }
        for (String parent : parents) {
            Profile p = container.getVersion().getProfile(parent);
            profiles.add(p);
        }
        return profiles.toArray(new Profile[profiles.size()]);
    }

    @Override
    public void setParents(Profile[] parents) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setConfigurations(Map<String, Map<String, String>> configurations) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOverlay() {
        return false;
    }

    @Override
    public void delete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBundles(List<String> values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFabs(List<String> values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFeatures(List<String> values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRepositories(List<String> values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOverrides(List<String> values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean configurationEquals(Profile other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean agentConfigurationEquals(Profile other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAbstract() {
        return true;
    }

    @Override
    public boolean isLocked() {
        return true;
    }

    @Override
    public boolean isHidden() {
        return true;
    }

    @Override
    public String getId() {
        return "process-profile-" + requirements.getId();
    }
}
