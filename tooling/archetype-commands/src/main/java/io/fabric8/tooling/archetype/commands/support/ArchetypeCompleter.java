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
package io.fabric8.tooling.archetype.commands.support;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.JAXBException;

import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.tooling.archetype.ArchetypeService;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.osgi.service.component.ComponentContext;

/**
 * Completes common main class names
 */
@Component(immediate = true)
@Service({ArchetypeCompleter.class, Completer.class})
public class ArchetypeCompleter extends AbstractComponent implements Completer {

    @Reference(referenceInterface = ArchetypeService.class, bind = "bindArchetypeService", unbind = "unbindArchetypeService")
    private ArchetypeService archetypeService;

    private List<String> archetypes = new LinkedList<String>();

    @Override
    public int complete(final String buffer, final int cursor, final List candidates) {
        StringsCompleter delegate = new StringsCompleter(archetypes);
        return delegate.complete(buffer, cursor, candidates);
    }

    @Activate
    void activate(ComponentContext componentContext) throws IOException, JAXBException {
        activateComponent();
        for (String[] gav : this.archetypeService.listArchetypeGAVs()) {
            this.archetypes.add(String.format("%s:%s:%s", gav[0], gav[1], gav[2]));
        }
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        this.archetypes = new LinkedList<String>();
    }

    public void bindArchetypeService(ArchetypeService archetypeService) {
        this.archetypeService = archetypeService;
    }

    public void unbindArchetypeService(ArchetypeService archetypeService) {
        this.archetypeService = null;
    }

}
