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
package io.fabric8.service;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.createDefault;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.deleteSafe;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.exists;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getChildren;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getStringData;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.setData;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.fabric8.api.scr.support.Strings;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import io.fabric8.api.Container;
import io.fabric8.api.FabricException;
import io.fabric8.api.PortService;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.zookeeper.ZkPath;
@ThreadSafe
@Component(name = "io.fabric8.portservice.zookeeper", label = "Fabric8 ZooKeeper Port Service", metatype = false)
@Service(PortService.class)
public final class ZookeeperPortService extends AbstractComponent implements PortService {

    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();

    private InterProcessReadWriteLock readWriteLock;

    @Activate
    void activate() {
        readWriteLock = new InterProcessReadWriteLock(curator.get(), ZkPath.PORTS_LOCK.getPath());
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public int registerPort(Container container, String pid, String key, int fromPort, int toPort, Set<Integer> excludes) {
        assertValid();
        InterProcessMutex lock = readWriteLock.writeLock();
        try {
            if (lock.acquire(60, TimeUnit.SECONDS)) {
                int port = lookupPort(container, pid, key);
                if (port > 0) {
                    return port;
                }
                Set<Integer> boundPorts = findUsedPortByHost(container);
                boundPorts.addAll(excludes);

                for (port = fromPort; port <= toPort; port++) {
                    if (!boundPorts.contains(port)) {
                        registerPort(container, pid, key, port);
                        return port;
                    }
                }
            } else {
                throw new FabricException("Could not acquire port lock due to timeout.");
            }
            throw new FabricException("Could not find port within range [" + fromPort + "," + toPort + "]");
        } catch (Exception ex) {
            throw FabricException.launderThrowable(ex);
        } finally {
            releaseLock(lock);
        }
    }

    @Override
    public void registerPort(Container container, String pid, String key, int port) {
        assertValid();
        String portAsString = String.valueOf(port);
        String containerPortsPath = ZkPath.PORTS_CONTAINER_PID_KEY.getPath(container.getId(), pid, key);
        String ip = container.getIp();
        assertValidIp(container, ip);
        String ipPortsPath = ZkPath.PORTS_IP.getPath(ip);
        InterProcessMutex lock = readWriteLock.writeLock();
        try {
            if (lock.acquire(60, TimeUnit.SECONDS)) {
                createDefault(curator.get(), containerPortsPath, portAsString);
                createDefault(curator.get(), ipPortsPath, portAsString);

                setData(curator.get(), containerPortsPath, portAsString);
                String existingPorts = getStringData(curator.get(), ipPortsPath);
                if (!existingPorts.contains(portAsString)) {
                    setData(curator.get(), ipPortsPath, existingPorts + " " + portAsString);
                }
            } else {
                throw new FabricException("Could not acquire port lock due to timeout.");
            }
        } catch (Exception ex) {
            throw FabricException.launderThrowable(ex);
        } finally {
            releaseLock(lock);
        }
    }

    protected static void assertValidIp(Container container, String ip) {
        if (Strings.isNullOrBlank(ip)) {
            throw new IllegalArgumentException("No IP defined for container " + container.getId());
        }
    }

    @Override
    public void unregisterPort(Container container, String pid, String key) {
        assertValid();
        String containerPortsPidKeyPath = ZkPath.PORTS_CONTAINER_PID_KEY.getPath(container.getId(), pid, key);
        String ip = container.getIp();
        assertValidIp(container, ip);
        String ipPortsPath = ZkPath.PORTS_IP.getPath(ip);
        InterProcessMutex lock = readWriteLock.writeLock();
        try {
            if (lock.acquire(60, TimeUnit.SECONDS)) {
                if (exists(curator.get(), containerPortsPidKeyPath) != null) {
                    int port = lookupPort(container, pid, key);
                    deleteSafe(curator.get(), containerPortsPidKeyPath);

                    Set<Integer> allPorts = findUsedPortByHost(container);
                    allPorts.remove(port);
                    StringBuilder sb = new StringBuilder();
                    boolean first = true;
                    for (Integer p : allPorts) {
                        if (first) {
                            sb.append(p);
                            first = false;
                        } else {
                            sb.append(" ").append(p);
                        }
                    }
                    setData(curator.get(), ipPortsPath, sb.toString());
                }
            } else {
                throw new FabricException("Could not acquire port lock due to timeout.");
            }
        } catch (Exception ex) {
            throw FabricException.launderThrowable(ex);
        } finally {
            releaseLock(lock);
        }
    }

    @Override
    public void unregisterPort(Container container, String pid) {
        assertValid();
        String containerPortsPidPath = ZkPath.PORTS_CONTAINER_PID.getPath(container.getId(), pid);
        InterProcessMutex lock = readWriteLock.writeLock();
        try {
            if (lock.acquire(60, TimeUnit.SECONDS)) {
                if (exists(curator.get(), containerPortsPidPath) != null) {
                    for (String key : getChildren(curator.get(), containerPortsPidPath)) {
                        unregisterPort(container, pid, key);
                    }
                    deleteSafe(curator.get(), containerPortsPidPath);
                }
            } else {
                throw new FabricException("Could not acquire port lock due to timeout.");
            }
        } catch (Exception ex) {
            throw FabricException.launderThrowable(ex);
        } finally {
            releaseLock(lock);
        }
    }

    @Override
    public void unregisterPort(Container container) {
        assertValid();
        String containerPortsPath = ZkPath.PORTS_CONTAINER.getPath(container.getId());
        InterProcessMutex lock = readWriteLock.writeLock();
        try {
            if (lock.acquire(60, TimeUnit.SECONDS)) {
                if (exists(curator.get(), containerPortsPath) != null) {
                    for (String pid : getChildren(curator.get(), containerPortsPath)) {
                        unregisterPort(container, pid);
                    }
                    deleteSafe(curator.get(), containerPortsPath);
                }
            } else {
                throw new FabricException("Could not acquire port lock due to timeout.");
            }
        } catch (Exception ex) {
            throw FabricException.launderThrowable(ex);
        } finally {
            releaseLock(lock);
        }
    }

    @Override
    public int lookupPort(Container container, String pid, String key) {
        assertValid();
        int port = 0;
        String path = ZkPath.PORTS_CONTAINER_PID_KEY.getPath(container.getId(), pid, key);
        InterProcessMutex lock = readWriteLock.readLock();
        try {
            if (lock.acquire(60, TimeUnit.SECONDS)) {
                if (exists(curator.get(), path) != null) {
                    port = Integer.parseInt(getStringData(curator.get(), path));
                }
            }
        } catch (Exception ex) {
            throw FabricException.launderThrowable(ex);
        } finally {
            releaseLock(lock);
        }
        return port;
    }

    @Override
    public Set<Integer> findUsedPortByContainer(Container container) {
        assertValid();
        Set<Integer> ports = new HashSet<Integer>();
        String path = ZkPath.PORTS_CONTAINER.getPath(container.getId());
        InterProcessMutex lock = readWriteLock.readLock();
        try {
            if (lock.acquire(60, TimeUnit.SECONDS)) {
                if (exists(curator.get(), path) != null) {

                    for (String pid : getChildren(curator.get(), path)) {
                        for (String key : getChildren(curator.get(), ZkPath.PORTS_CONTAINER_PID.getPath(container.getId(), pid))) {
                            String port = getStringData(curator.get(), ZkPath.PORTS_CONTAINER_PID_KEY.getPath(container.getId(), pid, key));
                            try {
                                ports.add(Integer.parseInt(port));
                            } catch (Exception ex) {
                                //ignore
                            }
                        }
                    }
                }
            } else {
                throw new FabricException("Could not acquire port lock due to timeout.");
            }
        } catch (Exception ex) {
            throw FabricException.launderThrowable(ex);
        } finally {
            releaseLock(lock);
        }
        return ports;
    }

    @Override
    public Set<Integer> findUsedPortByHost(Container container) {
        assertValid();
        String ip = container.getIp();
        assertValidIp(container, ip);
        Set<Integer> ports = new HashSet<Integer>();
        String path = ZkPath.PORTS_IP.getPath(ip);
        InterProcessMutex lock = readWriteLock.readLock();
        try {
            if (lock.acquire(60, TimeUnit.SECONDS)) {
                createDefault(curator.get(), path, "");
                String boundPorts = getStringData(curator.get(), path);
                if (boundPorts != null && !boundPorts.isEmpty()) {
                    for (String port : boundPorts.split(" ")) {
                        try {
                            ports.add(Integer.parseInt(port.trim()));
                        } catch (NumberFormatException ex) {
                            //ignore
                        }
                    }
                }
            } else {
                throw new FabricException("Could not acquire port lock due to timeout.");
            }
        } catch (Exception ex) {
            throw FabricException.launderThrowable(ex);
        } finally {
            releaseLock(lock);
        }
        return ports;
    }

    private void releaseLock(InterProcessMutex lock) {
        try {
            if (lock != null)
                lock.release();
        } catch (Exception e) {
            //ignore?
        }
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }
}
