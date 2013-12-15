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

import biz.c24.io.api.data.Element;
import org.apache.camel.RuntimeCamelException;

/**
 * Exception thrown if an element type does not actually implement the {@link Element} interface
 */
public class TypeNotElementRuntimeException extends RuntimeCamelException {
    private final Class<?> type;

    public TypeNotElementRuntimeException(Class<?> type) {
        super("The data type " + type.getCanonicalName() + " does not implement " + Element.class.getCanonicalName());
        this.type = type;
    }

    public Class<?> getType() {
        return type;
    }
}
