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
package io.fabric8.service.jclouds;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import io.fabric8.api.CreateContainerBasicOptions;
import io.fabric8.api.CreateContainerOptions;
import io.fabric8.api.CreateRemoteContainerOptions;
import io.fabric8.api.jcip.NotThreadSafe;
import org.jclouds.compute.ComputeService;

import java.net.URI;
import java.util.*;

/**
 * Arguments for creating a new container via JClouds
 */
@NotThreadSafe // because base class isn't
public class CreateJCloudsContainerOptions extends CreateContainerBasicOptions<CreateJCloudsContainerOptions> implements CreateRemoteContainerOptions {
    private static final long serialVersionUID = 4489740280396972109L;

    @JsonProperty
    private final String osFamily;
    @JsonProperty
    private final String osVersion;
    @JsonProperty
    private final String imageId;
    @JsonProperty
    private final String hardwareId;
    @JsonProperty
    private final String locationId;
    @JsonProperty
    private final String group;
    @JsonProperty
    private final String user;
    @JsonProperty
    private final String password;
    @JsonProperty
    private final String contextName;
    @JsonProperty
    private final String providerName;
    @JsonProperty
    private final String apiName;
    @JsonProperty
    private final String endpoint;
    @JsonProperty
    private final JCloudsInstanceType instanceType;
    @JsonProperty
    private final String identity;
    @JsonProperty
    private final String credential;
    @JsonProperty
    private final String owner;
    @JsonProperty
    private final Map<String, String> serviceOptions;
    @JsonProperty
    private final Map<String, String> nodeOptions;
    @JsonProperty
    private final int servicePort;
    @JsonProperty
    private final String publicKeyFile;
    @JsonIgnore
    private final transient ComputeService computeService;
    @JsonProperty
    private final String path;

    @JsonProperty
    private final Map<String, String> environmentalVariables; // keep immutable

    CreateJCloudsContainerOptions(String bindAddress, String resolver, String globalResolver, String manualIp,
                                         int minimumPort, int maximumPort, Set<String> profiles, String version, Map<String, String> dataStoreProperties, int zooKeeperServerPort, int zooKeeperServerConnectionPort,
                                         String zookeeperPassword, boolean ensembleStart, boolean agentEnabled, boolean autoImportEnabled,
                                         String importPath, Map<String, String> users, String name, String parent,
                                         String providerType, boolean ensembleServer, String preferredAddress,
                                         Map<String, Properties> systemProperties, int number, URI proxyUri, String zookeeperUrl,
                                         String jvmOpts, boolean adminAccess, boolean clean,
                                         String osFamily, String osVersion, String imageId,
                                         String hardwareId, String locationId, String group, String user, String password,
                                         String contextName, String providerName, String apiName, String endpoint,
                                         JCloudsInstanceType instanceType, String identity, String credential, String owner, Map<String, String> serviceOptions, Map<String, String> nodeOptions, int servicePort, String publicKeyFile,
                                         ComputeService computeService, String path, Map<String, String> environmentalVariables) {

        super(bindAddress, resolver, globalResolver, manualIp, minimumPort, maximumPort, profiles, version, dataStoreProperties, zooKeeperServerPort, zooKeeperServerConnectionPort,
                zookeeperPassword,ensembleStart, agentEnabled,false, 0, autoImportEnabled, importPath, users, name, parent, providerType,
                ensembleServer, preferredAddress, systemProperties, number, proxyUri, zookeeperUrl, jvmOpts, adminAccess, clean);

        this.osFamily = osFamily;
        this.osVersion = osVersion;
        this.imageId = imageId;
        this.hardwareId = hardwareId;
        this.locationId = locationId;
        this.group = group;
        this.user = user;
        this.password = password;
        this.contextName = contextName;
        this.providerName = providerName;
        this.apiName = apiName;
        this.endpoint = endpoint;
        this.instanceType = instanceType;
        this.identity = identity;
        this.credential = credential;
        this.owner = owner;
        this.serviceOptions = serviceOptions;
        this.nodeOptions = nodeOptions;
        this.servicePort = servicePort;
        this.publicKeyFile = publicKeyFile;
        this.computeService = computeService;
        this.path = path;
        this.environmentalVariables = Collections.unmodifiableMap(new HashMap<String, String>(environmentalVariables));
    }

    @Override
    public CreateContainerOptions updateCredentials(String newUser, String newPassword) {
        return new CreateJCloudsContainerOptions(getBindAddress(), getResolver(), getGlobalResolver(), getManualIp(), getMinimumPort(),
                getMaximumPort(), getProfiles(), getVersion(), getDataStoreProperties(), getZooKeeperServerPort(), getZooKeeperServerConnectionPort(), getZookeeperPassword(), isEnsembleStart(), isAgentEnabled(), isAutoImportEnabled(),
                getImportPath(), getUsers(), getName(), getParent(), "jclouds", isEnsembleServer(), getPreferredAddress(), getSystemProperties(),
                getNumber(), getProxyUri(), getZookeeperUrl(), getJvmOpts(), isAdminAccess(), isClean(),
                osFamily, osVersion, imageId, hardwareId, locationId,
                group, newUser != null ? newUser : user, newPassword != null ? newPassword : password,
                contextName, providerName, apiName, endpoint, instanceType, identity, credential,
                owner, serviceOptions, nodeOptions, servicePort, publicKeyFile, computeService, path, environmentalVariables);
    }

