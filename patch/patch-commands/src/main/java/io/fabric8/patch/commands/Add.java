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
package io.fabric8.patch.commands;

import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.boot.commands.support.AbstractCommandComponent;
import io.fabric8.patch.Service;
import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.service.command.Function;

@Component(immediate = true)
@org.apache.felix.scr.annotations.Service({ Function.class, AbstractCommand.class })
@org.apache.felix.scr.annotations.Properties({
        @Property(name = "osgi.command.scope", value = Add.SCOPE_VALUE),
        @Property(name = "osgi.command.function", value = Add.FUNCTION_VALUE)
})
public class Add extends AbstractCommandComponent {

    public static final String SCOPE_VALUE = "patch";
    public static final String FUNCTION_VALUE = "add";
    public static final String DESCRIPTION = "Download a patch";

    @Reference(referenceInterface = Service.class)
    private final ValidatingReference<Service> service = new ValidatingReference<Service>();

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
        return new AddAction(service.get());
    }

    void bindService(Service service) {
        this.service.bind(service);
    }

    void unbindService(Service service) {
        this.service.unbind(service);
    }

}
