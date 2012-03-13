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

public class CreateContainerBasicMetadata<O extends CreateContainerOptions> implements CreateContainerMetadata<O> {

    private String containerName;
    private O createOptions;
    private transient Throwable failure;
    private transient Container container;

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

    public void setCreateOptions(O createOptions) {
        this.createOptions = createOptions;
    }

    @Override
    public String toString() {
        return  containerName;
    }
}
