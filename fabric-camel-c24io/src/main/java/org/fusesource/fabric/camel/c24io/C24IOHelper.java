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
import biz.c24.io.api.data.DataModel;
import biz.c24.io.api.data.Element;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.util.ObjectHelper;

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

    public static Element getMandatoryElement(Class<?> type) {
        Element element = getElement(type);
        if (element == null) {
            throw new TypeNotElementRuntimeException(type);
        }
        return element;
    }

    public static Element getElement(Class<?> elementType) {
        if (Element.class.isAssignableFrom(elementType)) {
            try {
                Object value = elementType.getMethod("getInstance").invoke(null);
                if (value instanceof Element) {
                    return (Element) value;
                }
            } catch (InvocationTargetException e) {
                throw new RuntimeCamelException(e.getTargetException());
            } catch (Exception e) {
                throw new RuntimeCamelException(e);
            }
        }
        Object object = ObjectHelper.newInstance(elementType, Object.class);
        if (object instanceof Element) {
            return (Element) object;
        } else if (object instanceof ComplexDataObject) {
            ComplexDataObject dataObject = (ComplexDataObject) object;
            return dataObject.getDefiningElementDecl();
        }
        return null;
    }
}
