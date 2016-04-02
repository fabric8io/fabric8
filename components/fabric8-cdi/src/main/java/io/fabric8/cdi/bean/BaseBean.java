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
package io.fabric8.cdi.bean;

import io.fabric8.cdi.Types;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.util.AnnotationLiteral;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class BaseBean<T> implements Bean<T>, PassivationCapable {

    private static final Annotation[] DEFAULT_QUALIFIERS = {
            new AnnotationLiteral<Default>() {
            },
            new AnnotationLiteral<Any>() {
            }
    };

    private Type beanType;
    private final String name;
    private final Set<Type> types;
    private final Set<Annotation> qualifiers;

    public BaseBean(String name, Annotation... annotations) {
        this.name = name;
        ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();
        this.beanType = (Class<T>) genericSuperclass.getActualTypeArguments()[0];
        Set<Type> allTypes = new LinkedHashSet<>();
        allTypes.add(beanType);

        for (Type t = Types.superClassOf(beanType); !allTypes.contains(Object.class) && t != null; t =   Types.superClassOf(t)) {
            allTypes.add(t);
        }

        types = Collections.unmodifiableSet(allTypes);
        qualifiers = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(annotations)));
    }

    public BaseBean(String name) {
        this.name = name;
        ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();
        this.beanType = (Class<T>) genericSuperclass.getActualTypeArguments()[0];
        Set<Type> allTypes = new LinkedHashSet<>();
        allTypes.add(beanType);

        for (Type t = Types.superClassOf(beanType); !allTypes.contains(Object.class) && t != null; t =   Types.superClassOf(t)) {
            allTypes.add(t);
        }

        types = Collections.unmodifiableSet(allTypes);
        qualifiers = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(DEFAULT_QUALIFIERS)));
    }

    public BaseBean(String name, Type beanType, Annotation ... annotations) {
        this.name = name;
        this.beanType = beanType;
        Set<Type> allTypes = new LinkedHashSet<>();
        allTypes.add(beanType);

        for (Type t = Types.superClassOf(beanType); !allTypes.contains(Object.class) && t != null; t =   Types.superClassOf(t)) {
            allTypes.add(t);
        }

        types = Collections.unmodifiableSet(allTypes);
        qualifiers = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(annotations)));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<?> getBeanClass() {
        return Types.asClass(beanType);
    }

    @Override
    public String getId() {
        return getName();
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public Set<Type> getTypes() {
        return types;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return ApplicationScoped.class;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public boolean isAlternative() {
        return false;
    }
}
