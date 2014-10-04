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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import io.fabric8.api.Container;
import io.fabric8.process.manager.DownloadStrategy;
import io.fabric8.process.manager.InstallOptions;

public class ContainerInstallOptions extends InstallOptions {

    public static class ContainerInstallOptionsBuilder extends InstallOptionsBuilder<ContainerInstallOptionsBuilder> {
        private String user;
        private String password;

        public ContainerInstallOptionsBuilder user(final String user) {
            this.user = user;
            return this;
        }

        public ContainerInstallOptionsBuilder password(final String password) {
            this.password = password;
            return this;
        }

        public ContainerInstallOptions build() throws MalformedURLException {
                return new ContainerInstallOptions(getId(), user, password, getName(), getUrl(), getControllerUrl(), getControllerJson(), getExtractCmd(), getPostUnpackCmds(), getPostInstallCmds(), isOffline(), getOptionalDependencyPatterns(), getExcludeDependencyFilterPatterns(), getMainClass(), getProperties(), getEnvironment(), getJvmOptions(), getJarFiles(), getContainer(), getDownloadStrategy());

        }
    }

    private final String user;
    private final String password;

    public static ContainerInstallOptionsBuilder builder() {
        return new ContainerInstallOptionsBuilder();
    }

    public ContainerInstallOptions(String id, String user, String password, String name, URL url, URL controllerUrl, String controllerJson, String extractCmd, String[] postUnpackCmds, String[] postInstallCmds, boolean offline, String[] optionalDependencyPatterns, String[] excludeDependencyFilterPatterns, String mainClass, Map<String, Object> properties, Map<String, String> environment, String[] jvmOptions, Map<String, File> jarFiles, Container container, DownloadStrategy downloadStrategy) {
        super(id, name, url, controllerUrl, controllerJson, extractCmd, postUnpackCmds, postInstallCmds, offline, optionalDependencyPatterns, excludeDependencyFilterPatterns, mainClass, properties, environment, jvmOptions, jarFiles, container, downloadStrategy);
        this.user = user;
        this.password = password;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public InstallOptions asInstallOptions() {
        return new InstallOptions(getId(), getName(), getUrl(), getControllerUrl(), getControllerJson(), getExtractCmd(), getPostUnpackCmds(), getPostInstallCmds(), isOffline(), getOptionalDependencyPatterns(), getExcludeDependencyFilterPatterns(), getMainClass(), getProperties(), getEnvironment(), getJvmOptions(), getJarFiles(), getContainer(), getDownloadStrategy());
    }
}
