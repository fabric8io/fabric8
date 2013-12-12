package io.fabric8.jolokia.facade.facades;

import io.fabric8.api.HasId;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.api.VersionSequence;
import io.fabric8.jolokia.facade.utils.Helpers;
import org.jolokia.client.J4pClient;
import org.json.simple.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Stan Lewis
 */
public class VersionFacade implements Version, HasId {

    J4pClient j4p;
    String id;
    VersionSequence sequence;

    public VersionFacade(J4pClient j4p, String id) {
        this.j4p = j4p;
        this.id = id;
        this.sequence = new VersionSequence(id);
    }

    private <T extends Object> T getFieldValue(String field) {
        return Helpers.getFieldValue(j4p, "getVersion(java.lang.String, java.util.List)", id, field);
    }

    @Override
    public String getName() {
        return id;
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
    public VersionSequence getSequence() {
        return sequence;
    }

    @Override
    public Version getDerivedFrom() {
        String id = getFieldValue("derivedFrom");
        if (id == null) {
            return null;
        }
        return new VersionFacade(j4p, id);
    }

    @Override
    public Profile[] getProfiles() {
        List<Profile> rc = new ArrayList<Profile>();

        JSONArray array = getFieldValue("profiles");
        if (array == null || array.size() == 0) {
            return new Profile[0];
        }

        for (Object profile : array) {
            rc.add(new ProfileFacade(j4p, id, (String)profile));
        }
        return rc.toArray(new Profile[rc.size()]);
    }

    @Override
    public Profile getProfile(String s) {
        return new ProfileFacade(j4p, id, s);
    }

    @Override
    public Profile createProfile(String s) {
        Map<String, Object> profile = Helpers.exec(j4p, "createProfile(java.lang.String, java.lang.String)", id, s);
        if (profile == null) {
            return null;
        }
        return new ProfileFacade(j4p, id, s);
    }

    @Override
    public void copyProfile(String s, String s2, boolean b) {
        Void v = Helpers.exec(j4p, "copyProfile(java.lang.String, java.lang.String, java.lang.String, boolean)", id, s, s2, b);
    }

    @Override
    public void renameProfile(String s, String s2, boolean b) {
        Void v = Helpers.exec(j4p, "renameProfile(java.lang.String, java.lang.String, java.lang.String, boolean)", id, s, s2, b);
    }

    @Override
    public boolean hasProfile(String s) {
        String [] profiles = getFieldValue("profiles");
        return Arrays.asList(profiles).contains(s);
    }

    @Override
    public void delete() {
        Void v = Helpers.exec(j4p, "deleteVersion(java.lang.String)", id);
    }

    @Override
    public int compareTo(Version version) {
        return this.sequence.compareTo(version.getSequence());
    }

    @Override
    public String getId() {
        return id;
    }
}
