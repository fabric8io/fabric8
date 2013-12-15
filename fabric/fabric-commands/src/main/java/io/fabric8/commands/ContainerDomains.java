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

import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import io.fabric8.api.Container;
import io.fabric8.boot.commands.support.FabricCommand;
import static io.fabric8.utils.FabricValidations.validateContainersName;

@Command(name = "container-jmx-domains", scope = "fabric", description = "Lists a container's JMX domains.")
public class ContainerDomains extends FabricCommand {

    @Argument(index = 0, name = "container", description = "The container name", required = true, multiValued = false)
    private String container = null;

    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        validateContainersName(container);
        Container found = getContainer(container);

        List<String> domains = found.getJmxDomains();
        for (String domain : domains) {
            System.out.println(domain);
        }
        return null;
    }

}
