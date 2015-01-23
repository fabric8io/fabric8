/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.support.commands;

import io.fabric8.api.FabricService;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.boot.commands.support.AbstractCommandComponent;
import io.fabric8.support.api.SupportService;
import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.apache.felix.scr.annotations.*;
import org.apache.felix.service.command.Function;

@Component(immediate = true)
@org.apache.felix.scr.annotations.Service({ Function.class, AbstractCommand.class })
@org.apache.felix.scr.annotations.Properties({
        @Property(name = "osgi.command.scope", value = Collect.SCOPE_VALUE),
        @Property(name = "osgi.command.function", value = Collect.FUNCTION_VALUE)
})
public class Collect extends AbstractCommandComponent {

    public static final String SCOPE_VALUE = "support";
    public static final String FUNCTION_VALUE = "collect";
    public static final String DESCRIPTION = "Collect information for support";

    @Reference(referenceInterface = SupportService.class)
    private final ValidatingReference<SupportService> service = new ValidatingReference<SupportService>();

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
        return new CollectAction(service.get());
    }

    void bindService(SupportService service) {
        this.service.bind(service);
    }

    void unbindService(SupportService service) {
        this.service.unbind(service);
    }


}
