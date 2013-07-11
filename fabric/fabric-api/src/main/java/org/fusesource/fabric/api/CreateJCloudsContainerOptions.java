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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Arguments for creating a new container via JClouds
 */
public class CreateJCloudsContainerOptions extends CreateContainerBasicOptions<CreateJCloudsContainerOptions> implements CreateRemoteContainerOptions {
    private static final long serialVersionUID = 4489740280396972109L;

    public static class Builder extends CreateContainerBasicOptions.Builder<Builder> {

        private String osFamily;
        private String osVersion;
        private String imageId;
        private String hardwareId;
        private String locationId;
        private String group;
        private String user;
        private String password;
        private String contextName;
        private String providerName;
        private String apiName;
        private String endpoint;
        private JCloudsInstanceType instanceType = JCloudsInstanceType.Fastest;
        private String identity;
        private String credential;
        private String owner;
        private final Map<String, String> serviceOptions = new HashMap<String, String>();
        private final Map<String, String> nodeOptions = new HashMap<String, String>();
        private int servicePort = 0;
        private String publicKeyFile;
        private transient Object computeService;
        private String path = "~/containers/";


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

        public CreateJCloudsContainerOptions build() {
            return new CreateJCloudsContainerOptions(bindAddress, resolver, globalResolver, manualIp, minimumPort,
                    maximumPort, profiles, zooKeeperServerPort, zookeeperPassword, agentEnabled, autoImportEnabled,
                    importPath, users, name, parent, providerType, ensembleServer, preferredAddress, systemProperties,
                    number, proxyUri, zookeeperUrl, jvmOpts, adminAccess, version, osFamily, osVersion, imageId, hardwareId, locationId,
                    group, user, password, contextName, providerName, apiName, endpoint, instanceType, identity, credential,
                    owner, serviceOptions, nodeOptions, servicePort, publicKeyFile, computeService, path);
        }
    }


    private final String osFamily;
    private final String osVersion;
    private final String imageId;
    private final String hardwareId;
    private final String locationId;
    private final String group;
    private final String user;
    private final String password;
    private final String contextName;
    private final String providerName;
    private final String apiName;
    private final String endpoint;
    private final JCloudsInstanceType instanceType;
    private final String identity;
    private final String credential;
    private final String owner;
    private final Map<String, String> serviceOptions;
    private final Map<String, String> nodeOptions;
    private final int servicePort;
    private final String publicKeyFile;
    private final transient Object computeService;
    private final String path;

    public CreateJCloudsContainerOptions(String bindAddress, String resolver, String globalResolver, String manualIp,
                                         int minimumPort, int maximumPort, Set<String> profiles, int getZooKeeperServerPort,
                                         String zookeeperPassword, boolean agentEnabled, boolean autoImportEnabled,
                                         String importPath, Map<String, String> users, String name, String parent,
                                         String providerType, boolean ensembleServer, String preferredAddress,
                                         Map<String, Properties> systemProperties, int number, URI proxyUri, String zookeeperUrl,
                                         String jvmOpts, boolean adminAccess, String version, String osFamily, String osVersion, String imageId,
                                         String hardwareId, String locationId, String group, String user, String password,
                                         String contextName, String providerName, String apiName, String endpoint,
                                         JCloudsInstanceType instanceType, String identity, String credential, String owner, Map<String, String> serviceOptions, Map<String, String> nodeOptions, int servicePort, String publicKeyFile,
                                         Object computeService, String path) {

        super(bindAddress, resolver, globalResolver, manualIp, minimumPort, maximumPort, profiles, getZooKeeperServerPort,
                zookeeperPassword, agentEnabled, autoImportEnabled, importPath, users, name, parent, providerType,
                ensembleServer, preferredAddress, systemProperties, number, proxyUri, zookeeperUrl, jvmOpts, adminAccess, version);

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
    }

    @Override
    public CreateContainerOptions updateCredentials(String newUser, String newPassword) {
        return new CreateJCloudsContainerOptions(bindAddress, resolver, globalResolver, manualIp, minimumPort,
                maximumPort, profiles, zooKeeperServerPort, zookeeperPassword, agentEnabled, autoImportEnabled,
                importPath, users, name, parent, providerType, ensembleServer, preferredAddress, systemProperties,
                number, proxyUri, zookeeperUrl, jvmOpts, adminAccess, version, osFamily, osVersion, imageId, hardwareId, locationId,
                group, newUser != null ? newUser : user, newPassword != null ? newPassword : password,
                contextName, providerName, apiName, endpoint, instanceType, identity, credential,
                owner, serviceOptions, nodeOptions, servicePort, publicKeyFile, computeService, path);
    }


    public static Builder builder() {
        return new Builder();
    }

    public String getPath() {
        return path;
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

    public Object getComputeService() {
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

}
