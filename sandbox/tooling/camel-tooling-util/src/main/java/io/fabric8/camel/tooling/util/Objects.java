/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.camel.tooling.util;

public class Objects {

    /**
     * A helper method to return a non null value or the default value if it is null
     *
     * @param value
     * @param defaultValue
     * @param <T>
     * @return
     */
    public <T> T getOrElse(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    public <T> T notNull(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    //?def assertInjected[T <: AnyRef](value: T)(implicit m: ClassManifest[T]): T = notNull(value, "Value of type " + m.erasure.getName + " has not been injected!")

}
