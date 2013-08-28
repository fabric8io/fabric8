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
import org.fusesource.fabric.api.DataStorePlugin;
import org.fusesource.fabric.api.PlaceholderResolver;

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
    private Map<String, List<String>> profilesCache = new ConcurrentHashMap<String, List<String>>();
    private Map<String, Map<String, Map<String, String>>> configurationsCache = new ConcurrentHashMap<String, Map<String, Map<String, String>>>();
    private Map<String, Map<String, String>> configurationCache = new ConcurrentHashMap<String, Map<String, String>>();

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

    public List<String> getVersions() {
        if (cachedVersions == null) {
            cachedVersions = super.getVersions();
        }
        return cachedVersions;
    }

    public List<String> getProfiles(String version) {
        List<String> answer = profilesCache.get(version);
        if (answer == null) {
            answer = super.getProfiles(version);
            profilesCache.put(version, answer);
        }
        return answer;
    }

    public Map<String, Map<String, String>> getConfigurations(String version, String profile) {
        String key = "" + version + "/" + profile;
        Map<String, Map<String, String>> answer = configurationsCache.get(key);
        if (answer == null) {
            answer = super.getConfigurations(version, profile);
            configurationsCache.put(key, answer);
        }
        return answer;
    }

    public Map<String, String> getConfiguration(String version, String profile,
                                                String pid) {
        String key = "" + version + "/" + profile + "/" + pid;
        Map<String, String> answer = configurationCache.get(key);
        if (answer == null) {
            answer = super.getConfiguration(version, profile, pid);
            configurationCache.put(key, answer);
        }
        return answer;
    }

    protected void fireChangeNotifications() {
        clearCaches();
        super.fireChangeNotifications();
    }

    protected void clearCaches() {
        cachedVersions = null;
        profilesCache.clear();
        configurationsCache.clear();
        configurationCache.clear();
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
