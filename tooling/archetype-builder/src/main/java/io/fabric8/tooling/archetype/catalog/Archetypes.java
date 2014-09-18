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
package io.fabric8.tooling.archetype.catalog;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "archetype-catalog")
@XmlAccessorType(XmlAccessType.FIELD)
public class Archetypes {

    @XmlElementWrapper(name = "archetypes")
    @XmlElement(name = "archetype")
    List<Archetype> archetypes = new ArrayList<Archetype>();

    public void add(Archetype a) {
        archetypes.add(a);
    }

    public static JAXBContext newJaxbContext() throws JAXBException {
        return JAXBContext.newInstance(Archetypes.class, Archetype.class);
    }

    public static Marshaller newMarshaller() throws JAXBException {
        Marshaller m = newJaxbContext().createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        return m;
    }

    public static Unmarshaller newUnmarshaller() throws JAXBException {
        return newJaxbContext().createUnmarshaller();
    }

    public List<Archetype> getArchetypes() {
        return archetypes;
    }

}
