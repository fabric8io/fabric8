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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBException;
import javax.xml.transform.stream.StreamSource;

import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.boot.commands.support.VersionCompleter;
import io.fabric8.tooling.archetype.generator.Archetype;
import io.fabric8.tooling.archetype.generator.Archetypes;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
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

    private List<String> archetypes = new ArrayList<String>();

    private ComponentContext componentContext;

    @Override
    public int complete(final String buffer, final int cursor, final List candidates) {
        StringsCompleter delegate = new StringsCompleter();
        return delegate.complete(buffer, cursor, candidates);
    }

    @Activate
    void activate(ComponentContext componentContext) throws IOException, JAXBException {
        activateComponent();
        this.componentContext = componentContext;
        URL catalog = this.componentContext.getBundleContext().getBundle().getResource("fabric8-archetype-catalog.xml");
        Archetypes archetypes = (Archetypes) Archetypes.newUnmarshaller().unmarshal(new StreamSource(catalog.openStream()));
        for (Archetype arch : archetypes.getArchetypes()) {
            this.archetypes.add(String.format("%s:%s:%s", arch.groupId, arch.artifactId, arch.version));
        }
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    public List<String> getArchetypes() {
        return archetypes;
    }

}
