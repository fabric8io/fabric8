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
package io.fabric8.docker.provider;

import io.fabric8.api.CreateContainerBasicOptions;
import io.fabric8.api.CreateContainerOptions;
import io.fabric8.api.CreateRemoteContainerOptions;
import io.fabric8.docker.api.container.ContainerConfig;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonProperty;

public class CreateDockerContainerOptions extends CreateContainerBasicOptions<CreateDockerContainerOptions> implements CreateRemoteContainerOptions {
    private static final long serialVersionUID = 4489740280396972109L;


    public static class Builder extends CreateContainerBasicOptions.Builder<Builder> {
        @JsonProperty
        private String image;
        @JsonProperty
        private String[] cmd;
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

        public Builder image(String image) {
            this.image = image;
            return this;
        }

        public Builder cmd(String[] cmd) {
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

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public String[] getCmd() {
            return cmd;
        }

        public void setCmd(String[] cmd) {
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

        public CreateDockerContainerOptions build() {
            return new CreateDockerContainerOptions(getBindAddress(), getResolver(), getGlobalResolver(), getManualIp(), getMinimumPort(),
                    getMaximumPort(), getProfiles(), getVersion(), getDataStoreProperties(), getZooKeeperServerPort(), getZooKeeperServerConnectionPort(),
                    getZookeeperPassword(), isEnsembleStart(), isAgentEnabled(), isWaitForProvision(), getBootstrapTimeout(), isAutoImportEnabled(), getImportPath(),
                    getUsers(), getName(), getParent(), DockerConstants.SCHEME, isEnsembleServer(), getPreferredAddress(), getSystemProperties(), getNumber(),
                    getProxyUri(), getZookeeperUrl(), getJvmOpts(), isAdminAccess(), isClean(), image, cmd, entrypoint, user, workingDir, gearProfile, environmentalVariables);

        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonProperty
    private final String image;
    @JsonProperty
    private final String[] cmd;
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


    public ContainerConfig createContainerConfig() {
        ContainerConfig answer = new ContainerConfig();
        answer.setImage(image);
        answer.setCmd(cmd);
        answer.setEntrypoint(entrypoint);
        answer.setUser(user);
        answer.setWorkingDir(workingDir);

        // TODO how to pass in environmentalVariables
        return answer;
    }

    private CreateDockerContainerOptions(String bindAddress, String resolver, String globalResolver, String manualIp, int minimumPort, int maximumPort, Set<String> profiles, String version, Map<String, String> dataStoreProperties, int getZooKeeperServerPort, int zooKeeperServerConnectionPort, String zookeeperPassword, boolean ensembleStart, boolean agentEnabled, boolean waitForProvision, long provisionTimeout, boolean autoImportEnabled, String importPath, Map<String, String> users, String name, String parent, String providerType, boolean ensembleServer, String preferredAddress, Map<String, Properties> systemProperties, Integer number, URI proxyUri, String zookeeperUrl, String jvmOpts, boolean adminAccess, boolean clean, String image, String[] cmd, String entrypoint, String user, String workingDir, String gearProfile, Map<String, String> environmentalVariables) {
        super(bindAddress, resolver, globalResolver, manualIp, minimumPort, maximumPort, profiles, version, dataStoreProperties, getZooKeeperServerPort, zooKeeperServerConnectionPort, zookeeperPassword, ensembleStart, agentEnabled, waitForProvision, provisionTimeout, autoImportEnabled, importPath, users, name, parent, providerType, ensembleServer, preferredAddress, systemProperties, number, proxyUri, zookeeperUrl, jvmOpts, adminAccess, clean);
        this.image = image;
        this.cmd = cmd;
        this.entrypoint = entrypoint;
        this.user = user;
        this.workingDir = workingDir;
        this.gearProfile = gearProfile;
        this.environmentalVariables = environmentalVariables;
    }

    @Override
    public CreateContainerOptions updateCredentials(String user, String credential) {
        return new CreateDockerContainerOptions(getBindAddress(), getResolver(), getGlobalResolver(), getManualIp(), getMinimumPort(),
                getMaximumPort(), getProfiles(), getVersion(), getDataStoreProperties(), getZooKeeperServerPort(), getZooKeeperServerConnectionPort(),
                getZookeeperPassword(), isEnsembleStart(), isAgentEnabled(), isWaitForProvision(), getBootstrapTimeout(), isAutoImportEnabled(), getImportPath(),
                getUsers(), getName(), getParent(), getProviderType(), isEnsembleServer(), getPreferredAddress(), getSystemProperties(), getNumber(),
                getProxyUri(), getZookeeperUrl(), getJvmOpts(), isAdminAccess(), isClean(), image, cmd, entrypoint, user, workingDir, gearProfile, environmentalVariables);
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

    public String[] getCmd() {
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
}
