/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.agent.download;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import io.fabric8.agent.download.impl.MavenDownloadManager;
import io.fabric8.agent.utils.AgentUtils;
import io.fabric8.api.Constants;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Profiles;
import io.fabric8.maven.MavenResolver;
import io.fabric8.maven.MavenResolvers;
import org.apache.maven.settings.Mirror;

public final class DownloadManagers {

    /**
     * Creates a download manager using the current container's maven configuration
     */
    public static DownloadManager createDownloadManager(FabricService fabricService, ScheduledExecutorService executorService) {
        Profile overlayProfile = fabricService.getCurrentContainer().getOverlayProfile();
        Profile effectiveProfile = Profiles.getEffectiveProfile(fabricService, overlayProfile);
        return createDownloadManager(fabricService, effectiveProfile, executorService);
    }

    /**
     * Creates a DownloadManager
     */
    public static DownloadManager createDownloadManager(FabricService fabricService, Profile profile, ScheduledExecutorService executorService) {
        Map<String, String> configuration = profile.getConfiguration(Constants.AGENT_PID);
        if (configuration == null) {
            configuration = new HashMap<>();
        }
        Dictionary<String, String> properties = mapToDictionary(configuration);
        Mirror mirror = AgentUtils.getMavenProxy(fabricService);
        MavenResolver resolver = MavenResolvers.createMavenResolver(mirror, properties, "org.ops4j.pax.url.mvn");
        return createDownloadManager(resolver, executorService);
    }

    /**
     * Creates a DownloadManager
     */
    public static DownloadManager createDownloadManager(MavenResolver resolver, ScheduledExecutorService executorService) {
        return new MavenDownloadManager(resolver, executorService);
    }

    /**
     * Utility method for converting a {@link java.util.Map} into {@link java.util.Properties}
     */
    private static Dictionary<String, String> mapToDictionary(Map<String, String> map) {
        Hashtable<String, String> p = new Hashtable<>();
        Set<Map.Entry<String, String>> set = map.entrySet();
        for (Map.Entry<String, String> entry : set) {
            p.put(entry.getKey(), entry.getValue());
        }
        return p;
    }

    // Private constructor
    private DownloadManagers() { }
}
