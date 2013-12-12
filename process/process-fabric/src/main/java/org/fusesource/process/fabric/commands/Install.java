/*
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
package org.fusesource.process.fabric.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import io.fabric8.utils.shell.ShellUtils;
import org.fusesource.process.fabric.ContainerInstallOptions;
import org.fusesource.process.manager.InstallTask;
import org.fusesource.process.manager.Installation;


/**
 * Installs a new process
 */
@Command(name = "process-install", scope = "fabric", description = "Installs a managed process into this container.")
public class Install extends ContainerInstallSupport {

    @Argument(index = 1, required = true, name = "name", description = "The name of the process to add")
    protected String name;

    @Argument(index = 2, required = true, name = "url", description = "The URL of the installation distribution to install. Typically this is a tarball or zip file")
    protected String url;

    @Argument(index = 4, required = false, name = "extractCmd", description = "The extract command and args to use on the downloaded artifact, defaults to 'tar zxf'")
    protected String[] extractCmd = { "tar", "zxf"};

    void doWithAuthentication(String jmxUser, String jmxPassword) throws Exception {
        ContainerInstallOptions options = ContainerInstallOptions.builder()
                .container(container)
                .user(jmxUser)
                .password(jmxPassword)
                .name(name)
                .url(url)
                .extractCmd(getExtract(extractCmd))
                .controllerUrl(getControllerURL())
                .build();

        // allow a post install step to be specified - e.g. specifying jars/wars?
        InstallTask postInstall = null;

        Installation install = getContainerProcessManager().install(options, postInstall);
        ShellUtils.storeFabricCredentials(session, jmxUser, jmxPassword);
        System.out.println("Installed process " + install.getId() + " to " + install.getInstallDir());
    }

    private String getExtract(String[] extract) {
        if (extract == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (String str : extract) {
            if (str != null && !str.trim().isEmpty()) {
                builder.append(str).append(' ');
            }
        }
        final String trimmedValue = builder.toString().trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

}
