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
package io.fabric8.api.scr;

import io.fabric8.api.jcip.ThreadSafe;


/**
 * Provides validation support.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 13-Sep-2013
 */
@ThreadSafe
public final class ValidationSupport implements Validatable {

    /* This uses volatile to make sure that every thread sees the last written value
     *
     * - The use of AtomicBoolean would be wrong because it does not guarantee that
     *   prior written state is also seen by other threads
     */
    private volatile boolean valid;

    public void setValid() {
        valid = true;
    }

    public void setInvalid() {
        valid = false;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public void assertValid() {
        if (!valid) {
            RuntimeException rte = new InvalidComponentException();
            throw rte;
        }
    }
}