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
package io.fabric8.kubernetes.provider.commands;

import io.fabric8.utils.Objects;
import io.fabric8.kubernetes.api.Kubernetes;
import io.fabric8.kubernetes.provider.KubernetesService;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

import java.util.List;

@Command(name = ReplicationControllerDelete.FUNCTION_VALUE, scope = "fabric",
        description = ReplicationControllerDelete.DESCRIPTION)
public class ReplicationControllerDeleteAction extends AbstractAction {
    @Argument(index = 0, name = "replicationControllers", description = "The replicationController IDs to delete", required = true, multiValued = true)
    List<String> replicationControllers = null;

    private final KubernetesService kubernetesService;

    public ReplicationControllerDeleteAction(KubernetesService kubernetesService) {
        this.kubernetesService = kubernetesService;
    }

    @Override
    protected Object doExecute() throws Exception {
        Kubernetes kubernetes = kubernetesService.getKubernetes();
        Objects.notNull(kubernetes, "kubernetes");
        Objects.notNull(replicationControllers, "replicationControllers");

        for (String replicationController : replicationControllers) {
            deleteReplicationController(kubernetes, replicationController);
        }
        System.out.println("Deleted replicationControllers: " + replicationControllers);
        return null;
    }

    protected void deleteReplicationController(Kubernetes kubernetes, String replicationController) throws Exception {
        kubernetes.deleteReplicationController(replicationController);
    }
}
