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
package io.fabric8.runtime.embedded.registration;

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
@Component(label = "Embedded Runtime Properties Service", immediate = true, metatype = false)
@Service(RuntimeProperties.class)
public class EmbeddedPropertiesService extends AbstractRuntimeProperties {

    @Reference(referenceInterface = Runtime.class)
    private ValidatingReference<Runtime> runtime = new ValidatingReference<>();

    @Activate
    void activate() throws Exception {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    protected String getPropertyInternal(String key, String defaultValue) {
        return (String) runtime.get().getProperty(key, defaultValue);
    }

    void bindRuntime(Runtime service) {
        runtime.bind(service);
    }

    void unbindRuntime(Runtime service) {
        runtime.unbind(service);
    }
}
