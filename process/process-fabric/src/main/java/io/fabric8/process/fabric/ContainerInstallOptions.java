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
package io.fabric8.process.fabric;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import io.fabric8.process.manager.InstallOptions;

public class ContainerInstallOptions extends InstallOptions {

    public static class ContainerInstallOptionsBuilder extends InstallOptionsBuilder<ContainerInstallOptionsBuilder> {

        private String container;
        private String user;
        private String password;

        public ContainerInstallOptionsBuilder container(final String container) {
            this.container = container;
            return this;
        }

        public ContainerInstallOptionsBuilder user(final String user) {
            this.user = user;
            return this;
        }

        public ContainerInstallOptionsBuilder password(final String password) {
            this.password = password;
            return this;
        }

        public ContainerInstallOptions build() throws MalformedURLException {
                return new ContainerInstallOptions(container, user, password, getName(), getUrl(), getControllerUrl(), getControllerJson(), getExtractCmd(), isOffline(), getOptionalDependencyPatterns(), getExcludeDependencyFilterPatterns(), getMainClass(), getProperties(), getEnvironment());

        }
    }

    private final String container;
    private final String user;
    private final String password;

    public static ContainerInstallOptionsBuilder builder() {
        return new ContainerInstallOptionsBuilder();
    }

    public ContainerInstallOptions(String container, String user, String password, String name, URL url, URL controllerUrl, String controllerJson, String extractCmd, boolean offline, String[] optionalDependencyPatterns, String[] excludeDependencyFilterPatterns, String mainClass, Map<String, Object> properties, Map<String, String> environment) {
        super(name, url, controllerUrl, controllerJson, extractCmd, offline, optionalDependencyPatterns, excludeDependencyFilterPatterns, mainClass, properties, environment);
        this.container = container;
        this.user = user;
        this.password = password;
    }

    public String getContainer() {
        return container;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public InstallOptions asInstallOptions() {
        return new InstallOptions(getName(), getUrl(), getControllerUrl(), getControllerJson(), getExtractCmd(), isOffline(), getOptionalDependencyPatterns(), getExcludeDependencyFilterPatterns(), getMainClass(), getProperties(), getEnvironment());
    }
}
