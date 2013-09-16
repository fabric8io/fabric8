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

public class DefaultEventClock implements EventClock {

    private final EventClock eventClock;

    public DefaultEventClock() {
        this(TYPE.System);
    }

    public DefaultEventClock(TYPE type) {
        if (type.equals(TYPE.Mock)) {
            eventClock = new MockEventClock();
        } else {
            eventClock = new SystemEventClock();
        }
    }

    public long currentTimeMillis() {
        return eventClock.currentTimeMillis();
    }

    public void setCurrentTime(int value, TimeUnit unit) {
        eventClock.setCurrentTime(value, unit);
    }

    public void advanceClock(int value, TimeUnit unit) {
        eventClock.advanceClock(value, unit);
    }

    public void retreatClock(int value, TimeUnit unit) {
        eventClock.retreatClock(value, unit);
    }
}
