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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private List<String> cachedVersions;
    private Map<String, Boolean> loadedVersions = new ConcurrentHashMap<String, Boolean>();
    private Map<String, List<String>> profilesCache = new ConcurrentHashMap<String, List<String>>();
    private Map<String, Long> profileLastModified = new ConcurrentHashMap<String, Long>();
    private Map<String, Map<String, byte[]>> configurationsCache = new ConcurrentHashMap<String, Map<String, byte[]>>();

    @Activate
    public void init() {

    }

    @Deactivate
    public void destroy() {
        stop();
    }

    public synchronized void start() {
        super.start();
    }


    public synchronized void stop() {
        super.stop();
    }

    public String toString() {
        return "CachingGitDataStore(" + getGitService() + ")";
    }

    protected void ensureVersionLoaded(String version) {
        if (getVersions().contains(version)) {
            if (!loadedVersions.containsKey(version)) {
                synchronized (this) {
                    if (!loadedVersions.containsKey(version)) {
                        loadVersion(version);
                        loadedVersions.put(version, Boolean.TRUE);
                    }
                }
            }
        }
    }

    protected void loadVersion(final String version) {
        gitReadOperation(new GitOperation<Object>() {
            public Object call(Git git, GitContext context) throws Exception {
                List<String> profiles = new ArrayList<String>();
                checkoutVersion(git, version);
                File profilesDir = getProfilesDirectory(git);
                if (profilesDir.exists()) {
                    File[] files = profilesDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.isDirectory()) {
                                // TODO we could recursively scan for magic ".profile" files or something
                                // then we could put profiles into nicer tree structure?
                                String profile = file.getName();
                                profiles.add(profile);
                                // configurations
                                Map<String, byte[]> configs = doGetFileConfigurations(git, profile);
                                configurationsCache.put(version + "/" + profile, configs);
                                // last modified
                                File profileDirectory = file;
                                File metadataFile = new File(profileDirectory, AGENT_METADATA_FILE);
                                Long answer = null;
                                if (profileDirectory.exists()) {
                                    answer = profileDirectory.lastModified();
                                    if (metadataFile.exists()) {
                                        long modified = metadataFile.lastModified();
                                        if (modified > answer) {
                                            answer = modified;
                                        }
                                    }
                                }
                                profileLastModified.put(version + "/" + profile, answer);
                            }
                        }
                    }
                }
                profilesCache.put(version, profiles);
                return null;
            }
        });
    }

    public List<String> getVersions() {
        if (cachedVersions == null) {
            synchronized (this) {
                if (cachedVersions == null) {
                    cachedVersions = super.getVersions();
                }
            }
        }
        return cachedVersions;
    }

    public List<String> getProfiles(String version) {
        ensureVersionLoaded(version);
        return profilesCache.get(version);
    }

    public boolean hasProfile(String version, String profile) {
        return getProfiles(version).contains(profile);
    }

    @Override
    public long getLastModified(String version, String profile) {
        ensureVersionLoaded(version);
        return profileLastModified.get(version + "/" + profile);
    }

    public byte[] getFileConfiguration(final String version, final String profile, final String fileName) {
        return getFileConfigurations(version, profile).get(fileName);
    }

    public Map<String, byte[]> getFileConfigurations(String version, String profile) {
        ensureVersionLoaded(version);
        return configurationsCache.get(version + "/" + profile);
    }

    public Map<String, String> getConfiguration(String version, String profile, String pid) {
        Map<String, Map<String, String>> configs = getConfigurations(version, profile);
        if (configs.containsKey(pid)) {
            return configs.get(pid);
        } else {
            return new HashMap<String, String>();
        }
    }

    protected void fireChangeNotifications() {
        clearCaches();
        super.fireChangeNotifications();
    }

    protected void clearCaches() {
        cachedVersions = null;
        profilesCache.clear();
        loadedVersions.clear();
        configurationsCache.clear();
    }

    @Override
    public String getName() {
        return TYPE;
    }

    @Override
    public CachingGitDataStore getDataStore() {
        return this;
    }
}
