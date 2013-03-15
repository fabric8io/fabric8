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

import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.CreateEnsembleOptions;
import org.fusesource.fabric.boot.commands.support.EnsembleCommandSupport;

import static org.fusesource.fabric.utils.FabricValidations.validateContainersName;

@Command(name = "ensemble-remove", scope = "fabric", description = "Re-create the current ensemble, excluding the specified containers from the ensemble", detailedDescription = "classpath:ensemble.txt")
public class EnsembleRemove extends EnsembleCommandSupport {

    @Option(name = "--generate-zookeeper-password", multiValued = false, description = "Flag to enable automatic generation of password")
    private boolean generateZookeeperPassword = false;

    @Option(name = "--new-zookeeper-password", multiValued = false, description = "The ensemble new password to use (defaults to the old one)")
    private String zookeeperPassword;

    @Option(name = "-f", aliases = "--force", multiValued = false, description = "Flag to force the addition without prompt")
    private boolean force = false;

    @Argument(required = true, multiValued = true, description = "List of containers to be removed. Must be an even number of containers.")
    private List<String> containers;

    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        validateContainersName(containers);
        if (checkIfShouldModify(session, force)) {
            StringBuilder builder = new StringBuilder();
            builder.append("Removing containers:");
            for (String container : containers) {
                builder.append(" ").append(container);
            }
            builder.append(" from the ensemble. This may take a while.");
            System.out.println(builder.toString());

            if (generateZookeeperPassword) {
                CreateEnsembleOptions options = CreateEnsembleOptions.build();
                service.removeFromCluster(containers, options);
            } else if (zookeeperPassword == null || zookeeperPassword.isEmpty()) {
                service.removeFromCluster(containers);
            } else {
                CreateEnsembleOptions options = CreateEnsembleOptions.build().zookeeperPassword(zookeeperPassword);
                service.removeFromCluster(containers, options);
            }
            System.out.println("Updated Zookeeper connection string: "+ service.getZooKeeperUrl());
        }
        return null;
    }

}
