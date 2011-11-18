/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.api.log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Results of a query which also records the first and last timestamp searched
 */
public class LogResults {
    private List<LogEvent> events;
    private Date fromTimestamp;
    private Date toTimestamp;

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

    public Date getFromTimestamp() {
        return fromTimestamp;
    }

    public void setFromTimestamp(Date fromTimestamp) {
        this.fromTimestamp = fromTimestamp;
    }

    public Date getToTimestamp() {
        return toTimestamp;
    }

    public void setToTimestamp(Date toTimestamp) {
        this.toTimestamp = toTimestamp;
    }
}
