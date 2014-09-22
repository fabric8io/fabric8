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

import static io.fabric8.process.spring.boot.registry.ZooKeeperProcessRegistries.newCurator;
import static org.apache.zookeeper.KeeperException.NoNodeException;

/**
 * {@link io.fabric8.process.spring.boot.registry.ProcessRegistry} implementation reading properties from the ZooKeeper
 * nodes.
 * <br><br>
 * If {@code curator-framework} jar is present in the classpath, {@code ZooKeeperProcessRegistry} will be created.
 * {@code ZooKeeperProcessRegistry} attempts to read properties values from the ZooKeeper server.
 * <br><br>
 * In particular ZooKeeper registry will try to connect to the Fabric8 ZooKeeper runtime registry and read properties
 * from it. The default coordinates of Fabric8 runtime registry are {@code localhost:2181} (2181 is the default port used by the
 * Fabric8 to start ZooKeeper on). If you would like to change it, set {@code fabric8.process.registry.zk.hosts}
 * system property to the customized list of hosts:
 * <br><br>
 * {@code java -Dfabric8.process.registry.zk.hosts=host1:5555,host2:6666 -jar my-service.jar}
 * <br><br>
 * ZooKeeper registry interprets dots in the properties names as the slashes in ZNode paths. For example {@code foo.bar} property
 * will be resolved as the {@code foo/bar} ZNode path.
 * <br><br>
 * <pre>
 *   {@literal @}Value("${foo.bar}") // try to read foo/bar ZNode from the ZooKeeper
 *   String bar;
 * </pre>
 * <br><br>
 * If for some reasons you would like to explicitly disable the ZooKeeper process registry, use the
 * {@code fabric8.process.registry.zk.enabled} system property:
 * <br><br>
 * <pre>
 *  java -Dfabric8.process.registry.zk.hosts=host1:5555,host2:6666 -D-Dfabric8.process.registry.zk.enabled=false -jar my-service.jar
 * </pre>
 */
public class ZooKeeperProcessRegistry implements ProcessRegistry {

    private final static Logger LOG = LoggerFactory.getLogger(ZooKeeperProcessRegistry.class);

    public static final String HOSTS = "fabric8.process.registry.zk.hosts";

    private final String hosts;

    private final CuratorFramework zk;

    public ZooKeeperProcessRegistry(String hosts) {
        this.hosts = hosts;
        zk = newCurator(hosts);
    }

    public static ZooKeeperProcessRegistry autodetectZooKeeperProcessRegistry() {
        String hosts = System.getProperty(HOSTS, "localhost:2181");
        return new ZooKeeperProcessRegistry(hosts);
    }

    public String hosts() {
        return hosts;
    }

    // Overridden operations

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

    // Private helper methods

    protected String convertPropertiesKeyToPath(String key) {
        String path = "/" + key.replace('.', '/');
        LOG.debug("Converted key {} to ZK path {}.", key, path);
        return path;
    }

}