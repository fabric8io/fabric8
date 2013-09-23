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
package org.fusesource.fabric.git.internal;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.eclipse.jgit.api.Git;
import org.fusesource.fabric.api.DataStorePlugin;
import org.fusesource.fabric.api.PlaceholderResolver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Caching version of {@link GitDataStore} to minimise the use of git operations
 * and speed things up a little
 */

@Component(name = "org.fusesource.datastore.git.caching",
        description = "Fabric Git Caching DataStore")
@References({
        @Reference(cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
                referenceInterface = PlaceholderResolver.class,
                bind = "bindPlaceholderResolver", unbind = "unbindPlaceholderResolver", policy = ReferencePolicy.DYNAMIC),
        @Reference(referenceInterface = CuratorFramework.class, bind = "bindCurator", unbind = "unbindCurator", cardinality = org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY),
        @Reference(referenceInterface = GitService.class, bind = "bindGitService", unbind = "unbindGitService", cardinality = org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY)
}
)
@Service(DataStorePlugin.class)
public class CachingGitDataStore extends GitDataStore implements DataStorePlugin<GitDataStore> {

    public static final String TYPE = "caching-git";

    private static class VersionData {
        Map<String, ProfileData> profiles;
    }

    private static class ProfileData {
        long lastModified;
        Map<String, byte[]> configurations;
    }

    private static final VersionData NOT_LOADED = new VersionData();

    private final Map<String, VersionData> cachedVersions = new HashMap<String, VersionData>();

    @Activate
    public void init() {

    }

    @Deactivate
    public void destroy() {
        stop();
    }

    public void start() {
        super.start();
    }


    public void stop() {
        super.stop();
    }

    public String toString() {
        return "CachingGitDataStore(" + getGitService() + ")";
    }

    public List<String> getVersions() {
        List<String> versions = null;
        // See if we already have the list of versions
        synchronized (cachedVersions) {
            if (!cachedVersions.isEmpty()) {
                versions = new ArrayList<String>(cachedVersions.keySet());
            }
        }
        // If not, load them
        if (versions == null) {
            versions = super.getVersions();
            // and update the cache with NOT_LOADED version data
            synchronized (cachedVersions) {
                if (cachedVersions.isEmpty()) {
                    for (String version : versions) {
                        cachedVersions.put(version, NOT_LOADED);
                    }
                }
            }
        }
        return versions;
    }

    protected VersionData getVersionData(String version) {
        // Ensure the list of versions is loaded
        getVersions();
        VersionData data;
        // Check if the version has already been loaded
        synchronized (cachedVersions) {
            data = cachedVersions.get(version);
        }
        if (data == NOT_LOADED) {
            // If not, load it ...
            data = loadVersion(version);
            // ... and update the cache
            synchronized (cachedVersions) {
                cachedVersions.put(version, data);
            }
        }
        return data;
    }

    protected VersionData loadVersion(final String version) {
        return gitReadOperation(new GitOperation<VersionData>() {
            public VersionData call(Git git, GitContext context) throws Exception {
                VersionData data = new VersionData();
                data.profiles = new HashMap<String, ProfileData>();
                pouplateVersionData(git, version, data);
                pouplateVersionData(git, "master", data);
                return data;
            }
        });
    }

    protected void pouplateVersionData(Git git, String branch, VersionData data) throws Exception {
        checkoutVersion(git, branch);
        File profilesDir = getProfilesDirectory(git);
        if (profilesDir.exists()) {
            File[] files = profilesDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        addProfileData(git, data, file, "");
                    }
                }
            }
        }
    }

    private void addProfileData(Git git, VersionData data, File file, String prefix) throws IOException {
        // TODO we could recursively scan for magic ".profile" files or something
        // then we could put profiles into nicer tree structure?
        String profile = file.getName();
        if (useDirectoriesForProfiles) {
            if (profile.endsWith(PROFILE_FOLDER_SUFFIX)) {
                profile = prefix + profile.substring(0, profile.length() - PROFILE_FOLDER_SUFFIX.length());
            } else {
                // lets recurse all children
                File[] files = file.listFiles();
                if (files != null) {
                    for (File child : files) {
                        if (child.isDirectory()) {
                            addProfileData(git, data, child, prefix + file.getName() + "-");
                        }
                    }
                }
                return;
            }
        }

        ProfileData profileData = new ProfileData();
        data.profiles.put(profile, profileData);
        // configurations
        profileData.configurations = doGetFileConfigurations(git, profile);
        // last modified
        File profileDirectory = file;
        File metadataFile = new File(profileDirectory, AGENT_METADATA_FILE);
        profileData.lastModified = profileDirectory.lastModified();
        if (metadataFile.exists()) {
            long modified = metadataFile.lastModified();
            profileData.lastModified = Math.max(profileData.lastModified, modified);
        }
    }

    public List<String> getProfiles(String version) {
        VersionData data = getVersionData(version);
        return data != null && data.profiles != null
                ? new ArrayList<String>(data.profiles.keySet())
                : new ArrayList<String>();
    }

    public boolean hasProfile(String version, String profile) {
        VersionData v = getVersionData(version);
        ProfileData p = v != null && v.profiles != null ? v.profiles.get(profile) : null;
        return p != null;
    }

    @Override
    public long getLastModified(String version, String profile) {
        VersionData v = getVersionData(version);
        ProfileData p = v != null && v.profiles != null ? v.profiles.get(profile) : null;
        return p.lastModified;
    }

    public byte[] getFileConfiguration(final String version, final String profile, final String fileName) {
        VersionData v = getVersionData(version);
        ProfileData p = v != null && v.profiles != null ? v.profiles.get(profile) : null;
        return p.configurations != null ? p.configurations.get(fileName) : null;
    }

    public Map<String, byte[]> getFileConfigurations(String version, String profile) {
        VersionData v = getVersionData(version);
        ProfileData p = v != null && v.profiles != null ? v.profiles.get(profile) : null;
        return p != null ? p.configurations : Collections.<String, byte[]>emptyMap();
    }

    public Map<String, String> getConfiguration(String version, String profile, String pid) {
        Map<String, Map<String, String>> configs = getConfigurations(version, profile);
        if (configs.containsKey(pid)) {
            return configs.get(pid);
        } else {
            return new HashMap<String, String>();
        }
    }

    @Override
    protected void clearCaches() {
        synchronized (cachedVersions) {
            cachedVersions.clear();
        }
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public CachingGitDataStore getDataStore() {
        return this;
    }
}
