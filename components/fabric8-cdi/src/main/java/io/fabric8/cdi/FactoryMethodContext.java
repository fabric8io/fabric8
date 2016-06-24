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

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.Bean;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;

public class FactoryMethodContext {

    private final Bean bean;
    private final Type sourceType;
    private final Type returnType;
    private final AnnotatedMethod factoryMethod;

    public FactoryMethodContext(Bean bean, Type sourceType, Type returnType, AnnotatedMethod factoryMethod) {
        this.bean = bean;
        this.sourceType = sourceType;
        this.returnType = returnType;
        this.factoryMethod = factoryMethod;
    }

    public Bean getBean() {
        return bean;
    }

    public Type getSourceType() {
        return sourceType;
    }

    public Type getReturnType() {
        return returnType;
    }

    public AnnotatedMethod getFactoryMethod() {
        return factoryMethod;
    }


    public static Set<FactoryMethodContext> sort(Set<FactoryMethodContext> items) {
        Set<FactoryMethodContext> sorted = new LinkedHashSet<>();
        Set<FactoryMethodContext> visited = new LinkedHashSet<>();
        for (FactoryMethodContext e : items) {
            visit(e, items, visited, sorted);
        }
        return sorted;
    }


    private static void visit(FactoryMethodContext item, Set<FactoryMethodContext> all, Set<FactoryMethodContext> visited, Set<FactoryMethodContext> sorted) {
        if (!visited.add(item)) {
            return;
        }
        for (FactoryMethodContext t : collectDependencies(item, all)) {
            visit(t, all, visited, sorted);
        }
        sorted.add(item);
    }
    
    private static Set<FactoryMethodContext> collectDependencies(FactoryMethodContext item, Set<FactoryMethodContext> all) {
        Set<FactoryMethodContext> dependencies = new LinkedHashSet<>();
        for (FactoryMethodContext candidate : all) {
            if (item.getSourceType().equals(candidate.getReturnType())) {
                dependencies.add(candidate);
            }
        }
        return dependencies;
    }
}
