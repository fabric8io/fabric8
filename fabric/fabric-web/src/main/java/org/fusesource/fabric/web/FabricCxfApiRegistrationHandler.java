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
package org.fusesource.fabric.web;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.zookeeper.CreateMode;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.jcip.GuardedBy;
import org.fusesource.fabric.api.jcip.ThreadSafe;
import org.fusesource.fabric.api.scr.AbstractComponent;
import org.fusesource.fabric.api.scr.ValidatingReference;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils;
import org.fusesource.insight.log.support.Strings;
import org.ops4j.pax.web.service.spi.ServletEvent;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.osgi.framework.Bundle;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.BadAttributeValueExpException;
import javax.management.BadBinaryOpValueExpException;
import javax.management.BadStringOperationException;
import javax.management.InvalidApplicationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.add;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.deleteSafe;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setData;

@ThreadSafe
@Component(name = "org.fusesource.fabric.web.api", description = "Fabric API Registration Handler", immediate = true)
public final class FabricCxfApiRegistrationHandler extends AbstractComponent implements ConnectionStateListener {
    public static final String CXF_API_ENDPOINT_MBEAN_NAME = "org.apache.cxf:*";

    private static final Logger LOGGER = LoggerFactory.getLogger(FabricCxfApiRegistrationHandler.class);

    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();
    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();
    @Reference(referenceInterface = ConfigurationAdmin.class, cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    private ConfigurationAdmin configAdmin;

    @GuardedBy("ConcurrentMap")
    private final ConcurrentMap<Bundle, WebEvent> webEvents = new ConcurrentHashMap<Bundle, WebEvent>();
    @GuardedBy("ConcurrentMap")
    private final ConcurrentMap<Bundle, Map<String, ServletEvent>> servletEvents = new ConcurrentHashMap<Bundle, Map<String, ServletEvent>>();

    private NotificationListener listener = new NotificationListener() {
        @Override
        public void handleNotification(Notification notification, Object handback) {
            if (notification instanceof MBeanServerNotification) {
                MBeanServerNotification mBeanServerNotification = (MBeanServerNotification) notification;
                ObjectName mBeanName = mBeanServerNotification.getMBeanName();
                onMBeanEvent(getCurrentContainer(), mBeanName);
            }
        }
    };

    private NotificationFilter filter = new NotificationFilter() {
        @Override
        public boolean isNotificationEnabled(Notification notification) {
            return true;
        }
    };

    private QueryExp isCxfServiceEndpointQuery = new QueryExp() {
        @Override
        public boolean apply(ObjectName name) throws BadStringOperationException, BadBinaryOpValueExpException, BadAttributeValueExpException, InvalidApplicationException {
            String type = name.getKeyProperty("type");
            return type != null && "Bus.Service.Endpoint".equals(type);
        }

        @Override
        public void setMBeanServer(MBeanServer s) {
        }
    };

    private MBeanServer mBeanServer;
    private boolean registeredListener;

    @Activate
    synchronized void activate(ComponentContext context) throws Exception {
        activateComponent();

        if (mBeanServer == null) {
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
        }

        if (mBeanServer != null) {
            Object handback = null;
            mBeanServer.addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, listener, filter, handback);
            this.registeredListener = true;
        }
        replay();
    }

    @Deactivate
    synchronized void deactivate() throws Exception {
        deactivateComponent();
        if (registeredListener && mBeanServer != null) {
            mBeanServer.removeNotificationListener(MBeanServerDelegate.DELEGATE_NAME, listener);
        }
    }

    @Override
    public void stateChanged(CuratorFramework client, ConnectionState newState) {
        if (isValid()) {
            switch (newState) {
                case CONNECTED:
                case RECONNECTED:
                    replay();
            }
        }
    }


    /**
     * Replays again all events.
     */
    protected void replay() {
        // query all the mbeans and check they are all registered for the current container...
        if (mBeanServer != null) {
            Container container = getCurrentContainer();
            ObjectName objectName = createObjectName(CXF_API_ENDPOINT_MBEAN_NAME);
            if (objectName != null && container != null) {
                Set<ObjectInstance> instances = mBeanServer.queryMBeans(objectName, isCxfServiceEndpointQuery);
                for (ObjectInstance instance : instances) {
                    ObjectName oName = instance.getObjectName();
                    onMBeanEvent(container, oName);
                }
            }
            if (container == null) {
                LOGGER.warn("No container available!");
            }
        }
    }

    protected Container getCurrentContainer() {
        return fabricService.get().getCurrentContainer();
    }

