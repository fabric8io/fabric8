/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.cxf.endpoint;


import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.type.TypeFactory;

import org.apache.cxf.common.logging.LogUtils;


public class BeanValidationAnnotationIntrospector extends AnnotationIntrospector {
    private static final transient Logger LOG = LogUtils.getL7dLogger(BeanValidationAnnotationIntrospector.class);

    protected final TypeFactory typeFactory;

    public BeanValidationAnnotationIntrospector(TypeFactory typeFactory) {
        this.typeFactory = (typeFactory == null) ? TypeFactory.defaultInstance() : typeFactory;
    }

    
    @Override
    public Version version() {
        return new Version(1, 1, 0, "", "cxf", "json-schema-mbean");
    }

    
    @Override
    public boolean hasIgnoreMarker(AnnotatedMember m) {
        Member member = m.getMember();
        int modifiers = member.getModifiers();
        if (Modifier.isTransient(modifiers)) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Ignoring transient member " + m);
            }
            return true;
        } else if (m instanceof AnnotatedMethod) {
            AnnotatedMethod method = (AnnotatedMethod) m;
            String methodName = method.getName();
            // lets see if there is a transient field of the same name as the getter
            if (methodName.startsWith("get") && method.getParameterCount() == 0) {
                String fieldName = Introspector.decapitalize(methodName.substring(3));
                Class<?> declaringClass = method.getDeclaringClass();
                Field field = findField(fieldName, declaringClass);
                if (field != null) {
                    int fieldModifiers = field.getModifiers();
                    if (Modifier.isTransient(fieldModifiers)) {
                        LOG.fine("Ignoring member " + m + " due to transient field called " + fieldName);
                        return true;
                    }
                }
            }
        }
        return super.hasIgnoreMarker(m);

    }

    protected static Field findField(String fieldName, Class<?> declaringClass) {
        try {
            return declaringClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class<?> superclass = declaringClass.getSuperclass();
            if (superclass != null && superclass != declaringClass) {
                return findField(fieldName, superclass);
            } else {
                return null;
            }
        }
    }

    @Override
    public Boolean hasRequiredMarker(AnnotatedMember m) {
        NotNull annotation = m.getAnnotation(NotNull.class);
        if (annotation == null) {
            return null;
        }
        return Boolean.TRUE;
    }


}