    CreateJCloudsContainerOptions updateComputeService(ComputeService computeService) {
        return new CreateJCloudsContainerOptions(getBindAddress(), getResolver(), getGlobalResolver(), getManualIp(), getMinimumPort(),
                getMaximumPort(), getProfiles(), getVersion(), getDataStoreProperties(), getZooKeeperServerPort(), getZooKeeperServerConnectionPort(), getZookeeperPassword(), isEnsembleStart(), isAgentEnabled(), isAutoImportEnabled(),
                getImportPath(), getUsers(), getName(), getParent(), "jclouds", isEnsembleServer(), getPreferredAddress(), getSystemProperties(),
                getNumber(), getProxyUri(), getZookeeperUrl(), getJvmOpts(), isAdminAccess(), isClean(),
                osFamily, osVersion, imageId, hardwareId, locationId,
                group, user, password, contextName, providerName, apiName, endpoint, instanceType, identity, credential,
                owner, serviceOptions, nodeOptions, servicePort, publicKeyFile, computeService, path, environmentalVariables);
    }


    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getHostNameContext() {
        return getProviderName();
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getEnvironmentalVariables() {
        return environmentalVariables;
    }

    public String getImageId() {
        return imageId;
    }

    public String getHardwareId() {
        return hardwareId;
    }

    public String getLocationId() {
        return locationId;
    }

    public String getGroup() {
        return group;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getContextName() {
        return contextName;
    }

    public String getProviderName() {
        return computeService != null ? computeService.getContext().unwrap().getProviderMetadata().getId() : providerName;
    }

    public String getApiName() {
        return computeService != null ? computeService.getContext().unwrap().getProviderMetadata().getApiMetadata().getId() : apiName;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public JCloudsInstanceType getInstanceType() {
        return instanceType;
    }

    public String getIdentity() {
        return identity;
    }

    public String getCredential() {
        return credential;
    }

    public String getOwner() {
        return owner;
    }

    public int getServicePort() {
        return servicePort;
    }

    public String getOsFamily() {
        return osFamily;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public String getPublicKeyFile() {
        return publicKeyFile;
    }

    public Map<String, String> getServiceOptions() {
        return serviceOptions;
    }

    public Map<String, String> getNodeOptions() {
        return nodeOptions;
    }

    public ComputeService getComputeService() {
        return computeService;
    }


    public CreateJCloudsContainerOptions clone() throws CloneNotSupportedException {
        return (CreateJCloudsContainerOptions) super.clone();
    }

    @Override
    public String toString() {
        return "CreateJCloudsContainerArguments{" +
                "imageId='" + imageId + '\'' +
                ", hardwareId='" + hardwareId + '\'' +
                ", locationId='" + locationId + '\'' +
                ", group='" + group + '\'' +
                ", user='" + user + '\'' +
                ", instanceType='" + instanceType + '\'' +
                '}';
    }

    public static class Builder extends CreateContainerBasicOptions.Builder<Builder> {

        @JsonProperty
        private String osFamily;
        @JsonProperty
        private String osVersion;
        @JsonProperty
        private String imageId;
        @JsonProperty
        private String hardwareId;
        @JsonProperty
        private String locationId;
        @JsonProperty
        private String group;
        @JsonProperty
        private String user;
        @JsonProperty
        private String password;
        @JsonProperty
        private String contextName;
        @JsonProperty
        private String providerName;
        @JsonProperty
        private String apiName;
        @JsonProperty
        private String endpoint;
        @JsonProperty
        private JCloudsInstanceType instanceType = JCloudsInstanceType.Fastest;
        @JsonProperty
        private String identity;
        @JsonProperty
        private String credential;
        @JsonProperty
        private String owner;
        @JsonProperty
        private final Map<String, String> serviceOptions = new HashMap<String, String>();
        @JsonProperty
        private final Map<String, String> nodeOptions = new HashMap<String, String>();
        @JsonProperty
        private int servicePort = 0;
        @JsonProperty
        private String publicKeyFile;
        private transient ComputeService computeService;
        @JsonProperty
        private String path = "~/containers/";
        @JsonProperty
        private Map<String, String> environmentalVariables = new HashMap<String, String>();


        public Builder osVersion(final String osVersion) {
            this.osVersion = osVersion;
            return this;
        }

        public Builder osFamily(final String osFamily) {
            this.osFamily = osFamily;
            return this;
        }

        public Builder imageId(final String imageId) {
            this.imageId = imageId;
            return this;
        }

        public Builder hardwareId(final String hardwareId) {
            this.hardwareId = hardwareId;
            return this;
        }

        public Builder locationId(final String locationId) {
            this.locationId = locationId;
            return this;
        }

        public Builder group(final String group) {
            this.group = group;
            return this;
        }

        public Builder user(final String user) {
            this.user = user;
            return this;
        }


        public Builder password(final String password) {
            this.password = password;
            return this;
        }

        public Builder computeService(final ComputeService computeService) {
            this.computeService = computeService;
            return this;
        }

        public Builder contextName(final String contextName) {
            this.contextName = contextName;
            return this;
        }

        public Builder providerName(final String providerName) {
            this.providerName = providerName;
            return this;
        }

        public Builder apiName(final String apiName) {
            this.apiName = apiName;
            return this;
        }

        public Builder endpoint(final String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder instanceType(final JCloudsInstanceType instanceType) {
            this.instanceType = instanceType;
            return this;
        }

        public Builder identity(final String identity) {
            this.identity = identity;
            return this;
        }

        public Builder credential(final String credential) {
            this.credential = credential;
            return this;
        }

        /**
         * Use servie options instead.
         *
         * @param owner
         * @return
         */
        @Deprecated
        public Builder owner(final String owner) {
            this.owner = owner;
            if (owner != null) {
                this.serviceOptions.put("owner", owner);
            }
            return this;
        }

        public Builder nodeOptions(final Map<String, String> nodeOptions) {
            if (nodeOptions != null) {
                for (Map.Entry<String, String> entry : nodeOptions.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    this.nodeOptions.put(key, value);
                }
            }
            return this;
        }

        public Builder serviceOptions(final Map<String, String> serviceOptions) {
            if (serviceOptions != null) {
                for (Map.Entry<String, String> entry : serviceOptions.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    this.serviceOptions.put(key, value);
                }
            }
            return this;
        }


        public Builder servicePort(int servicePort) {
            this.servicePort = servicePort;
            return this;
        }

        public Builder publicKeyFile(final String publicKeyFile) {
            this.publicKeyFile = publicKeyFile;
            return this;
        }


        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder environmentalVariables(Map<String, String> environmentalVariables) {
            this.environmentalVariables = environmentalVariables;
            return this;
        }

        public Builder environmentalVariable(String key, String value) {
            this.environmentalVariables.put(key, value);
            return this;
        }

        public Builder environmentalVariable(String entry) {
            if (entry.contains("=")) {
                String key = entry.substring(0, entry.indexOf("="));
                String value = entry.substring(entry.indexOf("=") + 1);
                environmentalVariable(key, value);
            }
            return this;
        }

        public Builder environmentalVariable(List<String> entries) {
            if (entries != null) {
                for (String entry : entries) {
                    environmentalVariable(entry);
                }
            }
            return this;
        }

        public void setComputeService(ComputeService computeService) {
            this.computeService = computeService;
        }

        public String getOsFamily() {
            return osFamily;
        }

        public String getOsVersion() {
            return osVersion;
        }

        public String getImageId() {
            return imageId;
        }

        public String getHardwareId() {
            return hardwareId;
        }

        public String getLocationId() {
            return locationId;
        }

        public String getGroup() {
            return group;
        }

        public String getUser() {
            return user;
        }

        public String getPassword() {
            return password;
        }

        public String getContextName() {
            return contextName;
        }

        public String getProviderName() {
            return providerName;
        }

        public String getApiName() {
            return apiName;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public JCloudsInstanceType getInstanceType() {
            return instanceType;
        }

        public String getIdentity() {
            return identity;
        }

        public String getCredential() {
            return credential;
        }

        public String getOwner() {
            return owner;
        }

        public Map<String, String> getServiceOptions() {
            return serviceOptions;
        }

        public Map<String, String> getNodeOptions() {
            return nodeOptions;
        }

        public int getServicePort() {
            return servicePort;
        }

        public String getPublicKeyFile() {
            return publicKeyFile;
        }

        public Object getComputeService() {
            return computeService;
        }

        public String getPath() {
            return path;
        }

        public void setEnvironmentalVariables(Map<String, String> environmentalVariables) {
            this.environmentalVariables = environmentalVariables;
        }

        public CreateJCloudsContainerOptions build() {
            return new CreateJCloudsContainerOptions(getBindAddress(), getResolver(), getGlobalResolver(), getManualIp(), getMinimumPort(),
                    getMaximumPort(), getProfiles(), getVersion(), getDataStoreProperties(), getZooKeeperServerPort(), getZooKeeperServerConnectionPort(), getZookeeperPassword(), isEnsembleStart(), isAgentEnabled(), isAutoImportEnabled(),
                    getImportPath(), getUsers(), getName(), getParent(), "jclouds", isEnsembleServer(), getPreferredAddress(), getSystemProperties(),
                    getNumber(), getProxyUri(), getZookeeperUrl(), getJvmOpts(), isAdminAccess(), isClean(),
                    osFamily, osVersion, imageId, hardwareId, locationId,
                    group, user, password, contextName, providerName, apiName, endpoint, instanceType, identity, credential,
                    owner, serviceOptions, nodeOptions, servicePort, publicKeyFile, computeService, path, environmentalVariables);
        }
    }
}
