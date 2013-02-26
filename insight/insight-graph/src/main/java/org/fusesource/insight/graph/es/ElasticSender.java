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

package org.fusesource.insight.graph.es;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ElasticSender implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSender.class);

    private Node node;
    private int max = 1000;
    private Thread thread;
    private volatile boolean running;
    private BlockingQueue<IndexRequest> queue = new LinkedBlockingQueue<IndexRequest>();

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public void init() {
        running = true;
        thread = new Thread(this, "ElasticSender");
        thread.start();
    }

    public void destroy() {
        running = false;
        thread.interrupt();
    }

    public void put(IndexRequest data) {
        queue.add(data);
    }

    public void createIndexIfNeeded(CreateIndexRequest request) {
        try {
            node.client().admin().indices().create(request).actionGet();
        } catch (org.elasticsearch.indices.IndexAlreadyExistsException e) {
            // Ignore
        }
    }

    public void run() {
        while (running) {
            try {
                IndexRequest req = queue.take();
                // Send data
                BulkRequest bulk = new BulkRequest();
                int nb = 0;
                while (req != null && (nb == 0 || nb < max)) {
                    bulk.add(req);
                    nb++;
                    req = queue.poll();
                }
                if (bulk.numberOfActions() > 0) {
                    BulkResponse rep = node.client().bulk(bulk).actionGet();
                    for (BulkItemResponse bir : rep.items()) {
                        if (bir.failed()) {
                            LOGGER.warn("Error indexing item: {}", bir.getFailureMessage());
                        }
                    }
                }
            } catch (Exception e) {
                if (running) {
                    LOGGER.warn("Error while sending indexes", e);
                }
            }
        }
    }

    /**
     * Produce a string in double quotes with backslash sequences in all the
     * right places. A backslash will be inserted within </, producing <\/,
     * allowing JSON text to be delivered in HTML. In JSON text, a string
     * cannot contain a control character or an unescaped quote or backslash.
     * @param string A String
     * @return  A String correctly formatted for insertion in a JSON text.
     */
    public static void quote(String string, StringBuilder w) {
        if (string == null || string.length() == 0) {
            w.append("\"\"");
            return;
        }

        char         b;
        char         c = 0;
        String       hhhh;
        int          i;
        int          len = string.length();

        w.append('"');
        for (i = 0; i < len; i += 1) {
            b = c;
            c = string.charAt(i);
            switch (c) {
            case '\\':
            case '"':
                w.append('\\');
                w.append(c);
                break;
            case '/':
                if (b == '<') {
                    w.append('\\');
                }
                w.append(c);
                break;
            case '\b':
                w.append("\\b");
                break;
            case '\t':
                w.append("\\t");
                break;
            case '\n':
                w.append("\\n");
                break;
            case '\f':
                w.append("\\f");
                break;
            case '\r':
                w.append("\\r");
                break;
            default:
                if (c < ' ' || (c >= '\u0080' && c < '\u00a0') ||
                               (c >= '\u2000' && c < '\u2100')) {
                    hhhh = "000" + Integer.toHexString(c);
                    w.append("\\u" + hhhh.substring(hhhh.length() - 4));
                } else {
                    w.append(c);
                }
            }
        }
        w.append('"');
    }
}
