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
package io.fabric8.api.permit;

import io.fabric8.api.gravia.IllegalArgumentAssertion;

/**
 * Represents a system state
 *
 * @author thomas.diesler@jboss.com
 * @since 05-Mar-2014
 *
 * @Immutable
 */
public final class PermitKey<T> {

    private final Class<T> type;
    private final String name;

    public PermitKey(Class<T> type) {
        this(type, type.getName());
    }

    public PermitKey(Class<T> type, String name) {
        IllegalArgumentAssertion.assertNotNull(type, "type");
        IllegalArgumentAssertion.assertNotNull(name, "name");
        this.type = type;
        this.name = name;
    }

    public Class<T> getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PermitKey)) return false;
        PermitKey<?> other = (PermitKey<?>) obj;
        return name.equals(other.name);
    }

    @Override
    public String toString() {
        return "State[" + name + "]";
    }
}
