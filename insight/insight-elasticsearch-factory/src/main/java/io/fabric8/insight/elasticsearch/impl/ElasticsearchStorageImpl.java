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
package io.fabric8.insight.elasticsearch.impl;

import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.insight.metrics.model.MetricsStorageService;
import io.fabric8.insight.metrics.model.QueryResult;
import io.fabric8.insight.metrics.mvel.MetricsStorageServiceImpl;
import io.fabric8.insight.storage.StorageService;
import org.apache.felix.scr.annotations.*;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component(immediate = true, name = "io.fabric8.insight.elasticsearch")
@Service({StorageService.class, MetricsStorageService.class})
public class ElasticsearchStorageImpl implements StorageService, MetricsStorageService, Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchStorageImpl.class);

    private static final SimpleDateFormat indexFormat = new SimpleDateFormat("yyyy.MM.dd");

    @Reference(name = "node", referenceInterface = org.elasticsearch.node.Node.class, target = "(cluster.name=insight)")
    private final ValidatingReference<Node> node = new ValidatingReference<>();

    private int max = 1000;

    private Thread thread;

    private volatile boolean running;

    private BlockingQueue<ActionRequest> queue = new LinkedBlockingQueue<ActionRequest>();

    private MetricsStorageService metricsStorage = new MetricsStorageServiceImpl(this);

    @Activate
    public void activate() {
        running = true;
        thread = new Thread(this, "ElasticStorage");
        thread.start();
    }

    @Deactivate
    public void deactivate() {
        running = false;
        if (thread != null) {
            thread.interrupt();
        }
    }

    @Override
    public void store(String type, long timestamp, QueryResult queryResult) {
        metricsStorage.store(type, timestamp, queryResult);
    }

    @Override
    public void store(String type, long timestamp, String jsonData) {
        Date ts = new Date(timestamp);
        Date utc = new Date(ts.getTime() + ts.getTimezoneOffset() * 60000);
        IndexRequest ir = new IndexRequest()
                .index("insight-"+ indexFormat.format(utc))
                .type(type)
                .source(jsonData)
                .create(true);
        queue.add(ir);
    }

    public void run() {
        while (running) {
            try {
                ActionRequest req = queue.take();
                // Send data
                BulkRequest bulk = new BulkRequest();
                int nb = 0;
                while (req != null && (nb == 0 || nb < max)) {
                    bulk.add(req);
                    nb++;
                    req = queue.poll();
                }
                if (bulk.numberOfActions() > 0) {
                    BulkResponse rep = node.get().client().bulk(bulk).actionGet();
                    for (BulkItemResponse bir : rep.getItems()) {
                        if (bir.isFailed()) {
                            LOGGER.warn("Error executing request: {}", bir.getFailureMessage());
                        }
                    }
                }
            } catch (Exception e) {
                if (running) {
                    LOGGER.warn("Error while sending requests", e);
                }
            }
        }
    }

    private void bindNode(Node node) {
        this.node.bind(node);
    }

    private void unbindNode(Node node) {
        this.node.unbind(node);
    }

}
