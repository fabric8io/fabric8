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

package org.fusesource.fabric.api;

import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class CreateChildContainerOptions extends CreateContainerBasicOptions<CreateChildContainerOptions> {

    public static class Builder extends CreateContainerBasicOptions.Builder<Builder> {
        private String jmxUser;
        private String jmxPassword;

        public Builder jmxUser(final String jmxUser) {
            this.jmxUser = jmxUser;
            return this;
        }

        public Builder jmxPassword(final String jmxPassword) {
            this.jmxPassword = jmxPassword;
            return this;
        }

        public String getJmxUser() {
            return jmxUser;
        }

        public String getJmxPassword() {
            return jmxPassword;
        }

        public CreateChildContainerOptions build() {
            return new CreateChildContainerOptions(bindAddress, resolver, globalResolver, manualIp, minimumPort,
                    maximumPort, profiles, zooKeeperServerPort, zookeeperPassword, agentEnabled, autoImportEnabled,
                    importPath, users, name, parent, "child", ensembleServer, preferredAddress, systemProperties,
                    number, proxyUri, zookeeperUrl, jvmOpts, version, jmxUser, jmxPassword);
        }
    }

    private final String jmxUser;
    private final String jmxPassword;

    public CreateChildContainerOptions(String bindAddress, String resolver, String globalResolver, String manualIp, int minimumPort, int maximumPort, Set<String> profiles, int getZooKeeperServerPort, String zookeeperPassword, boolean agentEnabled, boolean autoImportEnabled, String importPath, Map<String, String> users, String name, String parent, String providerType, boolean ensembleServer, String preferredAddress, Map<String, Properties> systemProperties, int number, URI proxyUri, String zookeeperUrl, String jvmOpts, String version, String jmxUser, String jmxPassword) {
        super(bindAddress, resolver, globalResolver, manualIp, minimumPort, maximumPort, profiles, getZooKeeperServerPort, zookeeperPassword, agentEnabled, autoImportEnabled, importPath, users, name, parent, providerType, ensembleServer, preferredAddress, systemProperties, number, proxyUri, zookeeperUrl, jvmOpts, false, version);
        this.jmxUser = jmxUser;
        this.jmxPassword = jmxPassword;
    }

    @Override
    public CreateContainerOptions updateCredentials(String newJmxUser, String newJmxPassword) {
        return new CreateChildContainerOptions(bindAddress, resolver, globalResolver, manualIp, minimumPort,
                maximumPort, profiles, zooKeeperServerPort, zookeeperPassword, agentEnabled, autoImportEnabled,
                importPath, users, name, parent, "child", ensembleServer, preferredAddress, systemProperties,
                number, proxyUri, zookeeperUrl, jvmOpts, version, newJmxUser != null ? newJmxUser : jmxUser, newJmxPassword != null ? newJmxPassword : jmxPassword);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getJmxUser() {
        return jmxUser;
    }

    public String getJmxPassword() {
        return jmxPassword;
    }
}
