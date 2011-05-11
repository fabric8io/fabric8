/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.generator;

import org.fusesource.fabric.apollo.amqp.jaxb.schema.Doc;
import org.fusesource.fabric.apollo.amqp.jaxb.schema.Field;

import java.io.BufferedWriter;
import java.io.IOException;

public class AmqpField {

    AmqpDoc doc = new AmqpDoc();
    String name;
    String defaultValue;
    String label;
    String type;
    boolean multiple;
    boolean required;

    public void parseFromField(Field field) {
        defaultValue = field.getDefault();
        label = field.getLabel();
        name = field.getName();
        multiple = new Boolean(field.getMultiple()).booleanValue();
        required = new Boolean(field.getMandatory()).booleanValue();
        type = field.getType();
        doc.setLabel(label);

        for (Object object : field.getDoc()) {
            if (object instanceof Doc) {
                doc.parseFromDoc((Doc) object);
            } else {
                // TODO handle error:
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AmqpDoc getDoc() {
        return doc;
    }

    public void setDoc(AmqpDoc doc) {
        this.doc = doc;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public AmqpClass resolveAmqpFieldType() throws UnknownTypeException {
        if (isMultiple()) {
            return TypeRegistry.resolveAmqpClass("multiple");
        }
        AmqpClass ampqClass = TypeRegistry.resolveAmqpClass(this);
        return ampqClass;
    }

    public void writeJavaDoc(BufferedWriter writer, int indent) throws IOException {
        doc.writeJavaDoc(writer, indent);
    }

    public String getJavaName() {
        return Utils.toJavaName(name);
    }

    public String toString() {
        return String.format("|%25s |%25s |%6s |%6s |%10s |", name, type, required, multiple, defaultValue);
    }
}
