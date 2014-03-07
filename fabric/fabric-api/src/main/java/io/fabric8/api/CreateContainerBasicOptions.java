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

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import io.fabric8.api.jcip.NotThreadSafe;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@NotThreadSafe
public class CreateContainerBasicOptions<T extends CreateContainerBasicOptions> extends CreateEnsembleOptions implements CreateContainerOptions {

    static final long serialVersionUID = -663983552172109587L;

    @JsonProperty
    final String name;
    @JsonProperty
    final String parent;
    @JsonProperty
    final String providerType;
    @JsonProperty
    final boolean ensembleServer;
    @JsonProperty
    final String preferredAddress;
    @JsonProperty
    final Integer number;
    @JsonProperty
    final URI proxyUri;
    @JsonProperty
    final String zookeeperUrl;
    @JsonProperty
    final String jvmOpts;
    @JsonProperty
    final boolean adminAccess;

    @JsonProperty
    final Map<String, Properties> systemProperties; // [TODO] make immutable

    final Map<String, CreateContainerMetadata<T>> metadataMap = new HashMap<String, CreateContainerMetadata<T>>();

    protected CreateContainerBasicOptions(String bindAddress, String resolver, String globalResolver, String manualIp,
                                       int minimumPort, int maximumPort, Set<String> profiles, String version,
                                       Map<String, String> dataStoreProperties, int getZooKeeperServerPort, int zooKeeperServerConnectionPort,
                                       String zookeeperPassword, boolean ensembleStart, boolean agentEnabled, boolean waitForProvision, long provisionTimeout, boolean autoImportEnabled,
                                       String importPath, Map<String, String> users, String name, String parent,
                                       String providerType, boolean ensembleServer, String preferredAddress,
                                       Map<String, Properties> systemProperties, Integer number, URI proxyUri, String zookeeperUrl,
                                       String jvmOpts, boolean adminAccess, boolean clean) {

        super(bindAddress, resolver, globalResolver, manualIp, minimumPort, maximumPort, profiles, version, dataStoreProperties, getZooKeeperServerPort,
                zooKeeperServerConnectionPort, DEFAULT_TICKTIME, DEFAULT_INIT_LIMIT, DEFAULT_SYNC_LIMIT, DEFAULT_DATA_DIR, zookeeperPassword, ensembleStart, agentEnabled, waitForProvision, provisionTimeout, DEFAULT_MIGRATION_TIMEOUT, autoImportEnabled, importPath, users, clean);

        this.name = name;
        this.parent = parent;
        this.providerType = providerType;
        this.ensembleServer = ensembleServer;
        this.preferredAddress = preferredAddress;
        this.number = number;
        this.proxyUri = proxyUri;
        this.zookeeperUrl = zookeeperUrl;
        this.jvmOpts = jvmOpts;
        this.adminAccess = adminAccess;
        this.systemProperties = systemProperties;
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

    public Map<String, CreateContainerMetadata<T>> getMetadataMap() {
        return metadataMap;
    }

    public String getVersion() {
        return version;
    }

    public static class Builder<B extends Builder<?>> extends CreateEnsembleOptions.Builder<B> {

        @JsonProperty
        String name;
        @JsonProperty
        String parent;
        @JsonProperty
        String providerType;
        @JsonProperty
        boolean ensembleServer;
        @JsonProperty
        String preferredAddress;
        @JsonProperty
        Map<String, Properties> systemProperties = new HashMap<String, Properties>();
        @JsonProperty
        Integer number = 0;
        @JsonProperty
        URI proxyUri;
        @JsonProperty
        String zookeeperUrl;
        @JsonProperty
        String jvmOpts;
        @JsonProperty
        boolean adminAccess = true;

        @JsonIgnore
        Map<String, CreateContainerMetadata> metadataMap = new HashMap<String, CreateContainerMetadata>();

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

        public Map<String, CreateContainerMetadata> getMetadataMap() {
            return metadataMap;
        }

        public CreateContainerBasicOptions build() {
            return new CreateContainerBasicOptions(bindAddress, resolver, globalResolver, manualIp, minimumPort,
                    maximumPort, profiles, version, dataStoreProperties, zooKeeperServerPort, zooKeeperServerConnectionPort, zookeeperPassword, ensembleStart, agentEnabled, waitForProvision, bootstrapTimeout, autoImportEnabled,
                    importPath, users, name, parent, providerType, ensembleServer, preferredAddress, systemProperties,
                    number, proxyUri, zookeeperUrl, jvmOpts, adminAccess, clean);
        }
    }
}
