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

import java.util.Arrays;
import java.util.List;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.commands.support.FabricCommand;

@Command(name = "version-set", scope = "fabric", description = "Set the version to a container")
public class VersionSet extends FabricCommand {

    @Argument(index = 0, name = "version", description = "The version to set to the container", required = true, multiValued = false)
    private String versionName;

    @Argument(index = 1, name = "container", description = "The list of containers. Empty list assumes current container", required = false, multiValued = true)
    private List<String> containerNames;

    @Override
    protected Object doExecute() throws Exception {
        if (containerNames == null || containerNames.isEmpty()) {
            containerNames = Arrays.asList(fabricService.getCurrentContainer().getId());
        }

        Version version = fabricService.getVersion(versionName);

        if (version == null) {
            throw new IllegalArgumentException("Cannot find version: " + versionName);
        }

        for(String containerName : containerNames) {
            Container container = fabricService.getContainer(containerName);
            if (container != null) {
                container.setVersion(version);
            }
        }
        return null;
    }
}
