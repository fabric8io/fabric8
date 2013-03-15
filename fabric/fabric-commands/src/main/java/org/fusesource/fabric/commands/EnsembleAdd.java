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

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.CreateEnsembleOptions;
import org.fusesource.fabric.boot.commands.support.EnsembleCommandSupport;

import java.util.List;
import static org.fusesource.fabric.utils.FabricValidations.validateContainersName;

@Command(name = "ensemble-add", scope = "fabric", description = "Extend the current fabric ensemble by converting the specified containers into ensemble servers", detailedDescription = "classpath:ensembleAdd.txt")
public class EnsembleAdd extends EnsembleCommandSupport {

	@Option(name = "--generate-zookeeper-password", multiValued = false, description = "Flag to enable automatic generation of password")
	private boolean generateZookeeperPassword = false;

	@Option(name = "--new-zookeeper-password", multiValued = false, description = "The ensemble new password to use (defaults to the old one)")
	private String zookeeperPassword;

    @Argument(required = true, multiValued = true, description = "List of containers to be added")
    private List<String> containers;

    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        validateContainersName(containers);

        if (containers != null && !containers.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            builder.append("Adding containers:");
            for (String container : containers) {
                builder.append(" ").append(container);
            }
            builder.append(" to the ensemble. This may take a while.");
            System.out.println(builder.toString());
			if (generateZookeeperPassword) {
				CreateEnsembleOptions options = CreateEnsembleOptions.build();
				service.addToCluster(containers, options);
			} else if (zookeeperPassword == null || zookeeperPassword.isEmpty()) {
				service.addToCluster(containers);
			} else {
				CreateEnsembleOptions options = CreateEnsembleOptions.build().zookeeperPassword(zookeeperPassword);
				service.addToCluster(containers, options);
			}
		}
        return null;
    }

}
