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
package io.fabric8.agent.utils;

import io.fabric8.agent.DeploymentAgent;
import io.fabric8.agent.resolver.FeatureResource;
import io.fabric8.utils.Strings;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.resource.Resource;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

public class ConfigUtils {

    public static void installFeatureConfigs(BundleContext bundleContext, Map<String, Resource> resources) throws IOException {
        ServiceReference configAdminServiceReference = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
        if (configAdminServiceReference != null) {
            ConfigurationAdmin configAdmin = (ConfigurationAdmin) bundleContext.getService(configAdminServiceReference);
            for (FeatureResource resource : filterFeatureResources(resources)) {
                Map<String, Map<String, String>> configs = resource.getFeature().getConfigurations();
                for (Map.Entry<String, Map<String, String>> entry : configs.entrySet()) {
                    String pid = entry.getKey();
                    if (!isConfigurationManaged(configAdmin, pid)) {
                        applyConfiguration(configAdmin, pid, entry.getValue());
                    }
                }
            }
        }
    }

    static boolean isConfigurationManaged(ConfigurationAdmin configurationAdmin, String pid) throws IOException {
        org.osgi.service.cm.Configuration configuration = configurationAdmin.getConfiguration(pid);
        Dictionary<String, ?> properties = configuration.getProperties();
        if (properties == null) {
            return false;
        }
        String fabricManagedPid = (String) properties.get(DeploymentAgent.FABRIC_ZOOKEEPER_PID);
        return Strings.isNotBlank(fabricManagedPid);
    }

    static void applyConfiguration(ConfigurationAdmin configurationAdmin, String pid, Map<String, String> config) throws IOException {
        org.osgi.service.cm.Configuration configuration = configurationAdmin.getConfiguration(pid);
        Hashtable properties = new java.util.Properties();
        properties.putAll(config);
        configuration.setBundleLocation(null);
        configuration.update(properties);

    }

    static Collection<FeatureResource> filterFeatureResources(Map<String, Resource> resources) {
        Set<FeatureResource> featureResources = new HashSet<FeatureResource>();
        for (Resource resource : resources.values()) {
            if (resource instanceof FeatureResource) {
                featureResources.add((FeatureResource) resource);
            }
        }
        return featureResources;
    }

}
