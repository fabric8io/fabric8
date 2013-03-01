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

package org.fusesource.insight.log.elasticsearch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import static org.fusesource.insight.log.elasticsearch.ElasticSender.quote;

public class InsightEventHandler implements EventHandler {

    private String name;
    private String index;
    private String type;
    private ElasticSender sender;

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    private SimpleDateFormat indexFormat = new SimpleDateFormat("yyyy.MM.dd");

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ElasticSender getSender() {
        return sender;
    }

    public void setSender(ElasticSender sender) {
        this.sender = sender;
    }

    public void init() {
        /*
        CreateIndexRequest request = new CreateIndexRequest(index);

        HashMap<String, Object> not_analyzed = new HashMap<String, Object>();
        not_analyzed.put("type", "string");
        not_analyzed.put("index", "not_analyzed");

        HashMap<String, Object> properties = new HashMap<String, Object>();
        properties.put("seq", not_analyzed);

        HashMap<String, Object> options = new HashMap<String, Object>();
        options.put("properties", properties);
        request.mapping(type, options);

        sender.createIndexIfNeeded(request);
        */
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

            IndexRequest request = new IndexRequest()
                    .index(getIndex(timestamp))
                    .type(type)
                    .source(writer.toString())
                    .create(true);
            sender.put(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getIndex(long timestamp) {
        return this.index + "-"+ indexFormat.format(new Date(timestamp));
    }

    private String formatDate(long timestamp) {
        return simpleDateFormat.format(new Date(timestamp));
    }

}