    protected void onMBeanEvent(Container container, ObjectName oName) {
        try {
            if (isCxfServiceEndpointQuery.apply(oName)) {
                boolean validAddress = false;
                Object state = mBeanServer.getAttribute(oName, "State");
                Object addressValue = false;
                if (state instanceof String && state.toString().toUpperCase().startsWith("START")) {
                    addressValue = mBeanServer.getAttribute(oName, "Address");
                    if (addressValue instanceof String) {
                        validAddress = true;
                        String address = addressValue.toString();
                        LOGGER.info("Endpoint " + oName + " is alive at " + address);
                        registerApiEndpoint(container, oName, address);

                    }
                }
                if (!validAddress) {
                    //unregisterApiEndpoint(container, oName);
                    // TODO we may need to remove the registry entry?
                    LOGGER.warn("API endpoint " + oName + " has status " + state + " and does not have a valid address " + addressValue + " so we should unregister it");
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to process " + oName + ". " + e, e);
        }
    }

    public static ObjectName createObjectName(String name) {
        ObjectName objectName = null;
        try {
            objectName = new ObjectName(name);
        } catch (MalformedObjectNameException e) {
            LOGGER.error("Failed to create ObjectName for " + name + ". " + e, e);
        }
        return objectName;
    }

    protected void registerApiEndpoint(Container container, ObjectName oName, String address) {
        String path = getPath(container, oName, address);
        try {
            String url;
            String id = container.getId();
            if (isFullAddress(address)) {
                url = address;
            } else {
                String cxfBus = getCxfServletPath();
                url = "${zk:" + id + "/http}" + cxfBus + address;
            }

            // TODO lets assume that the location of apidocs is hard coded;
            // may be nice to discover from JMX?
            url += "/api-docs";

            String physicalUrl = ZooKeeperUtils.getSubstitutedData(curator.get(), url);

            if (validApiDocsEndpoint(physicalUrl)) {
                String json = "{\"id\":\"" + id + "\", \"services\":[\"" + url + "\"],\"container\":\"" + id + "\"}";
                LOGGER.info("Registered at " + path + " JSON: " + json);
                setData(curator.get(), path, json, CreateMode.EPHEMERAL);
            } else {
                LOGGER.info("Ignoring endpoint with no swagger API docs at " + path + " at " + physicalUrl);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to register API endpoint at {}.", path, e);
        }
    }

    protected void unregisterApiEndpoint(Container container, ObjectName oName) {
        String address = "";
        String path = getPath(container, oName, address);
        try {
            LOGGER.info("Unregister API at " + path);
            deleteSafe(curator.get(), path);
        } catch (Exception e) {
            LOGGER.error("Failed to unregister API endpoint at {}.", path, e);
        }
    }

    protected String getCxfServletPath() throws IOException, URISyntaxException {
        // TODO would be nice if there was an easy way to find this in JMX!
        String cxfBus = null;
        try {
            ConfigurationAdmin admin = getConfigAdmin();
            if (admin != null) {
                Configuration configuration = admin.getConfiguration("org.apache.cxf.osgi");
                if (configuration != null) {
                    Dictionary<String,Object> properties = configuration.getProperties();
                    if (properties != null) {
                        Object value = properties.get("org.apache.cxf.servlet.context");
                        if (value != null) {
                            cxfBus = value.toString();
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to lookup the cxf servlet path. " + e, e);
        }
        if (Strings.isEmpty(cxfBus)) {
            cxfBus = "/cxf";
            LOGGER.warn("Could not find the CXF servlet path in config admin so using a default value: " + cxfBus);
        } else {
            LOGGER.info("Found CXF servlet path from config admin: " + cxfBus);
        }
        return cxfBus;
    }

    protected boolean isFullAddress(String address) {
        return address.startsWith("http:") || address.startsWith("https:") || address.contains("://");
    }

    protected boolean validApiDocsEndpoint(String physicalUrl) {
        boolean answer = true;
        try {
            InputStream inputStream = new URL(physicalUrl).openStream();
            int b = inputStream.read();
            // lets assume its OK :)
            // TODO we could check for valid JSON or valid return code?
        } catch (Exception e) {
            answer = false;
        }
        return answer;

    }

    protected String getPath(Container container, ObjectName oName, String address) {
        String id = container.getId();

        String name = oName.getKeyProperty("port");
        if (Strings.isEmpty(name)) {
            name = "Unknown";
        }
        // trim quotes
        if (name.startsWith("\"") && name.endsWith("\"")) {
            name = name.substring(1, name.length() - 1);
        }
        String version = container.getVersion().toString();
        String endpointPath = address;
        if (isFullAddress(address)) {
            // lets remove the prefix "http://localhost:8181/cxf/"
            int idx = address.indexOf(":");
            if (idx > 0) {
                int length = address.length();
                // trim leading slashes after colon
                while (++idx < length && address.charAt(idx) == '/') ;
                idx = address.indexOf('/', idx);
                if (idx > 0) {
                    int nextIdx = address.indexOf('/', idx + 1);
                    if (nextIdx > 0) {
                        idx = nextIdx;
                    }
                }
                endpointPath = address.substring(idx);
            }
        }
        return ZkPath.API_REST_ENDPOINTS.getPath(name, version, id, endpointPath);
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }

    ConfigurationAdmin getConfigAdmin() {
        return configAdmin;
    }
}
