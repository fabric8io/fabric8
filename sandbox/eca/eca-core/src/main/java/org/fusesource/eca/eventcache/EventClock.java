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

package org.fusesource.eca.eventcache;

import java.util.concurrent.TimeUnit;

/**
 * An abstraction over System.currentTimeMillis() - so can have Mock implementation for testing
 */
public interface EventClock {

    enum TYPE {System, Mock};

    /**
     * Get the current time in milliseconds
     *
     * @return the current time
     */
    long currentTimeMillis();

    /**
     * Set the current time.
     * <p/>
     * This will be a no-op on System time implementation
     */
    void setCurrentTime(int value, TimeUnit unit);

    /**
     * Advance the current time.
     * <p/>
     * This will be a no-op on System time implementation
     */
    void advanceClock(int value, TimeUnit unit);

    /**
     * Retreat the current time.
     * <p/>
     * This will be a no-op on System time implementation
     */
    void retreatClock(int value, TimeUnit unit);
}
