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
package io.fabric8.service.child;

import io.fabric8.api.Container;
import io.fabric8.api.CreateChildContainerOptions;
import io.fabric8.api.CreateContainerBasicOptions;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.CreateContainerOptions;
import io.fabric8.api.EnvironmentVariables;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profiles;
import io.fabric8.common.util.Strings;
import io.fabric8.utils.PasswordEncoder;

import java.util.Map;
import java.util.Set;

/**
 * Helper methods for detecting the kinds of child containers based on the profiles
 */
public class ChildContainers {

    /**
     * Returns true if the given container is a java or process child container
     */
    public static boolean isJavaOrProcessContainer(FabricService fabric, Container container) {
        if (container != null) {
            CreateContainerMetadata<?> metadata = container.getMetadata();
            if (metadata != null) {
                CreateContainerOptions createOptions = metadata.getCreateOptions();
                if (createOptions instanceof CreateContainerBasicOptions) {
                    return isJavaOrProcessContainer(fabric, (CreateContainerBasicOptions) createOptions);
                }
            }
        }
        return false;
    }

    /**
     * Returns true if the given container is a java or process child container
     */
    public static boolean isJavaOrProcessContainer(FabricService fabric, CreateContainerBasicOptions options) {
        boolean isJavaContainer = isJavaContainer(fabric, options);
        boolean isProcessContainer = isProcessContainer(fabric, options);
        return isProcessContainer || isJavaContainer;
    }

    /**
     * Returns true if the given container is a java child container
     */
    public static boolean isJavaContainer(FabricService fabricService, CreateContainerBasicOptions options) {
        Map<String, ?> javaContainerConfig = Profiles.getOverlayConfiguration(fabricService, options.getProfiles(), options.getVersion(), ChildConstants.JAVA_CONTAINER_PID);
        return !javaContainerConfig.isEmpty();
    }

    /**
     * Returns true if the given container is a process child container
     */
    public static boolean isProcessContainer(FabricService fabricService, CreateContainerBasicOptions options) {
        Set<String> profileIds = options.getProfiles();
        String versionId = options.getVersion();
        Map<String, ?> processConfig = Profiles.getOverlayConfiguration(fabricService, profileIds, versionId, ChildConstants.PROCESS_CONTAINER_PID);
        return processConfig != null && processConfig.size() > 0;
    }

    /**
     * Creates the environment variables for the given container options using the profiles specified in the options to figure out
     * what environment variables to use.
     */
    public static Map<String, String> getEnvironmentVariables(FabricService service, CreateContainerBasicOptions options) {
        Set<String> profileIds = options.getProfiles();
        String versionId = options.getVersion();
        String zookeeperUrl = service.getZookeeperUrl();
        String zookeeperUser = service.getZooKeeperUser();
        String zookeeperPasswordRaw = service.getZookeeperPassword();
        String zookeeperPassword = zookeeperPasswordRaw;
        if (zookeeperPassword != null) {
            zookeeperPassword = PasswordEncoder.encode(zookeeperPassword);
        }
        String localIp = service.getCurrentContainer().getLocalIp();
        if (!Strings.isNullOrBlank(localIp)) {
            int idx = zookeeperUrl.lastIndexOf(':');
            if (idx > 0) {
                localIp += zookeeperUrl.substring(idx);
            }
            zookeeperUrl = localIp;
        }

        Map<String, String> envVarsOverlay = Profiles.getOverlayConfiguration(service, profileIds, versionId, EnvironmentVariables.ENVIRONMENT_VARIABLES_PID);
        String containerName = options.getName();
        envVarsOverlay.put(EnvironmentVariables.KARAF_NAME, containerName);
        envVarsOverlay.put(EnvironmentVariables.CONTAINER_NAME, containerName);
        if (!options.isEnsembleServer()) {
            if (!envVarsOverlay.containsKey(EnvironmentVariables.ZOOKEEPER_URL)) {
                envVarsOverlay.put(EnvironmentVariables.ZOOKEEPER_URL, zookeeperUrl);
            }
            if (!envVarsOverlay.containsKey(EnvironmentVariables.ZOOKEEPER_USER)) {
                envVarsOverlay.put(EnvironmentVariables.ZOOKEEPER_USER, zookeeperUser);
            }
            if (!envVarsOverlay.containsKey(EnvironmentVariables.ZOOKEEPER_PASSWORD)) {
                envVarsOverlay.put(EnvironmentVariables.ZOOKEEPER_PASSWORD, zookeeperPassword);
            }
            if (!envVarsOverlay.containsKey(EnvironmentVariables.ZOOKEEPER_PASSWORD_RAW)) {
                envVarsOverlay.put(EnvironmentVariables.ZOOKEEPER_PASSWORD_RAW, zookeeperPasswordRaw);
            }
            if (!envVarsOverlay.containsKey(EnvironmentVariables.ZOOKEEPER_PASSWORD_ENCODE)) {
                String zkPasswordEncode = System.getProperty("zookeeper.password.encode", "true");
                envVarsOverlay.put(EnvironmentVariables.ZOOKEEPER_PASSWORD_ENCODE, zkPasswordEncode);
            }
        }
        return envVarsOverlay;
    }

}
