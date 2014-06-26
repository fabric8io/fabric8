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
import io.fabric8.api.DataStore;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.common.util.Strings;
import io.fabric8.utils.SystemProperties;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

import static io.fabric8.common.util.Strings.emptyIfNull;
import static io.fabric8.utils.FabricValidations.validateContainerName;

@Command(name = ContainerInfo.FUNCTION_VALUE, scope = ContainerInfo.SCOPE_VALUE, description = ContainerInfo.DESCRIPTION)
public class ContainerInfoAction extends AbstractAction {

	static final String FORMAT = "%-30s %s";

	@Argument(index = 0, name = "container", description = "The name of the container container.", required = false, multiValued = false)
	private String containerName;

    private final FabricService fabricService;
    private final RuntimeProperties runtimeProperties;

    ContainerInfoAction(FabricService fabricService, RuntimeProperties runtimeProperties) {
        this.fabricService = fabricService;
        this.runtimeProperties = runtimeProperties;
    }

	@Override
	protected Object doExecute() throws Exception {

        containerName = Strings.isNotBlank(containerName) ? containerName : runtimeProperties.getRuntimeIdentity();

        validateContainerName(containerName);
		if (!containerExists(containerName)) {
			System.out.println("Container " + containerName + " does not exists!");
			return null;
		}
		Container container = fabricService.getContainer(containerName);

		System.out.println(String.format(FORMAT, "Name:", container.getId()));
		System.out.println(String.format(FORMAT, "Version:", container.getVersion()));
		System.out.println(String.format(FORMAT, "Connected:", container.isAlive()));
        System.out.println(String.format(FORMAT, "Type:", emptyIfNull(container.getType())));
        Long processId = container.getProcessId();
        System.out.println(String.format(FORMAT, "Process ID:", ((processId != null) ? processId.toString() : "")));
        if (Strings.isNotBlank(container.getLocation())) {
            System.out.println(String.format(FORMAT, "Location:", emptyIfNull(container.getLocation())));
        }
		System.out.println(String.format(FORMAT, "Resolver:", emptyIfNull(container.getResolver())));
		System.out.println(String.format(FORMAT, "Network Address:", emptyIfNull(container.getIp())));
		System.out.println(String.format(FORMAT, "Local Network Address:", emptyIfNull(container.getLocalIp())));
		System.out.println(String.format(FORMAT, "Public Network Address:", emptyIfNull(container.getPublicIp())));
        System.out.println(String.format(FORMAT, "Local Hostname:", emptyIfNull(container.getLocalHostname())));
        System.out.println(String.format(FORMAT, "Public Hostname:", emptyIfNull(container.getPublicHostname())));
		System.out.println(String.format(FORMAT, "SSH Url:", emptyIfNull(container.getSshUrl())));
		System.out.println(String.format(FORMAT, "JMX Url:", emptyIfNull(container.getJmxUrl())));
        System.out.println(String.format(FORMAT, "Http Url:", emptyIfNull(container.getHttpUrl())));
		System.out.println(String.format(FORMAT, "Jolokia Url:", emptyIfNull(container.getJolokiaUrl())));
        String debugPort = container.getDebugPort();
        if (Strings.isNotBlank(debugPort)) {
            System.out.println(String.format(FORMAT, "Debug Port:", debugPort));
        }
        // we want each profile on a separate line
        Profile[] profiles = container.getProfiles();
        for (int i = 0; i < profiles.length; i++) {
            String id = profiles[i].getId();
            if (i == 0) {
                System.out.println(String.format(FORMAT, "Profiles:", id));
            } else {
                System.out.println(String.format(FORMAT, "", id));
            }
        }
        String blueprintStatus = fabricService.getDataStore().getContainerAttribute(containerName, DataStore.ContainerAttribute.BlueprintStatus, "", false, false);
        String springStatus = fabricService.getDataStore().getContainerAttribute(containerName, DataStore.ContainerAttribute.SpringStatus, "", false, false);
        if (!blueprintStatus.isEmpty()) {
            System.out.println(String.format(FORMAT, "Blueprint Status:", blueprintStatus.toLowerCase()));
        }
        if (!springStatus.isEmpty()) {
            System.out.println(String.format(FORMAT, "Spring Status:", springStatus.toLowerCase()));
        }

		System.out.println(String.format(FORMAT, "Provision Status:", container.getProvisionStatus()));
		if (container.getProvisionException() != null) {
			System.out.println(String.format(FORMAT, "Provision Error:", container.getProvisionException()));
		}

		return null;
	}

	private boolean containerExists(String containerName) {
		Container[] containers = fabricService.getContainers();
		for (Container c : containers) {
			if (containerName.equals(c.getId())) {
				return true;
			}
		}
		return false;
	}
}
