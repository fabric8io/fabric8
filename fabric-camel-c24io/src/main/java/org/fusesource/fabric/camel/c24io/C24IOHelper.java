/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel.c24io;

import java.lang.reflect.InvocationTargetException;

import biz.c24.io.api.data.ComplexDataObject;
import biz.c24.io.api.data.Element;
import org.apache.camel.RuntimeCamelException;

/**
 * @version $Revision$
 */
public final class C24IOHelper {
    private C24IOHelper() {
        // Helper class
    }
    public static Element getElement(String modelClassName) {
        try {
            Class<?> elementType = Class.forName(modelClassName);
            return getElement(elementType);
        } catch (RuntimeCamelException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeCamelException(e);
        }
    }

    public static Element getElement(Class<?> elementType) {
        if (elementType.isAssignableFrom(ComplexDataObject.class) && !elementType.equals(ComplexDataObject.class)) {
            try {
                return (Element) elementType.getMethod("getInstance").invoke(null);
            } catch (InvocationTargetException e) {
                throw new RuntimeCamelException(e.getTargetException());
            } catch (Exception e) {
                throw new RuntimeCamelException(e);
            }
        } else {
            return null;
        }
    }
}
