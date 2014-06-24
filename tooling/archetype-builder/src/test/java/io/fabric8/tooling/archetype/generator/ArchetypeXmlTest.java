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
package io.fabric8.tooling.archetype.generator;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBException;

import io.fabric8.tooling.archetype.catalog.Archetype;
import io.fabric8.tooling.archetype.catalog.Archetypes;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ArchetypeXmlTest {

    @Test
    public void testMarshalArchetypes() throws JAXBException {
        Archetypes archetypes = new Archetypes();
        archetypes.add(new Archetype("foo", "bar", "1.1", "Some description"));
        archetypes.add(new Archetype("xyz", "whatever", "2.3", "Docs..."));

        StringWriter buffer = new StringWriter();
        Archetypes.newMarshaller().marshal(archetypes, buffer);

        System.out.println("Generated XML: " + buffer);

        Object result = Archetypes.newUnmarshaller().unmarshal(new StringReader(buffer.toString()));

        if (result instanceof Archetypes) {
            assertEquals(2, ((Archetypes) result).getArchetypes().size());
            for (Archetype a : ((Archetypes) result).getArchetypes()) {
                System.out.println(a);
            }
        } else {
            fail("Found " + result.getClass().getName() + " when expected an " + Archetypes.class.getName());
        }
    }

}
