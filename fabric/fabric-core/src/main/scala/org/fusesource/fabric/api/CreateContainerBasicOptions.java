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

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

public class CreateContainerBasicOptions<T extends CreateContainerBasicOptions> implements CreateContainerOptions, Serializable {

    private static final long serialVersionUID = 7806030498786182100L;

    protected String name;
    protected String parent;
    protected String providerType;
    protected URI providerURI;
    protected boolean ensembleServer;
    protected boolean debugContainer;
    protected Integer number = 1;
    protected URI proxyUri;
    protected String zookeeperUrl;


    public T ensembleServer(final boolean ensembleServer) {
        this.ensembleServer = ensembleServer;
        return (T) this;
    }

    public T debugContainer(final boolean debugContainer) {
        this.debugContainer = debugContainer;
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

    public String getName() {
        return name;
    }

    public String getParent() {
        return parent;
    }

    public String getProviderType() {
        return providerURI != null ? providerURI.getScheme() : providerType;
    }

    public URI getProviderURI() {
        return providerURI;
    }

    public boolean isEnsembleServer() {
        return ensembleServer;
    }

    public boolean isDebugContainer() {
        return debugContainer;
    }

    public int getNumber() {
        return number;
    }

    public URI getProxyUri() {
        return proxyUri;
    }

    public String getZookeeperUrl() {
        return zookeeperUrl;
    }
}
