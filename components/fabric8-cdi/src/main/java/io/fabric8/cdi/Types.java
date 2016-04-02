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
package io.fabric8.cdi;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

public final class Types {

    public static final Type LIST_OF_STRINGS = genericType(List.class, String.class);
    public static final Type SET_OF_STRINGS = genericType(Set.class, String.class);
    
    private Types() {
        //Utility Class
    }
    
    public static final Type genericType(final Type raw, final Type... arguments) {
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return arguments;
            }

            @Override
            public Type getRawType() {
                return raw;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };
    }


    public static Class asClass(Type type) {
        if (type instanceof Class) {
            return ((Class) type);
        } else if (type instanceof ParameterizedType) {
            return asClass(((ParameterizedType) type).getRawType());
        } else {
            return null;
        }
    }
    
    public static Type superClassOf(Type type) {
        if (type instanceof Class) {
            return ((Class) type).getSuperclass();
        } else if (type instanceof ParameterizedType) {
            return superClassOf(((ParameterizedType) type).getRawType());
        } else {
            return null;
        }
    }
}
