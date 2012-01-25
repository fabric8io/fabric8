/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import static org.fusesource.insight.log.elasticsearch.ElasticSender.quote;

public class InsightEventHandler implements EventHandler {

    private String name;
    private String index;
    private String type;
    private ElasticSender sender;

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
        CreateIndexRequest request = new CreateIndexRequest(index);
        sender.createIndexIfNeeded(request);
    }

    public void handleEvent(final Event event) {
        try {
            StringBuilder writer = new StringBuilder();
            writer.append("{ host : ");
            quote(name, writer);
            writer.append(", topic: ");
            quote(event.getTopic(), writer);
            writer.append(", properties: { ");
            boolean first = true;
            for (String name : event.getPropertyNames()) {
                if (first) {
                    first = false;
                } else {
                    writer.append(", ");
                }
                quote(name, writer);
                writer.append(": ");
                quote(event.getProperty(name).toString(), writer);
            }
            writer.append(" } }");

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

}
