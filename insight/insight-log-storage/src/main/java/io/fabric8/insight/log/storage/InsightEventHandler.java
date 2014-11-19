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
package io.fabric8.insight.log.storage;

import io.fabric8.insight.storage.StorageService;
import org.apache.felix.scr.annotations.*;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static io.fabric8.insight.log.storage.InsightUtils.formatDate;
import static io.fabric8.insight.log.storage.InsightUtils.quote;

@Component(immediate = true, name = "io.fabric8.insight.log.storage.events")
@Service(EventHandler.class)
@Properties({
        @Property(name = "event.topics", value = "*")
})
public class InsightEventHandler implements EventHandler {

    public static final String LOG_TYPE = "es.evt.type";

    private static final Logger LOGGER = LoggerFactory.getLogger(InsightLogAppender.class);

    private String name;

    private String type = "events";

    @Reference
    private StorageService storageService;

    @Activate
    public void activate(Map<String, ?> configuration) {
        name = System.getProperty("runtime.id");
        if (configuration.containsKey(LOG_TYPE)) {
            type = (String) configuration.get(LOG_TYPE);
        }
    }

    @Modified
    public void modified(Map<String, ?> configuration) {
        if (configuration.containsKey(LOG_TYPE)) {
            type = (String) configuration.get(LOG_TYPE);
        } else {
            type = "log";
        }
    }

    public void handleEvent(final Event event) {
        try {
            StringBuilder writer = new StringBuilder();
            writer.append("{ \"host\": ");
            quote(name, writer);
            writer.append(", \"topic\": ");
            quote(event.getTopic(), writer);
            writer.append(", \"properties\": { ");
            boolean first = true;

            long timestamp = 0;

            for (String name : event.getPropertyNames()) {
                if (first) {
                    first = false;
                } else {
                    writer.append(", ");
                }
                quote(name, writer);
                writer.append(": ");
                Object value = event.getProperty(name);
                if (value == null) {
                    writer.append("null");
                } else if (EventConstants.TIMESTAMP.equals(name) && value instanceof Long) {
                    timestamp = (Long) value;
                    quote(formatDate(timestamp), writer);
                } else if (value.getClass().isArray()) {
                    writer.append(" [ ");
                    boolean vfirst = true;
                    for (Object v : ((Object[]) value)) {
                        if (!vfirst) {
                            writer.append(", ");
                        } else {
                            vfirst = false;
                        }
                        quote(v.toString(), writer);
                    }
                    writer.append(" ] ");
                } else {
                    quote(value.toString(), writer);
                }
            }
            writer.append(" } }");

            if (timestamp == 0) {
                timestamp = System.currentTimeMillis();
            }
            if (type != null && storageService != null) {
                storageService.store(type, timestamp, writer.toString());
            }
        } catch (Exception e) {
            LOGGER.warn("Error appending log to elastic search", e);
        }
    }

}
