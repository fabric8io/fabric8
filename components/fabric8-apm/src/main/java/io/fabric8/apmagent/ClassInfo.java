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
package io.fabric8.apmagent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ClassInfo {
    private ClassLoader classLoader;
    private Class originalClass;
    private String className;
    private byte[] original;
    private byte[] transformed;
    private boolean canTransform;
    private ConcurrentMap<String, MethodDescription> transformedMethods = new ConcurrentHashMap<>();
    private ConcurrentMap<String, MethodDescription> allMethods = new ConcurrentHashMap<>();

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className != null ? className.replace("/", ".") : null;
    }

    public byte[] getOriginal() {
        return original;
    }

    public void setOriginal(byte[] original) {
        this.original = original;
    }

    public Class getOriginalClass() {
        return originalClass;
    }

    public void setOriginalClass(Class originalClass) {
        this.originalClass = originalClass;
    }

    public byte[] getTransformed() {
        return transformed;
    }

    public void setTransformed(byte[] transformed) {
        this.transformed = transformed;
    }

    public boolean isCanTransform() {
        return canTransform;
    }

    public void setCanTransform(boolean canTransform) {
        this.canTransform = canTransform;
    }

    public void addMethod(String name, String description) {
        MethodDescription methodDescription = new MethodDescription(getClassName(), name, description);
        allMethods.putIfAbsent(methodDescription.getMethodSignature(), methodDescription);
    }

    public String addTransformedMethod(String name, String description) {
        String key = MethodDescription.getMethodSignature(name, description);
        MethodDescription methodDescription = allMethods.get(key);
        assert (methodDescription != null);
        transformedMethods.putIfAbsent(key, methodDescription);
        return key;
    }

    public void removeTransformedMethod(String fullMethodName) {
        transformedMethods.remove(fullMethodName);
    }

    public Set<String> getAllMethodNames() {
        Set<String> set = new HashSet<>();
        for (MethodDescription methodDescription : allMethods.values()) {
            set.add(methodDescription.getMethodName());
        }
        return set;
    }

    public Set<String> getAllTransformedMethodNames() {
        Set<String> set = new HashSet<>();
        for (MethodDescription methodDescription : transformedMethods.values()) {
            set.add(methodDescription.getMethodName());
        }
        return set;
    }

    public Collection<MethodDescription> getTransformedMethodDescriptions() {
        ArrayList<MethodDescription> result = new ArrayList<>();
        result.addAll(transformedMethods.values());
        return result;
    }

    public boolean isTransformed() {
        return transformed != null && transformed.length > 0;
    }

    public void resetTransformed() {
        transformed = null;
        transformedMethods.clear();
    }

}
