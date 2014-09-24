/**
 *  Copyright 2005-2014 Red Hat, Inc.
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

import io.fabric8.agent.utils.AgentUtils;
import io.fabric8.api.Constants;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Profiles;
import io.fabric8.maven.util.MavenConfiguration;
import io.fabric8.maven.util.MavenConfigurationImpl;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.ops4j.util.property.PropertiesPropertyResolver;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Helper class for creating a DownloadManager.
 */
public class DownloadManagers {
    
    /**
     * Creates a {@link MavenConfiguration} based on the specified {@link java.util.Properties}.
     */
    public static MavenConfiguration createMavenConfiguration(FabricService fabricService, Properties properties) {
        AgentUtils.addMavenProxies(properties, fabricService);
        PropertiesPropertyResolver propertiesPropertyResolver = new PropertiesPropertyResolver(System.getProperties());
        DictionaryPropertyResolver dictionaryPropertyResolver = new DictionaryPropertyResolver(properties, propertiesPropertyResolver);
        MavenConfigurationImpl config = new MavenConfigurationImpl(dictionaryPropertyResolver, "org.ops4j.pax.url.mvn");
        return config;
    }


    /**
     * Creates a download manager using the current container's maven configuration
     */
    public static DownloadManager createDownloadManager(FabricService fabricService, ExecutorService executorService) throws MalformedURLException {
        Profile overlayProfile = fabricService.getCurrentContainer().getOverlayProfile();
        Profile effectiveProfile = Profiles.getEffectiveProfile(fabricService, overlayProfile);
        return createDownloadManager(fabricService, effectiveProfile, executorService);
    }

    /**
     * Creates a DownloadManager
     *
     * @param fabricService
     * @param profile
     * @param downloadExecutor
     * @return
     * @throws java.net.MalformedURLException
     */
    public static DownloadManager createDownloadManager(FabricService fabricService, Profile profile,
                                                        ExecutorService downloadExecutor) throws
            MalformedURLException {
        Map<String, String> configuration = profile.getConfiguration(Constants.AGENT_PID);
        if (configuration == null) {
            configuration = new HashMap<String, String>();
        }
        MavenConfiguration mavenConfiguration = createMavenConfiguration(fabricService, mapToProperties(configuration));
        return new DownloadManager(mavenConfiguration, downloadExecutor);
    }

    /**
     * Utility method for converting a {@link java.util.Map} into {@link java.util.Properties}
     *
     * @param map
     * @return
     */
    private static Properties mapToProperties(Map<String, String> map) {
        Properties p = new Properties();
        Set<Map.Entry<String, String>> set = map.entrySet();
        for (Map.Entry<String, String> entry : set) {
            p.put(entry.getKey(), entry.getValue());
        }
        return p;
    }

}
