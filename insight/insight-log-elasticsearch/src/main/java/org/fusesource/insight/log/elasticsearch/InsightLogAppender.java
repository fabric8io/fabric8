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

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.fusesource.insight.log.elasticsearch.ElasticSender.quote;

public class InsightLogAppender implements PaxAppender {

    // Making an assumption here that we will log less than 1,000,000 events/sec,  next this JVM
    // restarts, the next sequence number should be < any previously generated sequence numbers.
    static final private AtomicLong SEQUENCE_COUNTER = new AtomicLong(System.currentTimeMillis()*1000);
    
    private String name;
    private String index;
    private String type;
    private ElasticSender sender;

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");


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
        request = request.settings(
        "{"+
//            "\"number_of_shards\":10,"+
//            "\"number_of_replicas\":1,"+
            "\"analysis\":{"+
                "\"analyzer\":{"+
                    "\"lower\":{"+
                        "\"type\":\"custom\","+
                        "\"tokenizer\":\"keyword\","+
                        "\"filter\":[\"lowercase\"]"+
                    "}"+
                "}"+
            "}"+
        "}"
        );

        HashMap<String, Object> not_analyzed = new HashMap<String, Object>();
        not_analyzed.put("type", "string");
        not_analyzed.put("index", "not_analyzed");

        HashMap<String, Object> lower = new HashMap<String, Object>();
        lower.put("type", "string");
        lower.put("analyzer", "lower");

        HashMap<String, Object> properties = new HashMap<String, Object>();
        properties.put("host", lower);
        properties.put("level", lower);
        properties.put("thread", lower);
        properties.put("seq", not_analyzed);
        properties.put("logger", lower);

        HashMap<String, Object> options = new HashMap<String, Object>();
        options.put("properties", properties);
        request.mapping(type, options);

        sender.createIndexIfNeeded(request);
        */
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
            writer.append(",\n  \"seq\" : " + SEQUENCE_COUNTER.incrementAndGet());
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
            for (Object key : paxLoggingEvent.getProperties().keySet()) {
                if (first) {
                    first = false;
                } else {
                    writer.append(", ");
                }
                quote(key.toString(), writer);
                writer.append(": ");
                quote(paxLoggingEvent.getProperties().get(key).toString(), writer);
            }
            writer.append(" }");
            writer.append("\n}");

            String index = this.index + "-"+ new SimpleDateFormat("yyyy.MM.dd").format(new Date(paxLoggingEvent.getTimeStamp()));

            IndexRequest request = new IndexRequest()
                    .index(index)
                    .type(type)
                    .source(writer.toString())
                    .create(true);
            sender.put(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String formatDate(long timestamp) {
        return simpleDateFormat.format(new Date(timestamp));
    }

}
