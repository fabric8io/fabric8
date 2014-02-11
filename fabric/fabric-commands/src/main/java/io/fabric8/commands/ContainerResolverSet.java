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

package io.fabric8.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import io.fabric8.api.Container;
import io.fabric8.boot.commands.support.FabricCommand;

@Command(name = "container-resolver-set", scope = "fabric", description = "Apply the specified resolver policy to the specified container or containers", detailedDescription = "classpath:containerResolverSet.txt")
public class ContainerResolverSet extends FabricCommand {

    @Option(name = "--all", description = "Apply the resolver policy to all containers in the fabric.")
    private boolean all;

    @Option(name = "--container", required = false, multiValued = true, description = "Apply the resolver policy to the specified container.")
    private List<String> containerIds;

    @Argument(index = 0, required = true, multiValued = false, name = "resolver", description = "The resolver policy to set on the specified container(s). Possible values are: localip, localhostname, publicip, publichostname, manualip.")
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
                System.out.println("No container has been specified. Assuming the current container.");
                containerIds = Arrays.asList(fabricService.getCurrentContainer().getId());
            }
        } else {
            if (all) {
                throw new IllegalArgumentException("Can not use --all with a list of containers simultaneously.");
            }
        }

        for (String containerId : containerIds) {
            Container container = fabricService.getContainer(containerId);
            container.setResolver(resolver);
        }
        return null;
    }
}
