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
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.runtime.Runtime;
import org.osgi.service.component.ComponentContext;

import java.nio.file.Path;
import java.nio.file.Paths;

@ThreadSafe
@Component(label = "Embedded Runtime Properties Service", immediate = true, metatype = false)
@Service(RuntimeProperties.class)
public class EmbeddedPropertiesService extends AbstractComponent implements RuntimeProperties {

    @Reference(referenceInterface = Runtime.class)
    private ValidatingReference<Runtime> runtime = new ValidatingReference<>();

    private String identity;
    private Path homePath;
    private Path dataPath;
    private Path confPath;

    @Activate
    void activate(ComponentContext componentContext) throws Exception {
        identity = getRequiredProperty(RUNTIME_IDENTITY);
        homePath = Paths.get(getRequiredProperty(RUNTIME_HOME_DIR));
        dataPath = Paths.get(getRequiredProperty(RUNTIME_DATA_DIR));
        confPath = Paths.get(getRequiredProperty(RUNTIME_CONF_DIR));
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public String getRuntimeIdentity() {
        return identity;
    }

    @Override
    public Path getHomePath() {
        return homePath;
    }

    @Override
    public Path getConfPath() {
        return confPath;
    }

    @Override
    public Path getDataPath() {
        return dataPath;
    }

    @Override
    public String getProperty(String key) {
        return getPropertyInternal(key, null);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return getPropertyInternal(key, defaultValue);
    }

    private String getPropertyInternal(String key, String defaultValue) {
        return (String) runtime.get().getProperty(key, defaultValue);
    }

    private String getRequiredProperty(String propName) {
        String result = getPropertyInternal(propName, null);
        if (result != null) {
            return result;
        }
        throw new IllegalStateException("Cannot obtain required property: " + propName);
    }

    void bindRuntime(Runtime service) {
        runtime.bind(service);
    }

    void unbindRuntime(Runtime service) {
        runtime.unbind(service);
    }
}
