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
package io.fabric8.service;

import io.fabric8.api.RuntimeService;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.osgi.service.component.ComponentContext;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component(immediate = true)
@Service(RuntimeService.class)
public class RuntimeServiceImpl extends AbstractComponent implements RuntimeService {

    @Reference(referenceInterface = org.jboss.gravia.runtime.Runtime.class)
    private ValidatingReference<Runtime> runtime = new ValidatingReference<>();

    private String identity;
    private Path homePath;
    private Path dataPath;
    private Path confPath;

    @Activate
    void activate(ComponentContext componentContext) throws Exception {
        identity = IllegalStateAssertion.assertNotNull(getProperty(RUNTIME_IDENTITY), "Runtime ID cannot be null.");
        homePath = Paths.get(IllegalStateAssertion.assertNotNull(getProperty(RUNTIME_HOME_DIR), "Runtime home directory cannot be null."));
        dataPath = Paths.get(IllegalStateAssertion.assertNotNull(getProperty(RUNTIME_DATA_DIR), "Runtime data directory cannot be null."));
        confPath = Paths.get(IllegalStateAssertion.assertNotNull(getProperty(RUNTIME_CONF_DIR), "Runtime conf directory cannot be null."));
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

    void bindRuntime(Runtime service) {
        runtime.bind(service);
    }

    void unbindRuntime(Runtime service) {
        runtime.unbind(service);
    }
}
