/*
 * #%L
 * Wildfly Gravia Subsystem
 * %%
 * Copyright (C) 2010 - 2013 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package org.wildfly.extension.fabric.parser;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parse Fabric subsystem configuration
 *
 * @since 13-Nov-2013
 */
final class FabricSubsystemParser implements Namespace10, XMLStreamConstants, XMLElementReader<List<ModelNode>> {

    static XMLElementReader<List<ModelNode>> INSTANCE = new FabricSubsystemParser();

    // hide ctor
    private FabricSubsystemParser() {
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, FabricExtension.SUBSYSTEM_NAME);
        address.protect();

        ModelNode subsystemAdd = new ModelNode();
        subsystemAdd.get(OP).set(ADD);
        subsystemAdd.get(OP_ADDR).set(address);
        operations.add(subsystemAdd);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case VERSION_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        default:
                            throw unexpectedElement(reader);
                    }
                }
                default:
                    continue;
            }
        }
    }
}
