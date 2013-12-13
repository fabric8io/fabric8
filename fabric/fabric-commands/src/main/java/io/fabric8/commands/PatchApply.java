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
package io.fabric8.commands;

import java.net.URL;

import io.fabric8.api.Version;
import io.fabric8.boot.commands.support.FabricCommand;
import io.fabric8.utils.shell.ShellUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

@Command(name = "patch-apply", scope = "fabric", description = "Apply the given patch")
public class PatchApply extends FabricCommand {

    @Option(name="-u", aliases={"--username"}, description="Remote user name", required = false, multiValued = false)
    private String username;

    @Option(name="-p", aliases={"--password"}, description="Remote user password", required = false, multiValued = false)
    private String password;

    @Option(name = "--version", description = "Only apply upgrades for the given version")
    private String version;

    @Argument
    private URL patch;

    @Override
    protected Object doExecute() throws Exception {
        Version v = null;
        if (version != null && !version.isEmpty()) {
            v = fabricService.getVersion(version);
        }
        username = username != null && !username.isEmpty() ? username : ShellUtils.retrieveFabricUser(session);
        password = password != null ? password : ShellUtils.retrieveFabricUserPassword(session);
        fabricService.getPatchService().applyPatch(v, patch, username, password);
        return null;
    }

}
