/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.runtime.container.wildfly;

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

import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractRuntimeProperties;
import io.fabric8.api.scr.ValidatingReference;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.runtime.Runtime;

@ThreadSafe
@Component(label = "Widlfly Runtime Properties Service", immediate = true, metatype = false)
@Service(RuntimeProperties.class)
public class WildflyPropertiesService extends AbstractRuntimeProperties {

    @Reference(referenceInterface = Runtime.class)
    private ValidatingReference<Runtime> runtime = new ValidatingReference<>();

    @Activate
    void activate() throws Exception {
        activateInternal();
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    protected String getPropertyInternal(String key, String defaultValue) {
        return (String) runtime.get().getProperty(key, defaultValue);
    }

    private void activateInternal() {
        Runtime runtime = this.runtime.get();
        if (runtime.getProperty(RUNTIME_IDENTITY) == null) {
            System.setProperty(RUNTIME_IDENTITY, "wildfly");
        }
        if (runtime.getProperty(RUNTIME_HOME_DIR) == null) {
            System.setProperty(RUNTIME_HOME_DIR, System.getProperty("jboss.home.dir"));
        }
        if (runtime.getProperty(RUNTIME_DATA_DIR) == null) {
            System.setProperty(RUNTIME_DATA_DIR, runtime.getProperty(RUNTIME_HOME_DIR) + "/standalone/data");
        }
        if (runtime.getProperty(RUNTIME_CONF_DIR) == null) {
            System.setProperty(RUNTIME_CONF_DIR, runtime.getProperty(RUNTIME_HOME_DIR) + "/standalone/configuration");
        }
    }

    void bindRuntime(Runtime service) {
        runtime.bind(service);
    }

    void unbindRuntime(Runtime service) {
        runtime.unbind(service);
    }
}
