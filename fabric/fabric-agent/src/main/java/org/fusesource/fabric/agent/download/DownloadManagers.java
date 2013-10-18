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
package org.fusesource.fabric.agent.download;

import org.fusesource.fabric.agent.mvn.DictionaryPropertyResolver;
import org.fusesource.fabric.agent.mvn.MavenConfiguration;
import org.fusesource.fabric.agent.mvn.MavenConfigurationImpl;
import org.fusesource.fabric.agent.mvn.MavenSettingsImpl;
import org.fusesource.fabric.agent.mvn.PropertiesPropertyResolver;
import org.fusesource.fabric.agent.utils.AgentUtils;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Helper class for creating a DownloadManager.
 *
 * TODO we could maybe reuse this code between process-fabric and fabric-openshift?
 */
public class DownloadManagers {
    /**
     * Creates a {@link org.fusesource.fabric.agent.mvn.MavenConfiguration} based on the specified {@link java.util.Properties}.
     *
     * @param properties
     * @return
     */
    public static MavenConfiguration createMavenConfiguration(FabricService fabricService, Properties properties) {
        AgentUtils.addMavenProxies(properties, fabricService);
        PropertiesPropertyResolver propertiesPropertyResolver = new PropertiesPropertyResolver(System.getProperties());
        DictionaryPropertyResolver dictionaryPropertyResolver = new DictionaryPropertyResolver(properties, propertiesPropertyResolver);
        MavenConfigurationImpl config = new MavenConfigurationImpl(dictionaryPropertyResolver, "org.ops4j.pax.url.mvn");
        config.setSettings(new MavenSettingsImpl(config.getSettingsFileUrl(), config.useFallbackRepositories()));
        return config;
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
        Map<String, String> configuration = profile.getConfiguration("org.fusesource.fabric.agent");
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
