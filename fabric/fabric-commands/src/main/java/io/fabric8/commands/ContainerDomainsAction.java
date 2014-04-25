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

import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.boot.commands.support.FabricCommand;
import static io.fabric8.utils.FabricValidations.validateContainerName;

@Command(name = ContainerDomains.FUNCTION_VALUE, scope = ContainerDomains.SCOPE_VALUE, description = ContainerDomains.DESCRIPTION)
public class ContainerDomainsAction extends AbstractAction {

    @Argument(index = 0, name = "container", description = "The container name", required = true, multiValued = false)
    private String container = null;

    private final FabricService fabricService;

    ContainerDomainsAction(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    protected Object doExecute() throws Exception {
        validateContainerName(container);
        Container found = FabricCommand.getContainer(fabricService, container);
        List<String> domains = found.getJmxDomains();
        for (String domain : domains) {
            System.out.println(domain);
        }
        return null;
    }

}
