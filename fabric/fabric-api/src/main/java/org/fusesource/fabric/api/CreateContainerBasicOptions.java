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
import java.net.URISyntaxException;
import java.util.*;

public class CreateContainerBasicOptions<T extends CreateContainerBasicOptions> extends CreateEnsembleOptions implements CreateContainerOptions {


    public static class Builder<B extends Builder> extends CreateEnsembleOptions.Builder<B> {

        String name;
        String parent;
        String providerType;
        boolean ensembleServer;
        String preferredAddress;
        Map<String, Properties> systemProperties = new HashMap<String, Properties>();
        Integer number = 0;
        URI proxyUri;
        String zookeeperUrl;
        String jvmOpts;
        boolean adminAccess = true;
        String version;
        Map<String, CreateContainerMetadata> metadataMap = new HashMap<String, CreateContainerMetadata>();
        transient CreationStateListener creationStateListener = new NullCreationStateListener();

        public B preferredAddress(final String preferredAddress) {
            this.preferredAddress = preferredAddress;
            return (B) this;
        }

        public B ensembleServer(final boolean ensembleServer) {
            this.ensembleServer = ensembleServer;
            return (B) this;
        }

        public B number(final int number) {
            this.number = number;
            return (B) this;
        }


        public B name(final String name) {
            this.name = name;
            return (B) this;
        }

        public B parent(final String parent) {
            this.parent = parent;
            return (B) this;
        }

        public B providerType(final String providerType) {
            this.providerType = providerType;
            return (B) this;
        }

        public B zookeeperUrl(final String zookeeperUrl) {
            this.zookeeperUrl = zookeeperUrl;
            return (B) this;
        }

        public B proxyUri(final URI proxyUri) {
            this.proxyUri = proxyUri;
            return (B) this;
        }

        public B proxyUri(final String proxyUri) throws URISyntaxException {
            this.proxyUri = new URI(proxyUri);
            return (B) this;
        }

        public B jvmOpts(final String jvmOpts) {
            this.jvmOpts = jvmOpts;
            return (B) this;
        }

        public B adminAccess(final boolean adminAccess) {
            this.adminAccess = adminAccess;
            return (B) this;
        }

        public B creationStateListener(final CreationStateListener creationStateListener) {
            this.creationStateListener = creationStateListener;
            return (B) this;
        }

        public B version(String version) {
            this.version = version;
            return (B) this;
        }

        public String getName() {
            return name;
        }

        public String getParent() {
            return parent;
        }

        public String getProviderType() {
            return providerType;
        }

        public boolean isEnsembleServer() {
            return ensembleServer;
        }

        public String getPreferredAddress() {
            return preferredAddress;
        }

        public Map<String, Properties> getSystemProperties() {
            return systemProperties;
        }

        public Integer getNumber() {
            return number;
        }

        public URI getProxyUri() {
            return proxyUri;
        }

        public String getZookeeperUrl() {
            return zookeeperUrl;
        }

        public String getJvmOpts() {
            return jvmOpts;
        }

        public boolean isAdminAccess() {
            return adminAccess;
        }

        public String getVersion() {
            return version;
        }

        public Map<String, CreateContainerMetadata> getMetadataMap() {
            return metadataMap;
        }

        public CreationStateListener getCreationStateListener() {
            return creationStateListener;
        }

        public CreateContainerBasicOptions build() {
            return new CreateContainerBasicOptions(bindAddress, resolver, globalResolver, manualIp, minimumPort,
                    maximumPort, profiles, zooKeeperServerPort, zookeeperPassword, agentEnabled, autoImportEnabled,
                    importPath, users, name, parent, providerType, ensembleServer, preferredAddress, systemProperties,
                    number, proxyUri, zookeeperUrl, jvmOpts, adminAccess, version);
        }
    }


    final String name;
    final String parent;
    final String providerType;
    final boolean ensembleServer;
    final String preferredAddress;
    final Map<String, Properties> systemProperties;
    final Integer number;
    final URI proxyUri;
    final String zookeeperUrl;
    final String jvmOpts;
    final boolean adminAccess;
    final String version;
    final Map<String, CreateContainerMetadata<T>> metadataMap = new HashMap<String, CreateContainerMetadata<T>>();
    final transient CreationStateListener creationStateListener = new NullCreationStateListener();

    public CreateContainerBasicOptions(String bindAddress, String resolver, String globalResolver, String manualIp,
                                       int minimumPort, int maximumPort, Set<String> profiles, int getZooKeeperServerPort,
                                       String zookeeperPassword, boolean agentEnabled, boolean autoImportEnabled,
                                       String importPath, Map<String, String> users, String name, String parent,
                                       String providerType, boolean ensembleServer, String preferredAddress,
                                       Map<String, Properties> systemProperties, Integer number, URI proxyUri, String zookeeperUrl,
                                       String jvmOpts, boolean adminAccess, String version) {

        super(bindAddress, resolver, globalResolver, manualIp, minimumPort, maximumPort, profiles, getZooKeeperServerPort,
                zookeeperPassword, agentEnabled, autoImportEnabled, importPath, users);

        this.name = name;
        this.parent = parent;
        this.providerType = providerType;
        this.ensembleServer = ensembleServer;
        this.preferredAddress = preferredAddress;
        this.systemProperties = systemProperties;
        this.number = number;
        this.proxyUri = proxyUri;
        this.zookeeperUrl = zookeeperUrl;
        this.jvmOpts = jvmOpts;
        this.adminAccess = adminAccess;
        this.version = version;
    }

    public static Builder<? extends Builder> builder() {
        return new Builder<Builder>();
    }

    public String getProviderType() {
        return providerType;
    }

    @Override
    public CreateContainerOptions updateCredentials(String user, String credential) {
        throw new UnsupportedOperationException();
    }

    public String getName() {
        return name;
    }

    public String getParent() {
        return parent;
    }


    public boolean isEnsembleServer() {
        return ensembleServer;
    }

    public String getPreferredAddress() {
        return preferredAddress;
    }

    @Override
    public String getBindAddress() {
        return bindAddress;
    }

    public String getResolver() {
        return resolver;
    }

    @Override
    public String getManualIp() {
        return manualIp;
    }


    @Override
    public int getMinimumPort() {
        return minimumPort;
    }

    @Override
    public int getMaximumPort() {
        return maximumPort;
    }

    @Override
    public Map<String, Properties> getSystemProperties() {
        return systemProperties;
    }

    public Integer getNumber() {
        return number;
    }

    public URI getProxyUri() {
        return proxyUri;
    }

    public String getZookeeperUrl() {
        return zookeeperUrl;
    }

    public String getZookeeperPassword() {
        return zookeeperPassword;
    }

    public String getJvmOpts() {
        return jvmOpts;
    }

    public boolean isAdminAccess() {
        return adminAccess;
    }

    public CreationStateListener getCreationStateListener() {
        return creationStateListener;
    }

    public Map<String, CreateContainerMetadata<T>> getMetadataMap() {
        return metadataMap;
    }

    public String getVersion() {
        return version;
    }
}
