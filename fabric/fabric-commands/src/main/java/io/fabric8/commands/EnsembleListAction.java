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

import io.fabric8.api.ZooKeeperClusterService;

import java.io.PrintStream;
import java.util.List;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

@Command(name = "ensemble-list", scope = "fabric", description = "List the containers in the current fabric ensemble (ZooKeeper ensemble)", detailedDescription = "classpath:ensembleList.txt")
public class EnsembleListAction extends AbstractAction {

    private final ZooKeeperClusterService clusterService;

    EnsembleListAction(ZooKeeperClusterService clusterService) {
        this.clusterService = clusterService;
    }

    @Override
    protected Object doExecute() throws Exception {
        PrintStream out = System.out;
        List<String> containers = clusterService.getEnsembleContainers();
        if (containers != null) {
            out.println("[id]");
            for (String container : containers) {
                out.println(container);
            }
        }
        return null;
    }

}
