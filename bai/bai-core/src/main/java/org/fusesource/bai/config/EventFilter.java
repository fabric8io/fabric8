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

import org.fusesource.bai.AuditEvent;
import org.fusesource.common.util.Filter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Filters {@link AuditEvent} by {@link EventType}
 */
@XmlRootElement(name = "event")
@XmlAccessorType(XmlAccessType.FIELD)
public class EventFilter implements Filter<AuditEvent> {
    @XmlAttribute(required = true)
    private EventType eventType;

    public EventFilter() {
    }

    public EventFilter(EventType eventType) {
        this.eventType = eventType;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + eventType + ")";
    }

    public boolean matches(AuditEvent event) {
        return eventType != null && eventType.equals(event.getEventType());
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }
}
