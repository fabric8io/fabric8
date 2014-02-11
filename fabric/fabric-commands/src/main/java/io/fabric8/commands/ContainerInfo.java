/*
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

import io.fabric8.api.RuntimeProperties;
import io.fabric8.utils.Strings;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import io.fabric8.api.Container;
import io.fabric8.api.DataStore;
import io.fabric8.api.Profile;
import io.fabric8.boot.commands.support.FabricCommand;
import io.fabric8.utils.SystemProperties;
import static io.fabric8.utils.FabricValidations.validateContainersName;

@Command(name = "container-info", scope = "fabric", description = "Displays information about the containers")
public class ContainerInfo extends FabricCommand {

	static final String FORMAT = "%-30s %s";

    private RuntimeProperties runtimeProperties;

	@Argument(index = 0, name = "container", description = "The name of the container container.", required = false, multiValued = false)
	private String containerName;

	@Override
	protected Object doExecute() throws Exception {
		checkFabricAvailable();

        containerName = Strings.isNotBlank(containerName) ? containerName : runtimeProperties.getProperty(SystemProperties.KARAF_NAME);

        validateContainersName(containerName);
		if (!containerExists(containerName)) {
			System.out.println("Container " + containerName + " does not exists!");
			return null;
		}
		Container container = fabricService.getContainer(containerName);

		System.out.println(String.format(FORMAT, "Name:", container.getId()));
		System.out.println(String.format(FORMAT, "Version:", container.getVersion()));
		System.out.println(String.format(FORMAT, "Alive:", container.isAlive()));
		System.out.println(String.format(FORMAT, "Resolver:", container.getResolver()));
		System.out.println(String.format(FORMAT, "Network Address:", container.getIp()));
		System.out.println(String.format(FORMAT, "SSH Url:", container.getSshUrl()));
		System.out.println(String.format(FORMAT, "JMX Url:", container.getJmxUrl()));
        System.out.println(String.format(FORMAT, "Process ID:", container.getProcessId()));
		StringBuilder sb = new StringBuilder();
		Profile[] profiles = container.getProfiles();

		for (int i = 0; i < profiles.length; i++) {
			if (i != 0) {
				sb.append(" ");
			}
			sb.append(profiles[i].getId());
		}

		System.out.println(String.format(FORMAT, "Profiles:", sb.toString()));
		System.out.println(String.format(FORMAT, "Provision Status:", container.getProvisionStatus()));
        String blueprintStatus = fabricService.getDataStore().getContainerAttribute(containerName, DataStore.ContainerAttribute.BlueprintStatus, "", false, false);
        String springStatus = fabricService.getDataStore().getContainerAttribute(containerName, DataStore.ContainerAttribute.SpringStatus, "", false, false);
        if (!blueprintStatus.isEmpty()) {
            System.out.println(String.format(FORMAT, "Blueprint Status:", blueprintStatus.toLowerCase()));
        }
        if (!springStatus.isEmpty()) {
            System.out.println(String.format(FORMAT, "Spring Status:", springStatus.toLowerCase()));
        }

		if (container.getProvisionException() != null) {
			System.out.println(String.format(FORMAT, "Provision Error:", container.getProvisionException()));
		}

		return null;
	}

	/**
	 * Checks if container exists
	 *
	 * @param containerName
	 */
	private boolean containerExists(String containerName) {
		Container[] containers = fabricService.getContainers();
		for (Container c : containers) {
			if (containerName.equals(c.getId())) {
				return true;
			}
		}
		return false;
	}

    public RuntimeProperties getRuntimeProperties() {
        return runtimeProperties;
    }

    public void setRuntimeProperties(RuntimeProperties runtimeProperties) {
        this.runtimeProperties = runtimeProperties;
    }
}
