/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.kubernetes.api.extensions;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Helper class for working with the YAML config file thats located in
 * <code>~/.kube/config</code> which is updated when you use commands
 * like <code>oc login</code> and <code>oc project myproject</code>
 */
public class Configs {
    public static final String KUBERNETES_CONFIG_FILE_PROPERTY = "kubernetes.config.file";
    public static final String KUBERNETES_CONFIG_FILE_ENV_VAR = "KUBECONFIG";
    private static final transient Logger LOG = LoggerFactory.getLogger(Configs.class);
    private static final String defaultUserName = "admin";

    public static String currentUserName() {
        Config config = parseConfigs();
        if (config != null) {
            Context context = getCurrentContext(config);
            if (context != null) {
                String user = context.getUser();
                if (user != null) {
                    String[] parts = user.split("/");
                    if (parts != null && parts.length > 0) {
                        return parts[0];
                    }
                    return user;
                }
            }
        }
        return null;
    }

    public static Config parseConfigs() {
        File file = getKubernetesConfigFile();
        if (file.exists() && file.isFile()) {
            try {
                return KubernetesHelper.loadYaml(file, Config.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Returns the current context in the given config
     */
    public static Context getCurrentContext(Config config) {
        String contextName = config.getCurrentContext();
        if (contextName != null) {
            List<NamedContext> contexts = config.getContexts();
            if (contexts != null) {
                for (NamedContext context : contexts) {
                    if (Objects.equal(contextName, context.getName())) {
                        return context.getContext();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the current user token for the config and current context
     */
    public static String getUserToken(Config config, Context context) {
        AuthInfo authInfo = getUserAuthInfo(config, context);
        if (authInfo != null) {
            return authInfo.getToken();
        }
        return null;
    }

    /**
     * Returns the current {@link AuthInfo} for the current context and user
     */
    public static AuthInfo getUserAuthInfo(Config config, Context context) {
        AuthInfo authInfo = null;
        if (config != null && context != null) {
            String user = context.getUser();
            if (user != null) {
                List<NamedAuthInfo> users = config.getUsers();
                if (users != null) {
                    for (NamedAuthInfo namedAuthInfo : users) {
                        if (Objects.equal(user, namedAuthInfo.getName())) {
                            authInfo = namedAuthInfo.getUser();
                        }
                    }
                }
            }
        }
        return authInfo;
    }

    /**
     * Returns the current {@link Cluster} for the current context
     */
    public static Cluster getCluster(Config config, Context context) {
        Cluster cluster = null;
        if (config != null && context != null) {
            String clusterName = context.getCluster();
            if (clusterName != null) {
                List<NamedCluster> clusters = config.getClusters();
                if (clusters != null) {
                    for (NamedCluster namedCluster : clusters) {
                        if (Objects.equal(clusterName, namedCluster.getName())) {
                            cluster = namedCluster.getCluster();
                        }
                    }
                }
            }
        }
        return cluster;
    }

    public static File getKubernetesConfigFile() {
        String file = System.getProperty(KUBERNETES_CONFIG_FILE_PROPERTY);
        if (file != null) {
            return new File(file);
        }
        file = System.getenv(KUBERNETES_CONFIG_FILE_ENV_VAR);
        if (file != null) {
            return new File(file);
        }
        String homeDir = System.getProperty("user.home", ".");
        return new File(homeDir, ".kube/config");
    }
}
