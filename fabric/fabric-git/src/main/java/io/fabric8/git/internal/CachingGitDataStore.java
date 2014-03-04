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
package io.fabric8.git.internal;

import io.fabric8.api.Constants;
import io.fabric8.api.DataStore;
import io.fabric8.api.DataStoreRegistrationHandler;
import io.fabric8.api.FabricException;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.jcip.GuardedBy;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.Configurer;
import io.fabric8.api.visibility.VisibleForTesting;
import io.fabric8.git.GitService;
import io.fabric8.utils.DataStoreUtils;

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
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.gitective.core.CommitUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * A Caching version of {@link GitDataStore} to minimise the use of git operations
 * and speed things up a little
 */
@ThreadSafe
@Component(name = Constants.DATASTORE_TYPE_PID, label = "Fabric8 Caching Git DataStore", policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = true)
@References({
        @Reference(referenceInterface = DataStoreRegistrationHandler.class, bind = "bindRegistrationHandler", unbind = "unbindRegistrationHandler"),
        @Reference(referenceInterface = CuratorFramework.class, bind = "bindCurator", unbind = "unbindCurator"),
        @Reference(referenceInterface = GitService.class, bind = "bindGitService", unbind = "unbindGitService"),
        @Reference(referenceInterface = RuntimeProperties.class, bind = "bindRuntimeProperties", unbind = "unbindRuntimeProperties"),
}
)
@Service(DataStore.class)
@Properties(
        @Property(name = "type", value = CachingGitDataStore.TYPE)
)
public final class CachingGitDataStore extends GitDataStore {

    public static final String TYPE = "caching-git";

    @Reference
    private Configurer configurer;


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
                    }, true); //We always pull when the item is not present in the cache to prevent loading stale data.
                }
            });

    @Activate
    @VisibleForTesting
    public void activate(Map<String, ?> configuration) throws Exception {
        configurer.configure(configuration, this);
        protectedActivate(configuration);
    }

    @Deactivate
    void deactivate() {
        protectedDeactivate();
    }

    protected VersionData getVersionData(String version) {
        assertValid();
        VersionData data = null;
        try {
            data = cachedVersions.get(version);
        } catch (ExecutionException e) {
            throw FabricException.launderThrowable(e);
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
                        addProfileData(git, branch,  data, file, "");
                    }
                }
            }
        }
    }

    private void addProfileData(Git git, String version, VersionData data, File file, String prefix) throws IOException {
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
                            addProfileData(git, version, data, child, prefix + file.getName() + "-");
                        }
                    }
                }
                return;
            }
        }

        String revision = git.getRepository().getRefDatabase().getRef(version).getObjectId().getName();
        String path = convertProfileIdToDirectory(profile);
        RevCommit commit = CommitUtils.getLastCommit(git.getRepository(), revision, CONFIGS_PROFILES + File.separator + path);
        String lastModified = commit != null ? commit.getId().abbreviate(GIT_COMMIT_SHORT_LENGTH).name() : "";

        Map<String, byte[]> configurations = doGetFileConfigurations(git, profile);
        Map<String, Map<String, String>> substituted = new HashMap<String, Map<String, String>>();
        for (Map.Entry<String, byte[]> entry : configurations.entrySet()) {
            if (entry.getKey().endsWith(".properties")) {
                String pid = DataStoreUtils.stripSuffix(entry.getKey(), ".properties");
                substituted.put(pid, DataStoreUtils.toMap(DataStoreUtils.toProperties(entry.getValue())));
            }
        }
        ProfileData profileData = new ProfileData(lastModified, Collections.unmodifiableMap(configurations), Collections.unmodifiableMap(substituted));
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
    public String getLastModified(String version, String profile) {
        assertValid();
        VersionData v = getVersionData(version);
        ProfileData p = v != null && v.profiles != null ? v.profiles.get(profile) : null;
        return p != null ? p.lastModified : "";
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
        return p != null ? new HashMap(p.files) : Collections.<String, byte[]>emptyMap();
    }

    public Map<String, Map<String, String>> getConfigurations(String version, String profile) {
        assertValid();
        VersionData v = getVersionData(version);
        ProfileData p = v != null && v.profiles != null ? v.profiles.get(profile) : null;
        return p != null ? new HashMap(p.configs) : Collections.<String, Map<String, String>>emptyMap();
    }

    public Map<String, String> getConfiguration(String version, String profile, String pid) {
        assertValid();
        Map<String, Map<String, String>> configs = getConfigurations(version, profile);
        if (configs.containsKey(pid)) {
            return new HashMap(configs.get(pid));
        } else {
            return new HashMap<String, String>();
        }
    }

    @Override
    void removeVersion(String version) {
        super.removeVersion(version);
        cachedVersions.invalidate(version);
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
        final String lastModified;
        final Map<String, byte[]> files;
        final Map<String, Map<String, String>> configs;
        ProfileData(String lastModified, Map<String, byte[]> files, Map<String, Map<String, String>> configs) {
            this.lastModified = lastModified;
            this.files = files;
            this.configs = configs;
        }
    }

    @VisibleForTesting
    public void bindConfigurer(Configurer configurer) {
        this.configurer = configurer;
    }

    void unbindConfigurer(Configurer configurer) {
        this.configurer = null;
    }
}
