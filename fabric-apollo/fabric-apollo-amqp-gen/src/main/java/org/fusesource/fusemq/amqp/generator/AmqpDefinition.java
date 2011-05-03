/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fusemq.amqp.generator;

import java.io.BufferedWriter;
import java.io.IOException;

import org.fusesource.fusemq.amqp.jaxb.schema.Definition;

public class AmqpDefinition {

    Definition definition;
    AmqpDoc doc;

    AmqpDefinition(Definition definition) {
        parseFromDefinition(definition);
    }

    public void parseFromDefinition(Definition definition) {
        this.definition = definition;
        if (definition.getDoc() != null || definition.getDoc() != null) {
            doc = new AmqpDoc(definition.getDoc());
            doc.setLabel(definition.getLabel());
        }
    }

    public String getLabel() {
        return definition.getLabel();
    }

    public void writeJavaDoc(BufferedWriter writer, int indent) throws IOException {
        doc.writeJavaDoc(writer, indent);
    }

    public String getValue() {
        return definition.getValue();
    }

    public String getName() {
        return definition.getName();
    }

}
