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

import java.util.concurrent.TimeUnit;

/**
* A service that allows controlled transitions from one state to another.
*
* @author thomas.diesler@jboss.com
* @since 05-Mar-2014
*/
public interface PermitManager {

    /**
     * Activate the permit for the given key.
     */
    <T> void activate(PermitKey<T> key, T instance);

    /**
     * Deactivate the permit for the given key.
     *
     * This method blocks until all permits for the given key are returned.
     * No new permits can be aquired while the permit is deactive.
     */
    void deactivate(PermitKey<?> key);

    /**
     * Deactivate the permit for the given key.
     *
     * This method blocks until all permits for the given key are returned.
     * No new permits can be aquired while the permit is deactive.
     *
     * @throws PermitStateTimeoutException if the given timeout was reached before all permits were returned
     */
    void deactivate(PermitKey<?> key, long timeout, TimeUnit unit) throws PermitStateTimeoutException;

    /**
     * Aquire a permit for the given key.
     *
     * This method blocks until a permit is available.
     */
    <T> Permit<T> aquirePermit(PermitKey<T> key, boolean exclusive);

    /**
     * Aquire a permit for the given key.
     *
     * This method blocks until a permit is available.
     *
     * @throws PermitStateTimeoutException if the given timeout was reached before a permit became available
     */
    <T> Permit<T> aquirePermit(PermitKey<T> key, boolean exclusive, long timeout, TimeUnit unit) throws PermitStateTimeoutException;

    interface Permit<T> {

        /**
         * Get the associated key.
         */
        PermitKey<T> getPermitKey();

        /**
         * Get the instance associated with this permit
         */
        T getInstance();

        /**
         * Release this permit.
         */
        void release();
    }
}
