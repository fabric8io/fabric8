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

import org.fusesource.fusemq.amqp.jaxb.schema.Encoding;

public class AmqpEncoding {

    private String name;
    private String category;
    private String code;
    private String width;
    private String label;

    public void parseFromEncoding(Encoding encoding) {
        name = encoding.getName();
        category = encoding.getCategory();
        code = encoding.getCode();
        width = encoding.getWidth();
        label = encoding.getLabel();
        //TODO: encoding.getDoc();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isFixed()
    {
        return "fixed".equals(getCategory());
    }

    public boolean isVariable()
    {
        return "variable".equals(getCategory());
    }

    public boolean isCompound()
    {
        return "compound".equals(getCategory());
    }

    public boolean isArray()
    {
        return "array".equals(getCategory());
    }

    public String toString() {
        return "\n{" + name + ", cat=" + category + ", code=" + code + ", width=" + width +"}";
    }
}
