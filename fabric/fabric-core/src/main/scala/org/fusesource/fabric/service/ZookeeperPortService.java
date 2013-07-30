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
package org.fusesource.fabric.service;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessMultiLock;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.PlaceholderResolver;
import org.fusesource.fabric.api.PortService;
import org.fusesource.fabric.zookeeper.ZkPath;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.createDefault;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.deleteSafe;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.exists;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getStringData;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getChildren;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setData;

@Component(name = "org.fusesource.fabric.portservice.zookeeper",
           description = "Fabric ZooKeeper Port Service")
@Service(PortService.class)
public class ZookeeperPortService implements PortService {

    @Reference(cardinality = org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY)
    private CuratorFramework curator;
    private InterProcessLock lock;

    @Activate
    public void init() {
        this.lock = new InterProcessMultiLock(curator, Arrays.asList(ZkPath.PORTS_LOCK.getPath()));
    }

    public void destroy() {
        release();
    }


    @Override
    public int registerPort(Container container, String pid, String key, int fromPort, int toPort, Set<Integer> excludes)  {
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
                throw new FabricException("Could not acquire port lock");
            }
            throw new FabricException("Could not find port within range [" + fromPort + "," + toPort + "]");
        } catch (Exception ex) {
            throw new FabricException(ex);
        } finally {
            release();
        }
    }

    @Override
    public void registerPort(Container container, String pid, String key, int port)  {
        String portAsString = String.valueOf(port);
        String containerPortsPath = ZkPath.PORTS_CONTAINER_PID_KEY.getPath(container.getId(), pid, key);
        String ipPortsPath = ZkPath.PORTS_IP.getPath(container.getIp());
        try {
            if (lock.acquire(60, TimeUnit.SECONDS)) {
                createDefault(curator, containerPortsPath, portAsString);
                createDefault(curator, ipPortsPath, portAsString);

                setData(curator, containerPortsPath, portAsString);
                String existingPorts = getStringData(curator, ipPortsPath);
                if (!existingPorts.contains(portAsString)) {
                    setData(curator, ipPortsPath, existingPorts + " " + portAsString);
                }
            } else {
                throw new FabricException("Could not acquire port lock");
            }
        } catch (Exception ex) {
            throw new FabricException(ex);
        } finally {
            release();
        }
    }

    @Override
    public void unRegisterPort(Container container, String pid, String key) {
        String containerPortsPidKeyPath = ZkPath.PORTS_CONTAINER_PID_KEY.getPath(container.getId(), pid, key);
        String ipPortsPath = ZkPath.PORTS_IP.getPath(container.getIp());
        try {
            if (lock.acquire(60, TimeUnit.SECONDS)) {
                if (exists(curator, containerPortsPidKeyPath) != null) {
                    int port = lookupPort(container, pid, key);
                    deleteSafe(curator, containerPortsPidKeyPath);

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
                    setData(curator, ipPortsPath, sb.toString());
                }
            } else {
                throw new FabricException("Could not acquire port lock");
            }
        } catch (Exception ex) {
            throw new FabricException(ex);
        } finally {
            release();
        }
    }


    @Override
    public void unRegisterPort(Container container, String pid)  {
        String containerPortsPidPath = ZkPath.PORTS_CONTAINER_PID.getPath(container.getId(), pid);
        try {
            if (lock.acquire(60, TimeUnit.SECONDS)) {
                if (exists(curator, containerPortsPidPath) != null) {
                    for (String key : getChildren(curator, containerPortsPidPath)) {
                        unRegisterPort(container, pid, key);
                    }
                    deleteSafe(curator, containerPortsPidPath);
                }
            } else {
                throw new FabricException("Could not acquire port lock");
            }
        } catch (Exception ex) {
            throw new FabricException(ex);
        } finally {
            release();
        }
    }

    @Override
    public void unRegisterPort(Container container)  {
        String containerPortsPath = ZkPath.PORTS_CONTAINER.getPath(container.getId());
        try {
            if (lock.acquire(60, TimeUnit.SECONDS)) {
                if (exists(curator, containerPortsPath) != null) {
                    for (String pid : getChildren(curator, containerPortsPath)) {
                        unRegisterPort(container, pid);
                    }
                    deleteSafe(curator, containerPortsPath);
                }
            } else {
                throw new FabricException("Could not acquire port lock");
            }
        } catch (Exception ex) {
            throw new FabricException(ex);
        } finally {
            release();
        }
    }

    @Override
    public int lookupPort(Container container, String pid, String key) {
        int port = 0;
        String path = ZkPath.PORTS_CONTAINER_PID_KEY.getPath(container.getId(), pid, key);
        try {
            if (exists(curator, path) != null) {
                port = Integer.parseInt(getStringData(curator, path));
            }
        } catch (Exception ex) {
            throw new FabricException(ex);
        }
        return port;
    }

    @Override
    public Set<Integer> findUsedPortByContainer(Container container)  {
        HashSet<Integer> ports = new HashSet<Integer>();
        String path = ZkPath.PORTS_CONTAINER.getPath(container.getId());
        try {
            if (lock.acquire(60, TimeUnit.SECONDS)) {
                if (exists(curator, path) != null) {

                    for (String pid : getChildren(curator, path)) {
                        for (String key : getChildren(curator, ZkPath.PORTS_CONTAINER_PID.getPath(container.getId(), pid))) {
                            String port = getStringData(curator, ZkPath.PORTS_CONTAINER_PID_KEY.getPath(container.getId(), pid, key));
                            try {
                                ports.add(Integer.parseInt(port));
                            } catch (Exception ex) {
                                //ignore
                            }
                        }
                    }
                }
            } else {
                throw new FabricException("Could not acquire port lock");
            }
        } catch (Exception ex) {
            throw new FabricException(ex);
        } finally {
            release();
        }
        return ports;
    }

    @Override
    public Set<Integer> findUsedPortByHost(Container container)  {
        String ip = container.getIp();
        HashSet<Integer> ports = new HashSet<Integer>();
        String path = ZkPath.PORTS_IP.getPath(ip);
        try {
            if (lock.acquire(60, TimeUnit.SECONDS)) {
                createDefault(curator, path, "");
                String boundPorts = getStringData(curator, path);
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
                throw new FabricException("Could not acquire port lock");
            }
        } catch (Exception ex) {
            throw new FabricException(ex);
        } finally {
            release();
        }
        return ports;
    }

    private void release() {
        try {
            lock.release();
        } catch (Exception e) {
            //ignore?
        }
    }
}
