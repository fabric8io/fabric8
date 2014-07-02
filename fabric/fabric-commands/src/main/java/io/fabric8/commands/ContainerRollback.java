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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.fabric8.api.FabricService;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.boot.commands.support.AbstractCommandComponent;
import io.fabric8.boot.commands.support.ContainerCompleter;
import io.fabric8.boot.commands.support.VersionCompleter;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.service.command.Function;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.NullCompleter;

@Component(immediate = true)
@Service({ Function.class, AbstractCommand.class })
@org.apache.felix.scr.annotations.Properties({
        @Property(name = "osgi.command.scope", value = ContainerRollback.SCOPE_VALUE),
        @Property(name = "osgi.command.function", value = ContainerRollback.FUNCTION_VALUE)
})
public class ContainerRollback extends AbstractCommandComponent {

    public static final String SCOPE_VALUE = "fabric";
    public static final String FUNCTION_VALUE =  "container-rollback";
    public static final String DESCRIPTION = "Roll back the specified containers to an older version";

    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    // Completers
    @Reference(referenceInterface = ContainerCompleter.class, bind = "bindContainerCompleter", unbind = "unbindContainerCompleter")
    private ContainerCompleter containerCompleter; // dummy field
    @Reference(referenceInterface = VersionCompleter.class, bind = "bindVersionCompleter", unbind = "unbindVersionCompleter", cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC)
    private VersionCompleter versionCompleter; // dummy field

    private final List<Completer> completers = new ArrayList<Completer>();

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
        return new ContainerRollbackAction(fabricService.get());
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }

    void bindContainerCompleter(ContainerCompleter completer) {
        synchronized (completers) {
            // container last (2nd)
            completers.add(completer);
        }
    }

    void unbindContainerCompleter(ContainerCompleter completer) {
        completers.remove(completer);
    }

    void bindVersionCompleter(VersionCompleter completer) {
        synchronized (completers) {
            // version first
            completers.add(0, completer);
        }
    }

    void unbindVersionCompleter(VersionCompleter completer) {
        completers.remove(completer);
    }

    @Override
    public List<Completer> getCompleters() {
        synchronized (completers) {
            if (completers.isEmpty()) {
                return Arrays.<Completer>asList(new NullCompleter());
            }
            return Collections.unmodifiableList(completers);
        }
    }

}
