/**
 *
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
package io.fabric8.zookeeper.utils;

import io.fabric8.common.util.Objects;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple cache for use by master elected services which know they are the only code reading or writing from a
 * particular area of ZK; so that we can cache the fact that we've read or written to ZK to minimise the amount of ZK
 * reads/writes we make.
 *
 * e.g. when detecting services in Jolokia we can calculate the JSON to register into ZK and just perform a {@link #setStringData(String, String, org.apache.zookeeper.CreateMode)} method call; safe in the knowledge that if the JSON hasn't changed we won't be doing a ZK write; to avoid too much ZK noise.
 */
public class ZooKeeperMasterCache {
    private static final transient Logger LOG = LoggerFactory.getLogger(ZooKeeperMasterCache.class);

    private final CuratorFramework curator;
    private final Map<String,String> cache = new ConcurrentHashMap<>();

    public ZooKeeperMasterCache(CuratorFramework curator) {
        this.curator = curator;
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    public String getStringData(String zkPath) throws Exception {
        String answer = cache.get(zkPath);
        if (answer == null) {
            ZooKeeperUtils.getStringData(curator, zkPath);
        }
        return answer;
    }

    public void setStringData(String zkPath, String data, CreateMode createMode) throws Exception {
        String currentValue = cache.get(data);
        if (currentValue == null || !Objects.equal(currentValue, data)) {
            ZooKeeperUtils.setData(curator, zkPath, data, createMode);
            cache.put(zkPath, data);
        }
    }

    public void deleteData(String path) throws Exception {
        if (ZooKeeperUtils.exists(curator, path) != null) {
            LOG.info("unregistered web app at " + path);
            ZooKeeperUtils.deleteSafe(curator, path);
        }
        cache.remove(path);
    }
}
