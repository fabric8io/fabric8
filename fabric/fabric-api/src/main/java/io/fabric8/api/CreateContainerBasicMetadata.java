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

import java.util.HashMap;
import java.util.Map;

public class CreateContainerBasicMetadata<O extends CreateContainerOptions> implements CreateContainerMetadata<O> {

    static final long serialVersionUID = 7432148266874950445L;

    private String containerName = "<not available>";
    private O createOptions;
    private transient Throwable failure;
    private transient Container container;
    private String overridenResolver;
    private final Map<String,String> containerConfiguration = new HashMap<String, String>();

    public boolean isSuccess() {
        return failure == null;
    }

    public Throwable getFailure() {
        return failure;
    }

    public void setFailure(Throwable failure) {
        this.failure = failure;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public Container getContainer() {
        return container;
    }

    public void setContainer(Container container) {
        this.container = container;
    }

    public O getCreateOptions() {
        return createOptions;
    }

    public void setCreateOptions(CreateContainerOptions createOptions) {
        this.createOptions = (O) createOptions;
    }

    @Override
    public Map<String, String> getContainerConfiguration() {
        return containerConfiguration;
    }

    public String getOverridenResolver() {
        return overridenResolver;
    }

    @Override
    public void updateCredentials(String user, String credential) {
        this.createOptions = (O) createOptions.updateCredentials(user, credential);
    }

    public void setOverridenResolver(String overridenResolver) {
        this.overridenResolver = overridenResolver;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Container: ").append(containerName).append(".");
        if (getCreateOptions().isEnsembleServer() && getCreateOptions().getZookeeperPassword() != null) {
            sb.append("Registry Password: ").append(getCreateOptions().getZookeeperPassword());
        }
        return  sb.toString();
    }
}
