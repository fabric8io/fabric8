/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.fabric8.openshift;

import org.codehaus.jackson.annotate.JsonProperty;
import io.fabric8.api.CreateContainerBasicOptions;
import io.fabric8.api.CreateRemoteContainerOptions;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class CreateOpenshiftContainerOptions extends CreateContainerBasicOptions<CreateOpenshiftContainerOptions> implements CreateRemoteContainerOptions {
    private static final long serialVersionUID = 4489740280396972109L;

    public static String OPENSHIFT_BROKER_HOST = "OPENSHIFT_BROKER_HOST";
    public static String OPENSHIFT_NAMESPACE = "OPENSHIFT_NAMESPACE";


    public static class Builder extends CreateContainerBasicOptions.Builder<Builder> {
        @JsonProperty
        private String serverUrl = System.getenv(OPENSHIFT_BROKER_HOST);
        @JsonProperty
        private String login;
        @JsonProperty
        private String password;
        @JsonProperty
        private String domain = System.getenv(OPENSHIFT_NAMESPACE);
        @JsonProperty
        private String gearProfile = "small";
        @JsonProperty
        private Map<String, String> environmentalVariables = new HashMap<String, String>();

        public Builder serverUrl(final String serverUrl) {
            this.serverUrl = serverUrl;
            return this;
        }

        public Builder login(final String login) {
            this.login = login;
            return this;
        }

        public Builder password(final String password) {
            this.password = password;
            return this;
        }

        public Builder domain(final String domain) {
            this.domain = domain;
            return this;
        }

        public Builder gearProfile(final String gearProfile) {
            this.gearProfile = gearProfile;
            return this;
        }


        public Builder environmentalVariables(final Map<String, String> environmentalVariables) {
            this.environmentalVariables = environmentalVariables;
            return this;
        }


        public String getServerUrl() {
            return serverUrl;
        }

        public void setServerUrl(String serverUrl) {
            this.serverUrl = serverUrl;
        }

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public Map<String, String> getEnvironmentalVariables() {
            return environmentalVariables;
        }

        public void setEnvironmentalVariables(Map<String, String> environmentalVariables) {
            this.environmentalVariables = environmentalVariables;
        }

        public CreateOpenshiftContainerOptions build() {
            return new CreateOpenshiftContainerOptions(getBindAddress(), getResolver(), getGlobalResolver(), getManualIp(), getMinimumPort(),
                    getMaximumPort(), getProfiles(), getVersion(), getDataStoreProperties(), getZooKeeperServerPort(), getZooKeeperServerConnectionPort(), getZookeeperPassword(), isEnsembleStart(), isAgentEnabled(), isAutoImportEnabled(),
                    getImportPath(), getUsers(), getName(), getParent(), "openshift", isEnsembleServer(), getPreferredAddress(), getSystemProperties(),
                    getNumber(), getProxyUri(), getZookeeperUrl(), getJvmOpts(), isAdminAccess(), isClean(),
                    serverUrl, login, password, domain, gearProfile, environmentalVariables);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonProperty
    private final String serverUrl;
    @JsonProperty
    private final String login;
    @JsonProperty
    private final String password;
    @JsonProperty
    private final String domain;
    @JsonProperty
    private final String gearProfile;
    @JsonProperty
    private final Map<String, String> environmentalVariables;


    public CreateOpenshiftContainerOptions(String bindAddress, String resolver, String globalResolver, String manualIp, int minimumPort, int maximumPort, Set<String> profiles, String version, Map<String, String> dataStoreProperties, int zooKeeperServerPort, int zooKeeperServerConnectionPort, String zookeeperPassword, boolean ensembleStart, boolean agentEnabled, boolean autoImportEnabled, String importPath, Map<String, String> users, String name, String parent, String providerType, boolean ensembleServer, String preferredAddress, Map<String, Properties> systemProperties, Integer number, URI proxyUri, String zookeeperUrl, String jvmOpts, boolean adminAccess, boolean clean, String serverUrl, String login, String password, String domain, String gearProfile, Map<String, String> environmentalVariables) {
        super(bindAddress, resolver, globalResolver, manualIp, minimumPort, maximumPort, profiles, version, dataStoreProperties, zooKeeperServerPort, zooKeeperServerConnectionPort, zookeeperPassword, ensembleStart, agentEnabled, false, 0, autoImportEnabled, importPath, users, name, parent, providerType, ensembleServer, preferredAddress, systemProperties, number, proxyUri, zookeeperUrl, jvmOpts, adminAccess, clean);
        this.serverUrl = serverUrl;
        this.login = login;
        this.password = password;
        this.domain = domain;
        this.gearProfile = gearProfile;
        this.environmentalVariables = environmentalVariables;
    }

    @Override
    public CreateOpenshiftContainerOptions updateCredentials(String user, String credential) {
        return new CreateOpenshiftContainerOptions(getBindAddress(), getResolver(), getGlobalResolver(), getManualIp(), getMinimumPort(),
                getMaximumPort(), getProfiles(), getVersion(), getDataStoreProperties(),  getZooKeeperServerPort(), getZooKeeperServerConnectionPort(), getZookeeperPassword(), isEnsembleStart(), isAgentEnabled(), isAutoImportEnabled(),
                getImportPath(), getUsers(), getName(), getParent(), "openshift", isEnsembleServer(), getPreferredAddress(), getSystemProperties(),
                getNumber(), getProxyUri(), getZookeeperUrl(), getJvmOpts(), isAdminAccess(), isClean(),
                serverUrl, user, password, domain, gearProfile, environmentalVariables);
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getDomain() {
        return domain;
    }

    public String getGearProfile() {
        return gearProfile;
    }

    @Override
    public String getHostNameContext() {
        return "openshift";
    }

    @Override
    public String getPath() {
        return "~/";
    }

    @Override
    public Map<String, String> getEnvironmentalVariables() {
        return environmentalVariables;
    }
}
