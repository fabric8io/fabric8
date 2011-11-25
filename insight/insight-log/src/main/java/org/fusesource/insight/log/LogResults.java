/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.insight.log;

import java.util.ArrayList;
import java.util.List;

/**
 * Results of a query which also records the first and last timestamp searched
 */
public class LogResults {
    private List<LogEvent> events;
    private Long fromTimestamp;
    private Long toTimestamp;
    private String host;

    public void addEvent(LogEvent event) {
        if (events == null) {
            events = new ArrayList<LogEvent>();
        }
        events.add(event);
    }

    public List<LogEvent> getEvents() {
        return events;
    }

    public void setEvents(List<LogEvent> events) {
        this.events = events;
    }

    public Long getFromTimestamp() {
        return fromTimestamp;
    }

    public void setFromTimestamp(Long fromTimestamp) {
        this.fromTimestamp = fromTimestamp;
    }

    public Long getToTimestamp() {
        return toTimestamp;
    }

    public void setToTimestamp(Long toTimestamp) {
        this.toTimestamp = toTimestamp;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
