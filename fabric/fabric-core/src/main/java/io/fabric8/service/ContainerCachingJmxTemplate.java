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
package io.fabric8.service;

import io.fabric8.api.Container;

import javax.management.remote.JMXConnector;

/**
 * A Caching implementation of JmxTemplate which caches a connector for a given Container
 */
public class ContainerCachingJmxTemplate extends JmxTemplate {

    private final ContainerTemplate containerTemplate;

    public ContainerCachingJmxTemplate(ContainerTemplate containerTemplate) {
        this.containerTemplate = containerTemplate;
    }

    public ContainerTemplate getContainerTemplate() {
        return containerTemplate;
    }

    public Container getContainer() {
        return getContainerTemplate().getContainer();
    }

    @Override
    protected JMXConnector createConnector() {
        return getContainerTemplate().createConnector();
    }

}
