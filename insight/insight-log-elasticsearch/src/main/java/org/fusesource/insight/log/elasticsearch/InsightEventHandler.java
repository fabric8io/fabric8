/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
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
