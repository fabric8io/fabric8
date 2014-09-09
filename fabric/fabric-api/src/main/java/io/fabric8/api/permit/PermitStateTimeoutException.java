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
* An unchecked timeout exception
*
* @author thomas.diesler@jboss.com
* @since 05-Mar-2014
*/
public final class PermitStateTimeoutException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final PermitKey<?> state;
    private final long timeout;
    private final TimeUnit unit;

    public PermitStateTimeoutException(String message, PermitKey<?> state, long timeout, TimeUnit unit) {
        super(message);
        this.state = state;
        this.timeout = timeout;
        this.unit = unit;
    }

    public PermitKey<?> getState() {
        return state;
    }

    public long getTimeout() {
        return timeout;
    }

    public TimeUnit getUnit() {
        return unit;
    }
}
