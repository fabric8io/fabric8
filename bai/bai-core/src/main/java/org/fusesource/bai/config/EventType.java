/*
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
package org.fusesource.bai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the kinds of events that can be audited
 */
public enum EventType {
    CREATED,
    COMPLETED,
    SENDING,
    SENT,
    FAILURE,
    FAILURE_HANDLED,
    REDELIVERY,
    ALL;

    private static final transient Logger LOG = LoggerFactory.getLogger(EventType.class);

    public static EventType parseEventType(String text) {
        if ("*".equals(text)) {
            return EventType.ALL;
        }
        EventType answer = EventType.simpleNames.get(text);
        if (answer != null) {
            return answer;
        }
        try {
            return EventType.valueOf(text.toUpperCase());
        } catch (Exception e) {
            LOG.warn("Event-type policy could not be parsed. Contains invalid event type: " + text);
            return null;
        }
    }

    public static Map<String, EventType> simpleNames;

    static {
        simpleNames = new HashMap<String, EventType>();
        simpleNames.put("created", CREATED);
        simpleNames.put("completed", COMPLETED);
        simpleNames.put("sending", SENDING);
        simpleNames.put("sent", SENT);
        simpleNames.put("failure", FAILURE);
        simpleNames.put("failureHandled", FAILURE_HANDLED);
        simpleNames.put("redelivery", REDELIVERY);
        simpleNames.put("all", ALL);
        simpleNames.put("*", ALL);
    }
}