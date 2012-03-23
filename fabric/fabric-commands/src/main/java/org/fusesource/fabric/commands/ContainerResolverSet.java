/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.fusesource.fabric.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.commands.support.FabricCommand;
import org.fusesource.fabric.zookeeper.ZkPath;

@Command(name = "container-resolver-set", scope = "fabric", description = "Sets the resolver for the specified container")
public class ContainerResolverSet extends FabricCommand {

    @Option(name = "--all", description = "Upgrade all containers.")
    private boolean all;

    @Option(name = "--container", description = "Upgrade the given containers. Defaults to the current container.", required = false, multiValued = true)
    private List<String> containerIds;

    @Argument(index = 0, name = "resolver", description = "The resolver to set. A resolver defines the policy of how a container name is resovled to an ip.", required = true, multiValued = false)
    private String resolver;

    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();

        if (containerIds == null || containerIds.isEmpty()) {
            if (all) {
                containerIds = new ArrayList<String>();
                for (Container container : fabricService.getContainers()) {
                    containerIds.add(container.getId());
                }
            } else {
                containerIds = Arrays.asList(fabricService.getCurrentContainer().getId());
            }
        } else {
            if (all) {
                throw new IllegalArgumentException("Can not use --all with a list of containers simultaneously");
            }
        }

        for (String container:containerIds) {
            getZooKeeper().setData(ZkPath.CONTAINER_RESOLVER.getPath(container),resolver);
        }
        return null;
    }
}
