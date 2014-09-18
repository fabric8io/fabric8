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

import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.boot.commands.support.AbstractCommandComponent;
import io.fabric8.kubernetes.provider.KubernetesService;
import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.service.command.Function;

@Component(immediate = true)
@Service({Function.class, AbstractCommand.class})
@org.apache.felix.scr.annotations.Properties({
        @Property(name = "osgi.command.scope", value = ReplicationControllerDelete.SCOPE_VALUE),
        @Property(name = "osgi.command.function", value = ReplicationControllerDelete.FUNCTION_VALUE)
})
public class ReplicationControllerDelete extends AbstractCommandComponent {

    public static final String SCOPE_VALUE = "fabric";
    public static final String FUNCTION_VALUE = "replicationController-delete";
    public static final String DESCRIPTION = "Delete the given replicationController(s) in kubernetes";

    @Reference(referenceInterface = KubernetesService.class, bind = "bindKubernetesService", unbind = "unbindKubernetesService")
    private final ValidatingReference<KubernetesService> kubernetesService = new ValidatingReference<KubernetesService>();

    // Completers
    @Reference(referenceInterface = ReplicationControllerCompleter.class, bind = "bindReplicationControllerCompleter", unbind = "unbindReplicationControllerCompleter")
    private ReplicationControllerCompleter replicationControllerCompleter; // dummy field

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public Action createNewAction() {
        assertValid();
        return new ReplicationControllerDeleteAction(kubernetesService.get());
    }

    void bindKubernetesService(KubernetesService kubernetesService) {
        this.kubernetesService.bind(kubernetesService);
    }

    void unbindKubernetesService(KubernetesService kubernetesService) {
        this.kubernetesService.unbind(kubernetesService);
    }

    void bindReplicationControllerCompleter(ReplicationControllerCompleter completer) {
        bindCompleter(completer);
    }

    void unbindReplicationControllerCompleter(ReplicationControllerCompleter completer) {
        unbindCompleter(completer);
    }
}
