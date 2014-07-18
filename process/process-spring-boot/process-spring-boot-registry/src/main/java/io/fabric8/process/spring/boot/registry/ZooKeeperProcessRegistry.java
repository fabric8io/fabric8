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
package io.fabric8.process.spring.boot.registry;

import org.apache.curator.CuratorConnectionLossException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Objects.firstNonNull;
import static io.fabric8.process.spring.boot.registry.ZooKeeperProcessRegistries.newCurator;
import static org.apache.zookeeper.KeeperException.NoNodeException;

public class ZooKeeperProcessRegistry implements ProcessRegistry {

    private final static Logger LOG = LoggerFactory.getLogger(ZooKeeperProcessRegistry.class);

    private final String hosts;

    private final CuratorFramework zk;

    public ZooKeeperProcessRegistry(String hosts) {
        this.hosts = hosts;
        zk = newCurator(hosts);
    }

    public static ZooKeeperProcessRegistry autodetectZooKeeperProcessRegistry() {
        String hosts = firstNonNull(System.getProperty("ZOOKEEPER_HOSTS"), "localhost:2181");
        return new ZooKeeperProcessRegistry(hosts);
    }

    public String hosts() {
        return hosts;
    }

    @Override
    public String readProperty(String key) {
        String path = convertPropertiesKeyToPath(key);
        try {
            return new String(zk.getData().forPath(path));
        } catch (NoNodeException e) {
            LOG.debug("NoNodeException thrown for path {}. Returning null.", path);
            return null;
        } catch (CuratorConnectionLossException e){
            LOG.warn("CuratorConnectionLossException thrown for path {}. Returning null.", path);
            return null;
        } catch(KeeperException.ConnectionLossException e) {
            LOG.warn("ConnectionLossException thrown for path {}. Returning null.", path);
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected String convertPropertiesKeyToPath(String key) {
        return "/" + key.replace('.', '/');
    }

}
