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
package org.fusesource.bai.config.language;

public class MethodCallExpression extends org.apache.camel.model.language.MethodCallExpression {
    public MethodCallExpression() {
    }

    public MethodCallExpression(String beanName) {
        super(beanName);
    }

    public MethodCallExpression(String beanName, String method) {
        super(beanName, method);
    }

    public MethodCallExpression(Object instance) {
        super(instance);
    }

    public MethodCallExpression(Object instance, String method) {
        super(instance, method);
    }

    public MethodCallExpression(Class<?> type) {
        super(type);
    }

    public MethodCallExpression(Class<?> type, String method) {
        super(type, method);
    }
}
