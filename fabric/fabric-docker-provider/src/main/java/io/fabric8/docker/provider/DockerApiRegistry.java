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
package io.fabric8.docker.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.common.util.JMXUtils;
import io.fabric8.zookeeper.ZkPath;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.mina.util.CopyOnWriteMap;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getSubstitutedData;

/**
 */
@Component(name = "io.fabric8.docker.api.registry.mbean", label = "Fabric8 Docker REST API Registry MBean",
        description = "A JMX MBean which keeps track of all the containers and their Docker REST APIs by watching ZooKeeper",
        immediate = true, metatype = true, policy = ConfigurationPolicy.REQUIRE)
public class DockerApiRegistry extends AbstractComponent implements DockerApiRegistryMXBean {
    private static final transient Logger LOG = LoggerFactory.getLogger(DockerApiRegistry.class);

    @Reference(referenceInterface = MBeanServer.class, bind = "bindMBeanServer", unbind = "unbindMBeanServer")
    private MBeanServer mbeanServer;

    @Reference(referenceInterface = CuratorFramework.class, bind = "bindCurator", unbind = "unbindCurator")
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();

    private ObjectName objectName;
    private PathChildrenCache apiCache;

    private Map<String, ContainerApiDTO> containersMap = new CopyOnWriteMap<String, ContainerApiDTO>();
    private AtomicBoolean registered = new AtomicBoolean(false);

    @Activate
    void activate() {
        activateComponent();
        CuratorFramework curatorFramework = curator.get();

        String path = ZkPath.WEBAPPS_CLUSTER.getPath("io.hawt.dockerui.hawtio-dockerui");
        Stat stat = null;
        try {
            stat = curatorFramework.checkExists().forPath(path);
        } catch (Exception e) {
            LOG.warn("Failed to check if path " + path + " existed: " + e, e);
        }
        if (stat == null) {
            try {
                curatorFramework.create().creatingParentsIfNeeded().forPath(path);
            } catch (Exception e) {
                LOG.warn("Tried to create path " + path + " but got: " + e, e);
            }
        }

        apiCache = new PathChildrenCache(curatorFramework, path, false);
        apiCache.getListenable().addListener(new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent event) throws Exception {
                try {
                    LOG.debug("event: " + event);
                    ChildData childData = event.getData();
                    if (childData != null) {
                        PathChildrenCacheEvent.Type eventType = event.getType();
                        byte[] data = childData.getData();
                        LOG.info("Got childEvent " + eventType + " " + childData);
                        if (isValidData(data)) {
                            loadData(curatorFramework, eventType, data);
                        } else {
                            // do we have any children?
                            String path1 = childData.getPath();
                            List<String> names = curatorFramework.getChildren().forPath(path1);
                            for (String name : names) {
                                String fullPath = path1 + "/" + name;
                                data = curatorFramework.getData().forPath(fullPath);
                                if (isValidData(data)) {
                                    LOG.info("Loading data: " + fullPath);
                                    loadData(curatorFramework, eventType, data);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.warn("Caught: " + e, e);
                }
            }
        });
        try {
            //apiCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
            apiCache.start(PathChildrenCache.StartMode.NORMAL);
        } catch (Exception e) {
            LOG.warn("Failed to build initial apiCache: " + e, e);
        }
    }

    @Deactivate
    void deactivate() {
        try {
            apiCache.close();
        } catch (IOException e) {
            LOG.warn("Failed to close apiCache " + e, e);
        }
        deactivateComponent();
        try {
            unregisterMBean();
        } catch (Exception e) {
            LOG.warn("Caught while unregistering mbean on deactivate: " + e, e);
        }
    }


    protected void loadData(CuratorFramework curatorFramework, PathChildrenCacheEvent.Type eventType, byte[] data) throws Exception {
        ContainerApiDTO containerApiDTO = loadContainerApiDto(curatorFramework, data);
        if (containerApiDTO != null) {
            String key = containerApiDTO.getContainer();
            if (eventType.equals(PathChildrenCacheEvent.Type.CHILD_ADDED)) {
                containersMap.put(key, containerApiDTO);
            } else if (eventType.equals(PathChildrenCacheEvent.Type.CHILD_REMOVED)) {
                containersMap.remove(key);
            }
            if (containersMap.isEmpty()) {
                unregisterMBean();
            } else {
                registerMBean();
            }
        }
    }

    protected boolean isValidData(byte[] data) {
        return data != null && data.length > 0 && isValid();
    }

    @Override
    public List<String> getHostContainerIds() {
        List<String> answer = new ArrayList<String>(containersMap.keySet());
        Collections.sort(answer);
        return answer;
    }

    @Override
    public List<ContainerApiDTO> hostContainers() {
        List<ContainerApiDTO> answer = new ArrayList<ContainerApiDTO>(containersMap.values());
        return answer;
    }

    public ObjectName getObjectName() throws MalformedObjectNameException {
        if (objectName == null) {
            return new ObjectName("io.fabric8:type=DockerApiRegistry");
        }
        return objectName;
    }

    public void setObjectName(ObjectName objectName) {
        this.objectName = objectName;
    }

    private void registerMBean() throws Exception {
        if (mbeanServer != null && registered.compareAndSet(false, true)) {
            JMXUtils.registerMBean(this, mbeanServer, getObjectName());
        }
    }

    protected void unregisterMBean() throws Exception {
        if (mbeanServer != null && registered.compareAndSet(true, false)) {
            JMXUtils.unregisterMBean(mbeanServer, getObjectName());
        }
    }

    protected ContainerApiDTO loadContainerApiDto(CuratorFramework curatorFramework, byte[] data) throws IOException, URISyntaxException {
        String text = new String(data).trim();
        if (!text.isEmpty()) {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> map = mapper.readValue(data, HashMap.class);
            String id = stringValue(map, "id", "container");
            if (id != null) {
                String container = stringValue(map, "container", "agent");
                List services = listValue(map, "services");
                if (services != null && !services.isEmpty()) {
                    Object service = services.get(0);
                    if (service != null) {
                        String serviceText = getSubstitutedData(curatorFramework, service.toString());
                        if (serviceText != null && serviceText.length() > 0) {
                            String url = serviceText + "/dockerapi";
                            return new ContainerApiDTO(container, url);
                        }
                    }
                }
            }
        }
        return null;
    }

    protected static String stringValue(Map<String, Object> map, String... keys) {
        Object value = value(map, keys);
        if (value instanceof String) {
            return (String) value;
        } else if (value != null) {
            return value.toString();
        }
        return null;
    }

    protected static List listValue(Map<String, Object> map, String... keys) {
        Object value = value(map, keys);
        if (value instanceof List) {
            return (List) value;
        } else if (value instanceof Object[]) {
            return Arrays.asList((Object[]) value);
        }
        return null;
    }

    protected static Object value(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }


    void bindMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    void unbindMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = null;
    }
}
