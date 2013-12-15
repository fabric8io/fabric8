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

/**
 * Helper methods for creating an AgentTemplate or JmxTemplate from an Agent
 */
public final class Containers {

    private Containers() {
        //Utility Class
    }

    public static JmxTemplateSupport newJmxTemplate(Container container, String username, String password) {
        return newContainerTemplate(container, username, password).getJmxTemplate();
    }

    public static JmxTemplateSupport newNonCachingJmxTemplate(Container container, String username, String password) {
        return newContainerTemplate(container, username, password, false).getJmxTemplate();
    }

    public static ContainerTemplate newContainerTemplate(Container container, String username, String password) {
        return newContainerTemplate(container, username, password, true);
    }

    public static ContainerTemplate newContainerTemplate(Container container, String username, String password, boolean caching) {
        return new ContainerTemplate(container, username, password, caching);
    }
}
