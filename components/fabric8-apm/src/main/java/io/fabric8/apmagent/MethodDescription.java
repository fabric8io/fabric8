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

public class MethodDescription {
    private final String className;
    private final String methodName;
    private final String description;
    private final String methodSignature;
    private final String fullMethodName;

    MethodDescription(String className, String methodName, String description) {
        this.className = className.replace('/', '.');
        this.methodName = methodName.replace('/', '.');
        this.description = description;
        this.methodSignature = getMethodSignature(methodName, description);
        this.fullMethodName = this.className + "@" + this.methodSignature;
    }

    static String getMethodSignature(String name, String description) {
        return name.replace('/', '.') + description;
    }

    public String getFullMethodName() {
        return fullMethodName;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getDescription() {
        return description;
    }
}
