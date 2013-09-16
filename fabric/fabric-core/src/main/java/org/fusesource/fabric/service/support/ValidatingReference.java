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
package org.fusesource.fabric.service.support;

/**
 * A reference that validates its content on {@link #get()}
 *
 * @author Thomas.Diesler@jboss.com
 * @since 13-Sep-2013
 */
public class ValidatingReference<T> {

    private T reference;

    /**
     * Set the reference to the given instance
     */
    public synchronized void set(T ref) {
        reference = ref;
    }

    /**
     * Get the referenced instance
     * @throws InvalidComponentException If the reference is not valid
     */
    public synchronized T get() {
        if (reference == null) {
            RuntimeException rte = new InvalidComponentException();
            rte.printStackTrace();
            throw rte;
        }
        return reference;
    }

    /**
     * Get the referenced instance
     * @return The references instance or null
     */
    public synchronized T getOptional() {
        return reference;
    }

}