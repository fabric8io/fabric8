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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.fusesource.fabric.zookeeper.ZkDefs;

public class CreateContainerBasicOptions<T extends CreateContainerBasicOptions> implements CreateContainerOptions {

    protected String name;
    protected String parent;
    protected String providerType;
    protected URI providerURI;
    protected boolean ensembleServer;
    protected String preferredAddress;
    protected String resolver= ZkDefs.DEFAULT_RESOLVER;
    protected final Map<String, Properties> systemProperties = new HashMap<String, Properties>();
    protected Integer number = 1;
    protected URI proxyUri;
    protected String zookeeperUrl;
    protected String jvmOpts;
    protected boolean adminAccess = false;
    protected Map<String, CreateContainerMetadata<T>> metadataMap = new HashMap<String, CreateContainerMetadata<T>>();
    private transient CreationStateListener creationStateListener = new NullCreationStateListener();

    /**
     * Converts provider URI Query to a Map.
     *
     * @return
     */
    protected Map<String, String> getParameters() {
        Map<String, String> map = new HashMap<String, String>();
        if (providerURI != null && providerURI.getQuery() != null) {
            String[] params = providerURI.getQuery().split("&");
            for (String param : params) {
                String name = param.split("=")[0];
                String value = param.split("=")[1];
                map.put(name, value);
            }
        }
        return map;
    }

    public T preferredAddress(final String preferredAddress) {
        this.setPreferredAddress(preferredAddress);
        return (T) this;
    }

    public T resolver(final String resolver) {
        this.setResolver(resolver);
        return (T) this;
    }

    public T ensembleServer(final boolean ensembleServer) {
        this.ensembleServer = ensembleServer;
        return (T) this;
    }

    public T number(final int number) {
        this.number = number;
        return (T) this;
    }


    public T name(final String name) {
        this.name = name;
        return (T) this;
    }

    public T parent(final String parent) {
        this.parent = parent;
        return (T) this;
    }

    public T providerType(final String providerType) {
        this.providerType = providerType;
        return (T) this;
    }

    public T providerURI(final URI providerURI) {
        this.providerURI = providerURI;
        return (T) this;
    }

    public T providerUri(final String providerUri) throws URISyntaxException {
        this.providerURI = new URI(providerUri);
        return (T) this;
    }

    public T zookeeperUrl(final String zookeeperUrl) {
        this.zookeeperUrl = zookeeperUrl;
        return (T) this;
    }

    public T proxyUri(final URI proxyUri) {
        this.proxyUri = proxyUri;
        return (T) this;
    }

    public T proxyUri(final String proxyUri) throws URISyntaxException {
        this.proxyUri = new URI(proxyUri);
        return (T) this;
    }

    public T jvmOpts(final String jvmOpts) {
        this.jvmOpts = jvmOpts;
        return (T) this;
    }

    public T adminAccess(final boolean adminAccess) {
        this.adminAccess = adminAccess;
        return (T) this;
    }


    public T creationStateListener(final CreationStateListener creationStateListener) {
        this.creationStateListener = creationStateListener;
        return (T) this;
    }

    public String getProviderType() {
        return providerType != null ? providerType : (providerURI != null ? providerURI.getScheme() : null);
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public URI getProviderURI() {
        return providerURI;
    }

    public void setProviderURI(URI providerURI) {
        this.providerURI = providerURI;
    }

    public boolean isEnsembleServer() {
        return ensembleServer;
    }

    public void setEnsembleServer(boolean ensembleServer) {
        this.ensembleServer = ensembleServer;
    }

    public String getPreferredAddress() {
        return preferredAddress;
    }

    public void setPreferredAddress(String preferredAddress) {
        this.preferredAddress = preferredAddress;
    }

    public String getResolver() {
        return getParameters().get("resolver") != null ? getParameters().get("resolver") : resolver;
    }

    public void setResolver(String resolver) {
        if (Arrays.asList(ZkDefs.VALID_RESOLVERS).contains(resolver)) {
            this.resolver = resolver;
        } else {
            this.resolver = ZkDefs.DEFAULT_RESOLVER;
        }
    }

    @Override
    public Map<String,Properties> getSystemProperties() {
        return systemProperties;
    }



    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public URI getProxyUri() {
        return proxyUri;
    }

    public void setProxyUri(URI proxyUri) {
        this.proxyUri = proxyUri;
    }

    public String getZookeeperUrl() {
        return zookeeperUrl;
    }

    public void setZookeeperUrl(String zookeeperUrl) {
        this.zookeeperUrl = zookeeperUrl;
    }

    public String getJvmOpts() {
        return jvmOpts;
    }

    public void setJvmOpts(String jvmOpts) {
        this.jvmOpts = jvmOpts;
    }

    public boolean isAdminAccess() {
        return adminAccess;
    }

    public CreationStateListener getCreationStateListener() {
        return creationStateListener;
    }

    public void setCreationStateListener(CreationStateListener creationStateListener) {
        this.creationStateListener = creationStateListener;
    }


    public void setAdminAccess(boolean adminAccess) {
        this.adminAccess = adminAccess;
    }

    public Map<String, CreateContainerMetadata<T>> getMetadataMap() {
        return metadataMap;
    }
}
