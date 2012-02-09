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

public class BasicCreateContainerArguements implements CreateContainerArguments, Serializable {

    private static final long serialVersionUID = 7806030498786182100L;

    protected boolean ensembleServer;
    protected boolean debugContainer;
    protected int number = 1;
    protected URI proxyUri;


    public boolean isEnsembleServer() {
        return ensembleServer;
    }

    public void setEnsembleServer(boolean ensembleServer) {
        this.ensembleServer = ensembleServer;
    }

    public boolean isDebugContainer() {
        return debugContainer;
    }

    public void setDebugContainer(boolean debugContainer) {
        this.debugContainer = debugContainer;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public URI getProxyUri() {
        return proxyUri;
    }

    public void setProxyUri(URI proxyUri) {
        this.proxyUri = proxyUri;
    }
}
