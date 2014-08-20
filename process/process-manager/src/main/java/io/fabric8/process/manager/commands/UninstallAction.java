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

import io.fabric8.process.manager.Installation;
import io.fabric8.process.manager.ProcessManager;
import io.fabric8.process.manager.commands.support.ProcessControlCommandSupport;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

/**
 */
@Command(name = "unstall", scope = "process", description = "Uninstalls a managed process from this container.")
public class UninstallAction extends ProcessControlCommandSupport {

    @Option(name="-f", aliases={"--force"}, required = false, description = "Forces uninstalling even if the process is still alive")
    protected boolean force = false;

    protected UninstallAction(ProcessManager processManager) {
        super(processManager);
    }

    @Override
    protected void doControlCommand(Installation installation) throws Exception {
        if (!force && installation.getActivePid() != null) {
            System.out.println("The process is alive and cannot be uninstalled. Stop the process before uninstalling, or use the --force option.");
        } else {
            getProcessManager().uninstall(installation);
        }
    }
}
