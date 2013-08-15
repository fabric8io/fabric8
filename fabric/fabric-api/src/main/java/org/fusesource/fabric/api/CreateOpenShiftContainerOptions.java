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

import org.codehaus.jackson.annotate.JsonProperty;

public class CreateOpenShiftContainerOptions
        extends CreateContainerBasicOptions<CreateOpenShiftContainerOptions> {

    public static class Builder extends CreateContainerBasicOptions.Builder<Builder> {
        @JsonProperty
        private String openshiftUser;
        @JsonProperty
        private String openshiftPassword;

        public Builder openshiftUser(final String openshiftUser) {
            this.openshiftUser = openshiftUser;
            return this;
        }

        public Builder openshiftPassword(final String openshiftPassword) {
            this.openshiftPassword = openshiftPassword;
            return this;
        }

        public String getOpenshiftUser() {
            return openshiftUser;
        }

        public String getOpenshiftPassword() {
            return openshiftPassword;
        }

        public CreateOpenShiftContainerOptions build() {
            return new CreateOpenShiftContainerOptions(bindAddress, resolver, globalResolver, manualIp, minimumPort,
                    maximumPort, profiles, zooKeeperServerPort, zooKeeperServerConnectionPort, zookeeperPassword, agentEnabled, autoImportEnabled,
                    importPath, users, name, parent, "openshift", ensembleServer, preferredAddress, systemProperties,
                    number, proxyUri, zookeeperUrl, jvmOpts, version, openshiftUser, openshiftPassword);
        }
    }

    @JsonProperty
    private final String openshiftUser;
    @JsonProperty
    private final String openshiftPassword;

    public CreateOpenShiftContainerOptions(String bindAddress, String resolver, String globalResolver,
                                           String manualIp, int minimumPort, int maximumPort,
                                           Set<String> profiles, int zooKeeperServerPort,
                                           int zooKeeperServerConnectionPort, String zookeeperPassword,
                                           boolean agentEnabled, boolean autoImportEnabled, String importPath,
                                           Map<String, String> users, String name, String parent,
                                           String providerType, boolean ensembleServer,
                                           String preferredAddress, Map<String, Properties> systemProperties,
                                           int number, URI proxyUri, String zookeeperUrl, String jvmOpts,
                                           String version, String openshiftUser, String openshiftPassword) {
        super(bindAddress, resolver, globalResolver, manualIp, minimumPort, maximumPort, profiles, zooKeeperServerPort, zooKeeperServerConnectionPort, zookeeperPassword, agentEnabled, autoImportEnabled, importPath, users, name, parent, providerType, ensembleServer, preferredAddress, systemProperties, number, proxyUri, zookeeperUrl, jvmOpts, false, version);
        this.openshiftUser = openshiftUser;
        this.openshiftPassword = openshiftPassword;
    }

    @Override
    public CreateContainerOptions updateCredentials(String newOpenshiftUser, String newOpenshiftPassword) {
        return new CreateOpenShiftContainerOptions(bindAddress, resolver, globalResolver, manualIp, minimumPort,
                maximumPort, profiles, zooKeeperServerPort, zooKeeperServerConnectionPort, zookeeperPassword, agentEnabled, autoImportEnabled,
                importPath, users, name, parent, "openshift", ensembleServer, preferredAddress, systemProperties,
                number, proxyUri, zookeeperUrl, jvmOpts, version, newOpenshiftUser != null ? newOpenshiftUser : openshiftUser, newOpenshiftPassword != null ? newOpenshiftPassword : openshiftPassword);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getOpenshiftUser() {
        return openshiftUser;
    }

    public String getOpenshiftPassword() {
        return openshiftPassword;
    }
}
