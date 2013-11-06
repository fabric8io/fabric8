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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.eclipse.jgit.api.Git;
import org.fusesource.fabric.api.DataStore;
import org.fusesource.fabric.api.DataStoreRegistrationHandler;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.PlaceholderResolver;
import org.fusesource.fabric.api.jcip.GuardedBy;
import org.fusesource.fabric.api.jcip.ThreadSafe;
import org.fusesource.fabric.git.GitService;
import org.fusesource.fabric.utils.DataStoreUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * A Caching version of {@link GitDataStore} to minimise the use of git operations
 * and speed things up a little
 */
@ThreadSafe
@Component(name = DataStore.DATASTORE_TYPE_PID, policy = ConfigurationPolicy.REQUIRE, immediate = true)
@References({
        @Reference(referenceInterface = PlaceholderResolver.class, bind = "bindPlaceholderResolver", unbind = "unbindPlaceholderResolver", cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
        @Reference(referenceInterface = DataStoreRegistrationHandler.class, bind = "bindRegistrationHandler", unbind = "unbindRegistrationHandler"),
        @Reference(referenceInterface = CuratorFramework.class, bind = "bindCurator", unbind = "unbindCurator"),
        @Reference(referenceInterface = GitService.class, bind = "bindGitService", unbind = "unbindGitService"),
}
)
@Service(DataStore.class)
public final class CachingGitDataStore extends GitDataStore {

    public static final String TYPE = "caching-git";

    @GuardedBy("LoadingCache") private final LoadingCache<String, VersionData> cachedVersions = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, VersionData>() {
                @Override
                public VersionData load(final String version) throws Exception {
                    return gitOperation(new GitOperation<VersionData>() {
                        public VersionData call(Git git, GitContext context) throws Exception {
                            VersionData data = new VersionData();
                            pouplateVersionData(git, version, data);
                            pouplateVersionData(git, "master", data);
                            return data;
                        }
                    }, !hasVersion(version));
                }
            });

    @Activate
    protected void activate(Map<String, ?> configuration) throws Exception {
        super.activate(configuration);
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    protected VersionData getVersionData(String version) {
        assertValid();
        VersionData data = null;
        try {
            data = cachedVersions.get(version);
        } catch (ExecutionException e) {
            FabricException.launderThrowable(e);
        }
        return data;
    }

    protected void pouplateVersionData(Git git, String branch, VersionData data) throws Exception {
        assertValid();
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

        long lastModified = file.lastModified();
        File metadataFile = new File(file, AGENT_METADATA_FILE);
        if (metadataFile.exists()) {
            long modified = metadataFile.lastModified();
            lastModified = Math.max(lastModified, modified);
        }
        Map<String, byte[]> configurations = doGetFileConfigurations(git, profile);
        Map<String, Map<String, String>> substituted = new HashMap<String, Map<String, String>>();
        for (Map.Entry<String, byte[]> entry : configurations.entrySet()) {
            if (entry.getKey().endsWith(".properties")) {
                String pid = DataStoreUtils.stripSuffix(entry.getKey(), ".properties");
                substituted.put(pid, DataStoreUtils.toMap(DataStoreUtils.toProperties(entry.getValue())));
            }
        }
        ProfileData profileData = new ProfileData(lastModified, configurations, substituted);
        data.profiles.put(profile, profileData);
    }

    public List<String> getProfiles(String version) {
        assertValid();
        VersionData v = getVersionData(version);
        return v != null && v.profiles != null
                ? new ArrayList<String>(v.profiles.keySet())
                : new ArrayList<String>();
    }

    public boolean hasProfile(String version, String profile) {
        assertValid();
        VersionData v = getVersionData(version);
        ProfileData p = v != null && v.profiles != null ? v.profiles.get(profile) : null;
        return p != null;
    }

    @Override
    public long getLastModified(String version, String profile) {
        assertValid();
        VersionData v = getVersionData(version);
        ProfileData p = v != null && v.profiles != null ? v.profiles.get(profile) : null;
        return p != null ? p.lastModified : 0;
    }

    public byte[] getFileConfiguration(final String version, final String profile, final String fileName) {
        assertValid();
        VersionData v = getVersionData(version);
        ProfileData p = v != null && v.profiles != null ? v.profiles.get(profile) : null;
        return p != null && p.files != null ? p.files.get(fileName) : null;
    }

    public Map<String, byte[]> getFileConfigurations(String version, String profile) {
        assertValid();
        VersionData v = getVersionData(version);
        ProfileData p = v != null && v.profiles != null ? v.profiles.get(profile) : null;
        return p != null ? p.files : Collections.<String, byte[]>emptyMap();
    }

    public Map<String, Map<String, String>> getConfigurations(String version, String profile) {
        assertValid();
        VersionData v = getVersionData(version);
        ProfileData p = v != null && v.profiles != null ? v.profiles.get(profile) : null;
        return p != null ? p.configs : Collections.<String, Map<String, String>>emptyMap();
    }

    public Map<String, String> getConfiguration(String version, String profile, String pid) {
        assertValid();
        Map<String, Map<String, String>> configs = getConfigurations(version, profile);
        if (configs.containsKey(pid)) {
            return configs.get(pid);
        } else {
            return new HashMap<String, String>();
        }
    }

    @Override
    protected void clearCaches() {
        assertValid();
        cachedVersions.invalidateAll();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    private static class VersionData {
        final Map<String, ProfileData> profiles = new HashMap<String, ProfileData>();
    }

    private static class ProfileData {
        final long lastModified;
        final Map<String, byte[]> files;
        final Map<String, Map<String, String>> configs;
        ProfileData(long lastModified, Map<String, byte[]> files, Map<String, Map<String, String>> configs) {
            this.lastModified = lastModified;
            this.files = files;
            this.configs = configs;
        }
    }
}
