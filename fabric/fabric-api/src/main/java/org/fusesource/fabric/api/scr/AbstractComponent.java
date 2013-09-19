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
package org.fusesource.fabric.api.scr;


/**
 * An abstract base class for validatable components.
 * {@link}
 * @author Thomas.Diesler@jboss.com
 * @since 13-Sep-2013
 */
public abstract class AbstractComponent implements Validatable {

    /* This uses volatile to make sure that every thread sees the last written value
     *
     * - The use of AtomicBoolean would be wrong because it does not guarantee that state written
     *   prior to an unsynchronized call to activateComponent() is also seen
     *
     * - Synchronizing all methods in here would also work, but would effectively cause
     *   a lock acquisition on every public method in every component
     */
    private volatile boolean active;

    public void activateComponent() {
        active = true;
    }

    public void deactivateComponent() {
        active = false;
    }

    @Override
    public boolean isValid() {
        return active;
    }

    @Override
    public void assertValid() {
        if (!active) {
            RuntimeException rte = new InvalidComponentException();
            rte.printStackTrace();
            throw rte;
        }
    }
}