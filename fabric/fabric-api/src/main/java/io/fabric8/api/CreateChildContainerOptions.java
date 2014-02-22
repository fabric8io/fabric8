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

package io.fabric8.api;

import org.codehaus.jackson.annotate.JsonProperty;

import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class CreateChildContainerOptions extends CreateContainerBasicOptions<CreateChildContainerOptions> {

    static final long serialVersionUID = -4093288463703483710L;

    @JsonProperty
    private final String jmxUser;
    @JsonProperty
    private final String jmxPassword;

    private CreateChildContainerOptions(String bindAddress, String resolver, String globalResolver, String manualIp, int minimumPort, int maximumPort, Set<String> profiles, String version, Map<String, String> dataStoreProperties, int zooKeeperServerPort, int zooKeeperServerConnectionPort, String zookeeperPassword, boolean ensembleStart, boolean agentEnabled, boolean autoImportEnabled, String importPath, Map<String, String> users, String name, String parent, String providerType, boolean ensembleServer, String preferredAddress, Map<String, Properties> systemProperties, int number, URI proxyUri, String zookeeperUrl, String jvmOpts, String jmxUser, String jmxPassword) {
        super(bindAddress, resolver, globalResolver, manualIp, minimumPort, maximumPort, profiles, version, dataStoreProperties, zooKeeperServerPort, zooKeeperServerConnectionPort, zookeeperPassword, ensembleStart, agentEnabled, false, 0, autoImportEnabled, importPath, users, name, parent, providerType, ensembleServer, preferredAddress, systemProperties, number, proxyUri, zookeeperUrl, jvmOpts, false, false);
        this.jmxUser = jmxUser;
        this.jmxPassword = jmxPassword;
    }

    @Override
    public CreateContainerOptions updateCredentials(String newJmxUser, String newJmxPassword) {
        return new CreateChildContainerOptions(bindAddress, resolver, globalResolver, manualIp, minimumPort,
                maximumPort, profiles, version, dataStoreProperties, zooKeeperServerPort, zooKeeperServerConnectionPort, zookeeperPassword, ensembleStart, agentEnabled, autoImportEnabled,
                importPath, users, name, parent, "child", ensembleServer, preferredAddress, systemProperties,
                number, proxyUri, zookeeperUrl, jvmOpts,
                newJmxUser != null ? newJmxUser : jmxUser, newJmxPassword != null ? newJmxPassword : jmxPassword);
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

    public static class Builder extends CreateContainerBasicOptions.Builder<Builder> {
        @JsonProperty
        private String jmxUser;
        @JsonProperty
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
                    maximumPort, profiles, version, dataStoreProperties, zooKeeperServerPort, zooKeeperServerConnectionPort, zookeeperPassword, ensembleStart, agentEnabled, autoImportEnabled,
                    importPath, users, name, parent, "child", ensembleServer, preferredAddress, systemProperties,
                    number, proxyUri, zookeeperUrl, jvmOpts,
                    jmxUser, jmxPassword);
        }
    }
}
