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

package org.fusesource.insight.graph.support;

import com.googlecode.jmxtrans.OutputWriter;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.LifecycleException;
import com.googlecode.jmxtrans.util.ValidationException;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;
import org.fusesource.insight.graph.JmxCollector;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.ZKData;
import org.linkedin.zookeeper.tracker.TrackedNode;
import org.linkedin.zookeeper.tracker.ZKDataReader;
import org.linkedin.zookeeper.tracker.ZooKeeperTreeTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 */
public class ZKClusterOutputWriter extends BaseOutputWriter implements OutputWriter {
    private static final transient Logger LOG = LoggerFactory.getLogger(ZKClusterOutputWriter.class);

    private final JmxCollector collector;
    private final ZooKeeperTreeTracker<OutputWriter> tracker;
    private Map<String, KeyedObjectPool> objectPool;

    public ZKClusterOutputWriter(final JmxCollector collector, String zkPath) {
        this.collector = collector;

        ZKDataReader<OutputWriter> reader = new ZKDataReader<OutputWriter>() {
            @Override
            public ZKData<OutputWriter> readData(IZKClient zkClient, String path, Watcher watcher) throws InterruptedException, KeeperException {
                Stat stat = new Stat();

                Unmarshaller<OutputWriter> unmarshaller = null;
                if (path.endsWith(".json")) {
                    unmarshaller = new JsonUnmarshaller<OutputWriter>(OutputWriter.class);
                } else if (path.endsWith(".properties")) {
                    unmarshaller = new PropertiesObjectWriterUnmarshaller();
                } else {
                    LOG.debug("Ignoring ZK Path: " + path + " as it doesn't end in .json");
                    return new ZKData<OutputWriter>(null, stat);
                }
                LOG.info("Reading ZK path: " + path + " and converting to an OutputWriter");

                byte[] data = zkClient.getData(path, watcher, stat);
                try {
                    OutputWriter outputWriter = unmarshaller.unmarshal(path, data);
                    if (outputWriter != null) {
                        configureWriter(outputWriter);
                        outputWriter.start();
                    }
                    return new ZKData<OutputWriter>(outputWriter, stat);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public boolean isEqual(OutputWriter o1, OutputWriter o2) {
                return o1 == o2 || (o1 != null && o1.equals(o2));
            }
        };
        this.tracker = new ZooKeeperTreeTracker<OutputWriter>(collector.getZkClient(), reader, zkPath);
    }

    @Override
    public void start() throws LifecycleException {
        super.start();
        try {
            tracker.track();
        } catch (Exception e) {
            throw new LifecycleException(e.getMessage(), e);
        }
    }

    @Override
    public void stop() throws LifecycleException {
        tracker.destroy();
        super.stop();
    }

    @Override
    public void validateSetup(Query query) throws ValidationException {
        Map<String, TrackedNode<OutputWriter>> tree = tracker.getTree();
        for (Map.Entry<String, TrackedNode<OutputWriter>> entry : tree.entrySet()) {
            String name = entry.getKey();
            TrackedNode<OutputWriter> value = entry.getValue();
            OutputWriter data = value.getData();
            if (data != null) {
                configureWriter(data);
                data.validateSetup(query);
            }
        }
    }

    protected void configureWriter(OutputWriter data) {
        if (objectPool != null) {
            data.setObjectPoolMap(objectPool);
        }
    }

    @Override
    public void doWrite(Query query) throws Exception {
        Map<String, TrackedNode<OutputWriter>> tree = tracker.getTree();
        for (Map.Entry<String, TrackedNode<OutputWriter>> entry : tree.entrySet()) {
            String name = entry.getKey();
            TrackedNode<OutputWriter> value = entry.getValue();
            OutputWriter data = value.getData();
            if (data != null) {
                configureWriter(data);
                data.doWrite(query);
            }
        }
    }

    @Override
    public void setObjectPoolMap(Map<String, KeyedObjectPool> objectPool) {
        this.objectPool = objectPool;
    }
}
