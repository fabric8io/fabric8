/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.api.extensions;

import io.fabric8.kubernetes.api.model.config.AuthInfo;
import io.fabric8.kubernetes.api.model.config.Config;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.config.Context;
import io.fabric8.kubernetes.api.model.config.NamedAuthInfo;
import io.fabric8.kubernetes.api.model.config.NamedContext;
import io.fabric8.utils.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Helper class for working with the YAML config file thats located in
 * <code>~/.config/openshift/config</code> which is updated when you use commands
 * like <code>osc login</code> and <code>osc project myproject</code>
 */
public class Configs {
    public static final String OPENSHIFT_CONFIG_FILE_PROPERTY = "openshift.config.file";
    public static final String OPENSHIFT_CONFIG_FILE_ENV_VAR = "OPENSHIFTCONFIG";
    private static final transient Logger LOG = LoggerFactory.getLogger(Configs.class);

    public static Config parseConfigs() {
        File file = getOpenShiftConfigFile();
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

    public static File getOpenShiftConfigFile() {
        String file = System.getProperty(OPENSHIFT_CONFIG_FILE_PROPERTY);
        if (file != null) {
            return new File(file);
        }
        file = System.getenv(OPENSHIFT_CONFIG_FILE_ENV_VAR);
        if (file != null) {
            return new File(file);
        }
        String homeDir = System.getProperty("user.home", ".");
        return new File(homeDir, ".config/openshift/config");
    }
}
