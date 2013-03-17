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

package org.fusesource.insight.log.storage;

import org.fusesource.insight.storage.StorageService;
import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.fusesource.insight.log.service.support.MavenCoordinates.addMavenCoord;
import static org.fusesource.insight.log.storage.InsightUtils.formatDate;
import static org.fusesource.insight.log.storage.InsightUtils.quote;

public class InsightLogAppender implements PaxAppender {

    private static final Logger LOGGER = LoggerFactory.getLogger(InsightLogAppender.class);

    private String name;
    private String type;
    private StorageService storage;


    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setStorage(StorageService storage) {
        this.storage = storage;
    }

    public void doAppend(final PaxLoggingEvent paxLoggingEvent) {
        try {

            // Only store TRACE events which have a trace-id.
            if ( "TRACE".equals(paxLoggingEvent.getLevel().toString().toUpperCase()) &&
                  !paxLoggingEvent.getProperties().containsKey("trace-id") ) {
                return;
            }
            StringBuilder writer = new StringBuilder();
            writer.append("{ \"host\" : ");
            quote(name, writer);
            writer.append(",\n  \"timestamp\" : ");
            quote(formatDate(paxLoggingEvent.getTimeStamp()), writer);
            writer.append(",\n  \"level\" : ");
            quote(paxLoggingEvent.getLevel().toString(), writer);
            writer.append(",\n  \"logger\" : ");
            quote(paxLoggingEvent.getLoggerName(), writer);
            writer.append(",\n  \"thread\" : ");
            quote(paxLoggingEvent.getThreadName(), writer);
            writer.append(",\n  \"message\" : ");
            quote(paxLoggingEvent.getMessage(), writer);

            String[] throwable = paxLoggingEvent.getThrowableStrRep();
            if( throwable!=null ) {
                throwable = addMavenCoord(throwable);
                writer.append(",\n  \"exception\" : [");
                for (int i = 0; i < throwable.length; i++) {
                    if(i!=0)
                        writer.append(", ");
                    quote(throwable[i], writer);
                }
                writer.append("]");
            }

            writer.append(",\n  \"properties\" : { ");
            boolean first = true;
            Map<String, String> properties = new HashMap<String, String>();
            Set<Map.Entry> set = paxLoggingEvent.getProperties().entrySet();
            for (Map.Entry entry : set) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                if (key != null && value != null) {
                    properties.put(key.toString(), value.toString());
                }
            }
            addMavenCoord(properties);

            for (Object key : properties.keySet()) {
                if (first) {
                    first = false;
                } else {
                    writer.append(", ");
                }
                quote(key.toString(), writer);
                writer.append(": ");
                quote(properties.get(key).toString(), writer);
            }
            writer.append(" }");
            writer.append("\n}");

            storage.store(type, paxLoggingEvent.getTimeStamp(), writer.toString());
        } catch (Exception e) {
            LOGGER.warn("Error appending log to storage", e);
        }
    }

}
