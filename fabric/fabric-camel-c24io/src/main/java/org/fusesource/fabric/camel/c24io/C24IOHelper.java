/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.camel.c24io;

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
