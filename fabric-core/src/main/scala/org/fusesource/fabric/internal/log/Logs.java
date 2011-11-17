/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.internal.log;

import org.fusesource.fabric.api.log.LogEvent;
import org.ops4j.pax.logging.spi.PaxLocationInfo;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 */
public class Logs {
    public static LogEvent newInstance(PaxLoggingEvent event) {
        LogEvent answer = new LogEvent();
        answer.setMessage(event.getMessage());
        answer.setLogger(event.getLoggerName());
        answer.setTimestamp(new Date(event.getTimeStamp()));
        answer.setThread(event.getThreadName());
        answer.setException(event.getThrowableStrRep());

        Map eventProperties = event.getProperties();
        if (eventProperties != null && eventProperties.size() > 0) {
            Map<String,String> properties = new HashMap<String,String>();
            Set<Map.Entry> set = eventProperties.entrySet();
            for (Map.Entry entry  : set) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                if (key != null && value != null) {
                    properties.put(toString(key), toString(value));
                }
            }
            answer.setProperties(properties);
        }

        // TODO missing fields...
        // event.getRenderedMessage()
        // event.getFQNOfLoggerClass()
        PaxLocationInfo locationInformation = event.getLocationInformation();
        if (locationInformation != null) {
            answer.setClassName(locationInformation.getClassName());
            answer.setFileName(locationInformation.getFileName());
            answer.setMethodName(locationInformation.getMethodName());
            answer.setLineNumber(locationInformation.getLineNumber());
        }
        return answer;
    }

    protected static String toString(Object value) {
        // TODO should we add any custom marshalling for types?
        return value.toString();
    }
}
