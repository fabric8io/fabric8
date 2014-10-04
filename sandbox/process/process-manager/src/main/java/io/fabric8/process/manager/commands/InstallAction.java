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
package io.fabric8.process.manager.commands;

import io.fabric8.process.manager.InstallOptions;
import io.fabric8.process.manager.InstallTask;
import io.fabric8.process.manager.Installation;
import io.fabric8.process.manager.ProcessManager;
import io.fabric8.process.manager.commands.support.InstallActionSupport;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.osgi.framework.BundleContext;

/**
 * Installs a new process
 */
@Command(name = "install", scope = "process", description = "Installs a managed process into this container.")
public class InstallAction extends InstallActionSupport {

    @Argument(index = 0, required = true, name = "name", description = "The name of the process to add")
    protected String name;

    @Argument(index = 1, required = true, name = "url", description = "The URL of the installation distribution to install. Typically this is a tarball or zip file")
    protected String url;

    InstallAction(ProcessManager processManager, BundleContext bundleContext) {
        super(processManager, bundleContext);
    }

    @Override
    protected Object doExecute() throws Exception {
        InstallOptions.InstallOptionsBuilder builder = InstallOptions.builder().name(name).url(url).controllerUrl(getControllerURL());
        InstallOptions options = build(builder);

        // allow a post install step to be specified - e.g. specifying jars/wars?
        InstallTask postInstall = null;
        Installation install = getProcessManager().install(options, postInstall);

        System.out.println("Installed process " + install.getId() + " to " + install.getInstallDir());
        return null;
    }

}
