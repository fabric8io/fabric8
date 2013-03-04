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

import org.elasticsearch.action.index.IndexRequest;
import org.fusesource.insight.elasticsearch.ElasticSender;
import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fusesource.insight.log.elasticsearch.InsightUtils.formatDate;
import static org.fusesource.insight.log.elasticsearch.InsightUtils.getIndex;
import static org.fusesource.insight.log.elasticsearch.InsightUtils.quote;

public class InsightLogAppender implements PaxAppender {

    private static final Logger LOGGER = LoggerFactory.getLogger(InsightLogAppender.class);

    private String name;
    private String index;
    private String type;
    private ElasticSender sender;


    public void setName(String name) {
        this.name = name;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setSender(ElasticSender sender) {
        this.sender = sender;
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

            IndexRequest request = new IndexRequest()
                    .index(getIndex(this.index, paxLoggingEvent.getTimeStamp()))
                    .type(type)
                    .source(writer.toString())
                    .create(true);

            sender.push(request);
        } catch (Exception e) {
            LOGGER.warn("Error appending log to elastic search", e);
        }
    }

}
