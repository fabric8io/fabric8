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
package org.fusesource.fabric.commands;

import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.commands.support.FabricCommand;

@Command(name = "container-connect", scope = "fabric", description = "Connect to a remote container")
public class ContainerConnect extends FabricCommand {

    @Option(name="-u", aliases={"--username"}, description="Remote user name (Default: admin)", required = false, multiValued = false)
    private String username = "admin";

    @Option(name="-p", aliases={"--password"}, description="Remote user password (Default: admin)", required = false, multiValued = false)
    private String password = "admin";

    @Argument(index = 0, name="container", description="The container name", required = true, multiValued = false)
    private String container = null;

    @Argument(index = 1, name = "command", description = "Optional command to execute", required = false, multiValued = true)
    private List<String> command;

    protected Object doExecute() throws Exception {
        checkFabricAvailable();

        String cmdStr = "";
        if (command != null) {
            StringBuilder sb = new StringBuilder();
            for (String cmd : command) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(cmd);
            }
            cmdStr = "'" + sb.toString().replaceAll("'", "\\'") + "'";
        }

        Container found = getContainer(container);
        String sshUrl = found.getSshUrl();
        if (sshUrl == null) {
            throw new IllegalArgumentException("Container " + container + " has no SSH URL.");
        }
        String[] ssh = sshUrl.split(":");
        if (ssh.length < 2) {
            throw new IllegalArgumentException("Container " + container + " has an invalid SSH URL '" + sshUrl + "'");
        }
        session.execute("ssh -l " + username + " -P " + password + " -p " + ssh[1] + " " + ssh[0] + " " + cmdStr);
        return null;
    }

}
