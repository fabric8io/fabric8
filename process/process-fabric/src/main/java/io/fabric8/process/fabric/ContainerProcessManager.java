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

import com.google.common.collect.ImmutableMap;
import io.fabric8.api.FabricService;
import io.fabric8.process.manager.InstallTask;
import io.fabric8.process.manager.Installation;

import java.util.List;

public interface ContainerProcessManager {

    FabricService getFabricService();

    /**
     * Returns the current installed processes which may or may not be running right now
     */
    List<Installation> listInstallations(ContainerInstallOptions options);

    /**
     * Installs a process with an optional URL of the controller JSON
     * together with an optional install step
     */
    Installation install(ContainerInstallOptions options, InstallTask postInstall) throws Exception;

    /**
     * Installs an executable jar as a new managed process
     */
    Installation installJar(ContainerInstallOptions options, InstallTask postInstall) throws Exception;

    ImmutableMap<String, Installation> listInstallationMap(ContainerInstallOptions options);
}
