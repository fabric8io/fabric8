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

import org.fusesource.fusemq.amqp.jaxb.schema.Descriptor;

import java.util.StringTokenizer;

public class AmqpDescriptor {

    String formatCode;
    String symbolicName;
    long category;
    long descriptorId;

    public void parseFromDescriptor(Descriptor descriptor) {

        formatCode = descriptor.getCode();
        symbolicName = descriptor.getName();
        StringTokenizer tok = new StringTokenizer(formatCode, ":");
        category = Long.parseLong(tok.nextToken().substring(2), 16);
        descriptorId = Long.parseLong(tok.nextToken().substring(2), 16);
        // TODO descriptor.getDoc();
    }

    public String getFormatCode() {
        return formatCode;
    }

    public void setFormatCode(String formatCode) {
        this.formatCode = formatCode;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public void setSymbolicName(String symbolicName) {
        this.symbolicName = symbolicName;
    }

    public long getCategory() {
        return category;
    }

    public void setCategory(long category) {
        this.category = category;
    }

    public long getDescriptorId() {
        return descriptorId;
    }

    public void setDescriptorId(long descriptorId) {
        this.descriptorId = descriptorId;
    }

    public AmqpClass resolveDescribedType() throws UnknownTypeException {
        return TypeRegistry.resolveAmqpClass(getDescribedType());
    }

    public String getDescribedType() {
        return symbolicName.substring(symbolicName.lastIndexOf(":") + 1);
    }

    public String toString() {
        return String.format("     Format Code:%s\n     Category:%s\n     Descriptor ID:%s\n     Symbolic Name:%s", formatCode, category, descriptorId, symbolicName);
    }

}
