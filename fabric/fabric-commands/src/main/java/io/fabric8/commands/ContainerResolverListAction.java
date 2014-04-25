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
package io.fabric8.commands;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = ContainerResolverList.FUNCTION_VALUE, scope = ContainerResolverList.SCOPE_VALUE, description = ContainerResolverList.DESCRIPTION, detailedDescription = "classpath:containerResolverList.txt")
public class ContainerResolverListAction extends AbstractAction {

    static final String FORMAT = "%-16s %-16s %-16s %-16s %-32s %-16s %-32s";
    static final String[] HEADERS = { "[id]", "[resolver]", "[local hostname]", "[local ip]", "[public hostname]", "[public ip]", "[manual ip]" };

    @Argument(index = 0, name = "container", description = "The list of containers to display. Empty list assumes current container only.", required = false, multiValued = true)
    private List<String> containerIds;

    private final FabricService fabricService;

    ContainerResolverListAction(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    @Override
    protected Object doExecute() throws Exception {

        if (containerIds == null || containerIds.isEmpty()) {
            containerIds = new ArrayList<String>();
            for (Container container : fabricService.getContainers()) {
                containerIds.add(container.getId());
            }
        }
        System.out.println(String.format(FORMAT, (Object[]) HEADERS));
        for (String containerId : containerIds) {
            Container container = fabricService.getContainer(containerId);
            String localHostName = container.getLocalHostname();
            String localIp = container.getLocalIp();
            String publicHostName = container.getPublicHostname();
            String publicIp = container.getPublicIp();
            String manualIp = container.getManualIp();

            localHostName = localHostName != null ? localHostName : "";
            localIp = localIp != null ? localIp : "";
            publicHostName = publicHostName != null ? publicHostName : "";
            publicIp = publicIp != null ? publicIp : "";
            manualIp = manualIp != null ? manualIp : "";

            String resolver = container.getResolver();
            System.out.println(String.format(FORMAT, containerId, resolver, localHostName, localIp, publicHostName, publicIp, manualIp));
        }
        return null;
    }
}
