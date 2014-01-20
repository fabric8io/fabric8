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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.fabric8.api.Version;
import io.fabric8.boot.commands.support.FabricCommand;
import io.fabric8.utils.shell.ShellUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

@Command(name = "patch-apply", scope = "fabric", description = "Apply the given patch to the default version")
public class PatchApply extends FabricCommand {

    @Option(name="-u", aliases={"--username"}, description="Remote user name", required = false, multiValued = false)
    private String username;

    @Option(name="-p", aliases={"--password"}, description="Remote user password", required = false, multiValued = false)
    private String password;

    @Option(name = "--version", description = "Only apply upgrades for the given version instead of the default one")
    private String version;

    @Option(name = "--all-versions", description = "Apply patch to all versions instead of the default one")
    private boolean allVersions;

    @Argument
    private URL patch;

    @Override
    protected Object doExecute() throws Exception {
        List<Version> versions;
        if (version != null && !version.isEmpty()) {
            versions = Collections.singletonList(fabricService.getVersion(version));
        } else if (allVersions) {
            versions = Arrays.asList(fabricService.getVersions());
        } else {
            versions = Collections.singletonList(fabricService.getDefaultVersion());
        }
        username = username != null && !username.isEmpty() ? username : ShellUtils.retrieveFabricUser(session);
        password = password != null ? password : ShellUtils.retrieveFabricUserPassword(session);
        for (Version version : versions) {
            fabricService.getPatchService().applyPatch(version, patch, username, password);
        }
        return null;
    }

}
