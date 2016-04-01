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

public class FilterItem {
    private String className;
    private String methodName;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
        if (className != null) {
            this.className = className.replace('/', '.');
        }
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public boolean matches(String className) {
        className = className.replace('/', '.');
        return className.startsWith(getClassName()) || className.matches(getClassName());
    }

    public boolean matches(String className, String method, boolean matchIfNoFilterMethod) {
        boolean result = matches(className);
        if (result) {
            result = false;
            if (method == null || method.isEmpty()) {
                if (getMethodName() == null || getMethodName().isEmpty()) {
                    result = true;
                }
            } else if (getMethodName() != null && !getMethodName().isEmpty()) {
                result = method.matches(getMethodName());
            } else {
                result = matchIfNoFilterMethod;
            }
        }
        return result;
    }
}
