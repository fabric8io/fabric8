/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel.c24io;

import biz.c24.io.api.data.Element;
import org.apache.camel.RuntimeCamelException;

/**
 * Exception thrown if an element type does not actually implement the {@link Element} interface
 */
public class TypeNotElementRuntimeException extends RuntimeCamelException {
    private final Class<?> type;

    public TypeNotElementRuntimeException(Class<?> type) {
        super("The data type " + type.getCanonicalName() + " does not implement " + Element.class.getCanonicalName());
        this.type = type;
    }

    public Class<?> getType() {
        return type;
    }
}
