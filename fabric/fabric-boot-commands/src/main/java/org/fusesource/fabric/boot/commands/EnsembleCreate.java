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
package org.fusesource.fabric.boot.commands;

import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.ZooKeeperClusterService;
import org.fusesource.fabric.boot.commands.support.EnsembleCommandSupport;

@Command(name = "ensemble-create", scope = "fabric", description = "Create a new ZooKeeper ensemble", detailedDescription = "classpath:ensembleCreate.txt")
public class EnsembleCreate extends EnsembleCommandSupport {

    @Option(name = "--clean", description = "Clean local zookeeper cluster and configurations")
    private boolean clean;
    @Option(name = "-n", aliases = "--non-managed", multiValued = false, description = "Flag to keep the container non managed")
    private boolean nonManaged;
    @Argument(required = true, multiValued = true, description = "List of containers")
    private List<String> containers;

    @Override
    protected Object doExecute() throws Exception {
        if (clean) {
            service.clean();
        }
        if (nonManaged) {
            System.setProperty(ZooKeeperClusterService.AGENT_AUTOSTART, "false");
        } else {
            System.setProperty(ZooKeeperClusterService.AGENT_AUTOSTART, "true");
        }
        service.createCluster(containers);
        return null;
    }
}
