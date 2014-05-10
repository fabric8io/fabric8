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
package io.fabric8.docker.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.api.CreateContainerBasicOptions;
import io.fabric8.api.CreateContainerOptions;
import io.fabric8.api.CreateRemoteContainerOptions;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class CreateDockerContainerOptions extends CreateContainerBasicOptions<CreateDockerContainerOptions> implements CreateRemoteContainerOptions {
    private static final long serialVersionUID = 4489740280396972109L;


    public static class Builder extends CreateContainerBasicOptions.Builder<Builder> {
        @JsonProperty
        private String image;
        @JsonProperty
        private List<String> cmd;
        @JsonProperty
        private String entrypoint;
        @JsonProperty
        private String user;
        @JsonProperty
        private String workingDir;
        // TODO required?
        @JsonProperty
        private String gearProfile;
        @JsonProperty
        private Map<String, String> environmentalVariables = new HashMap<String, String>();
        @JsonProperty
        private Map<String, Integer> internalPorts = new HashMap<String, Integer>();
        @JsonProperty
        private Map<String, Integer> externalPorts = new HashMap<String, Integer>();

        public Builder image(String image) {
            this.image = image;
            return this;
        }

        public Builder cmd(List<String> cmd) {
            this.cmd = cmd;
            return this;
        }

        public Builder entrypoint(String entrypoint) {
            this.entrypoint = entrypoint;
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder workingDir(String workingDir) {
            this.workingDir = workingDir;
            return this;
        }

        public Builder gearProfile(String gearProfile) {
            this.gearProfile = gearProfile;
            return this;
        }

        public Builder environmentalVariables(Map<String, String> environmentalVariables) {
            this.environmentalVariables = environmentalVariables;
            return this;
        }

        public Builder internalPorts(Map<String, Integer> internalPorts) {
            this.internalPorts = internalPorts;
            return this;
        }

        public Builder externalPorts(Map<String, Integer> externalPorts) {
            this.externalPorts = externalPorts;
            return this;
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public List<String> getCmd() {
            return cmd;
        }

        public void setCmd(List<String> cmd) {
            this.cmd = cmd;
        }

        public String getEntrypoint() {
            return entrypoint;
        }

        public void setEntrypoint(String entrypoint) {
            this.entrypoint = entrypoint;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getWorkingDir() {
            return workingDir;
        }

        public void setWorkingDir(String workingDir) {
            this.workingDir = workingDir;
        }

        public String getGearProfile() {
            return gearProfile;
        }

        public void setGearProfile(String gearProfile) {
            this.gearProfile = gearProfile;
        }

        public Map<String, String> getEnvironmentalVariables() {
            return environmentalVariables;
        }

        public void setEnvironmentalVariables(Map<String, String> environmentalVariables) {
            this.environmentalVariables = environmentalVariables;
        }

        public Map<String, Integer> getInternalPorts() {
            return internalPorts;
        }

        public void setInternalPorts(Map<String, Integer> internalPorts) {
            this.internalPorts = internalPorts;
        }

        public Map<String, Integer> getExternalPorts() {
            return externalPorts;
        }

        public void setExternalPorts(Map<String, Integer> externalPorts) {
            this.externalPorts = externalPorts;
        }

        public CreateDockerContainerOptions build() {
            return new CreateDockerContainerOptions(getBindAddress(), getResolver(), getGlobalResolver(), getManualIp(), getMinimumPort(),
                    getMaximumPort(), getProfiles(), getVersion(), getDataStoreProperties(), getZooKeeperServerPort(), getZooKeeperServerConnectionPort(),
                    getZookeeperPassword(), isEnsembleStart(), isAgentEnabled(), isWaitForProvision(), getBootstrapTimeout(), isAutoImportEnabled(), getImportPath(),
                    getUsers(), getName(), getParent(), DockerConstants.SCHEME, isEnsembleServer(), getPreferredAddress(), getSystemProperties(), getNumber(),
                    getProxyUri(), getZookeeperUrl(), getJvmOpts(), isAdminAccess(), isClean(), image, cmd, entrypoint, user, workingDir, gearProfile, environmentalVariables, internalPorts, externalPorts);

        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonProperty
    private final String image;
    @JsonProperty
    private final List<String> cmd;
    @JsonProperty
    private final String entrypoint;
    @JsonProperty
    private final String user;
    @JsonProperty
    private final String workingDir;
    // TODO required?
    @JsonProperty
    private final String gearProfile;
    @JsonProperty
    private final Map<String, String> environmentalVariables;
    @JsonProperty
    private final Map<String, Integer> internalPorts;
    @JsonProperty
    private final Map<String, Integer> externalPorts;


    private CreateDockerContainerOptions(String bindAddress, String resolver, String globalResolver, String manualIp, int minimumPort, int maximumPort, Set<String> profiles, String version, Map<String, String> dataStoreProperties, int getZooKeeperServerPort, int zooKeeperServerConnectionPort, String zookeeperPassword, boolean ensembleStart, boolean agentEnabled, boolean waitForProvision, long provisionTimeout, boolean autoImportEnabled, String importPath, Map<String, String> users, String name, String parent, String providerType, boolean ensembleServer, String preferredAddress, Map<String, Properties> systemProperties, Integer number, URI proxyUri, String zookeeperUrl, String jvmOpts, boolean adminAccess, boolean clean, String image, List<String> cmd, String entrypoint, String user, String workingDir, String gearProfile, Map<String, String> environmentalVariables, Map<String, Integer> internalPorts, Map<String, Integer> externalPorts) {
        super(bindAddress, resolver, globalResolver, manualIp, minimumPort, maximumPort, profiles, version, dataStoreProperties, getZooKeeperServerPort, zooKeeperServerConnectionPort, zookeeperPassword, ensembleStart, agentEnabled, waitForProvision, provisionTimeout, autoImportEnabled, importPath, users, name, parent, providerType, ensembleServer, preferredAddress, systemProperties, number, proxyUri, zookeeperUrl, jvmOpts, adminAccess, clean);
        this.image = image;
        this.cmd = cmd;
        this.entrypoint = entrypoint;
        this.user = user;
        this.workingDir = workingDir;
        this.gearProfile = gearProfile;
        this.environmentalVariables = environmentalVariables;
        this.internalPorts = internalPorts;
        this.externalPorts = externalPorts;
    }

    @Override
    public CreateContainerOptions updateCredentials(String user, String credential) {
        return new CreateDockerContainerOptions(getBindAddress(), getResolver(), getGlobalResolver(), getManualIp(), getMinimumPort(),
                getMaximumPort(), getProfiles(), getVersion(), getDataStoreProperties(), getZooKeeperServerPort(), getZooKeeperServerConnectionPort(),
                getZookeeperPassword(), isEnsembleStart(), isAgentEnabled(), isWaitForProvision(), getBootstrapTimeout(), isAutoImportEnabled(), getImportPath(),
                getUsers(), getName(), getParent(), getProviderType(), isEnsembleServer(), getPreferredAddress(), getSystemProperties(), getNumber(),
                getProxyUri(), getZookeeperUrl(), getJvmOpts(), isAdminAccess(), isClean(), image, cmd, entrypoint, user, workingDir, gearProfile, environmentalVariables, internalPorts, externalPorts);
    }

    public CreateDockerContainerOptions updateManualIp(String manualip) {
        String resolver = "manualip";
        return new CreateDockerContainerOptions(getBindAddress(), resolver, resolver, manualip, getMinimumPort(),
                getMaximumPort(), getProfiles(), getVersion(), getDataStoreProperties(), getZooKeeperServerPort(), getZooKeeperServerConnectionPort(),
                getZookeeperPassword(), isEnsembleStart(), isAgentEnabled(), isWaitForProvision(), getBootstrapTimeout(), isAutoImportEnabled(), getImportPath(),
                getUsers(), getName(), getParent(), getProviderType(), isEnsembleServer(), getPreferredAddress(), getSystemProperties(), getNumber(),
                getProxyUri(), getZookeeperUrl(), getJvmOpts(), isAdminAccess(), isClean(), image, cmd, entrypoint, user, workingDir, gearProfile, environmentalVariables, internalPorts, externalPorts);
    }


    @Override
    public String getHostNameContext() {
        return "docker";
    }

    @Override
    public String getPath() {
        // TODO
        return null;
    }

    public String getImage() {
        return image;
    }

    public List<String> getCmd() {
        return cmd;
    }

    public String getEntrypoint() {
        return entrypoint;
    }

    public String getUser() {
        return user;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public String getGearProfile() {
        return gearProfile;
    }

    public Map<String, String> getEnvironmentalVariables() {
        return environmentalVariables;
    }

    public Map<String, Integer> getInternalPorts() {
        return internalPorts;
    }

    public Map<String, Integer> getExternalPorts() {
        return externalPorts;
    }
}
