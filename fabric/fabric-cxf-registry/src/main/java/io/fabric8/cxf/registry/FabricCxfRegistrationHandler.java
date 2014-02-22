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
package io.fabric8.cxf.registry;

import io.fabric8.api.Version;
import io.fabric8.internal.JsonHelper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.zookeeper.CreateMode;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.utils.Strings;
import io.fabric8.zookeeper.ZkPath;
import io.fabric8.zookeeper.utils.ZooKeeperUtils;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
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
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Set;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.setData;

@ThreadSafe
@Component(name = "io.fabric8.cxf.registry", label = "Fabric8 CXF Registration Handler", immediate = true, metatype = false)
public final class FabricCxfRegistrationHandler extends AbstractComponent implements ConnectionStateListener {

    public static final String CXF_API_ENDPOINT_MBEAN_NAME = "org.apache.cxf:*";
    private static final ObjectName CXF_OBJECT_NAME =  objectNameFor(CXF_API_ENDPOINT_MBEAN_NAME);


    private static final Logger LOGGER = LoggerFactory.getLogger(FabricCxfRegistrationHandler.class);
    private static final Object[] EMPTY_PARAMS = {};
    private static final String[] EMPTY_SIGNATURE = {};

    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();
    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();
    @Reference(referenceInterface = ConfigurationAdmin.class, cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    private ConfigurationAdmin configAdmin;

    private Set<String> registeredZkPaths = new ConcurrentSkipListSet<String>();

    private NotificationListener listener = new NotificationListener() {
        @Override
        public void handleNotification(Notification notification, Object handback) {
            if (notification instanceof MBeanServerNotification) {
                MBeanServerNotification mBeanServerNotification = (MBeanServerNotification) notification;
                ObjectName mBeanName = mBeanServerNotification.getMBeanName();
                String type = mBeanServerNotification.getType();
                onMBeanEvent(getCurrentContainer(), mBeanName, type);
            }
        }
    };

    private NotificationFilter filter = new NotificationFilter() {
        @Override
        public boolean isNotificationEnabled(Notification notification) {
            return (notification instanceof MBeanServerNotification) &&
                    CXF_OBJECT_NAME.apply(((MBeanServerNotification) notification).getMBeanName());
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
    void activate() throws Exception {
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
    void deactivate() throws Exception {
        if (registeredListener && mBeanServer != null) {
            mBeanServer.removeNotificationListener(MBeanServerDelegate.DELEGATE_NAME, listener);
        }

        // lets remove all the previously generated paths
        List<String> paths = new ArrayList<String>(registeredZkPaths);
        for (String path : paths) {
            removeZkPath(path);
        }
        deactivateComponent();
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
                    String type = null;
                    onMBeanEvent(container, oName, type);
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

    protected void onMBeanEvent(Container container, ObjectName oName, String type) {
        try {
            if (isCxfServiceEndpointQuery.apply(oName)) {
                Object state = mBeanServer.getAttribute(oName, "State");
                String address = null;
                try {
                    Object addressValue = mBeanServer.getAttribute(oName, "Address");
                    if (addressValue instanceof String) {
                        address = addressValue.toString();
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to get address for endpoint " + oName + " type " + type + " has status " + state + ". " + e, e);
                }
                boolean started = state instanceof String && state.toString().toUpperCase().startsWith("START");
                boolean created = state instanceof String && state.toString().toUpperCase().startsWith("CREATE");

                if (address != null && (started || created)) {
                    LOGGER.info("Registering endpoint " + oName + " type " + type + " has status " + state + "at " + address);
                    registerApiEndpoint(container, oName, address, started);
                } else {
                    if (address == null) {
                        LOGGER.warn("Endpoint " + oName + " type " + type + " has status " + state + "but no address");
                    } else {
                        LOGGER.info("Unregistering endpoint " + oName + " type " + type + " has status " + state + "at " + address);
                    }
                    unregisterApiEndpoint(container, oName);
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

    protected void registerApiEndpoint(Container container, ObjectName oName, String address, boolean started) {
        String actualEndpointUrl = null;
        try {
            String url;
            String id = container.getId();
            if (isFullAddress(address)) {
                url = address;
            } else {
                String cxfBus = getCxfServletPath(oName);
                url = "${zk:" + id + "/http}" + cxfBus + address;
            }

            actualEndpointUrl = ZooKeeperUtils.getSubstitutedData(curator.get(), url);

            // lets assume these locations are hard coded
            // may be nice to discover from JMX one day
            String apiDocPath = "/api-docs";
            String wsdlPath = "?wsdl";
            String wadlPath = "?_wadl";

            Version version = container.getVersion();
            String versionId = version != null ? version.getId() : null;

            String json = "{\"id\":" + JsonHelper.jsonEncodeString(id)
                    + ", \"container\":" + JsonHelper.jsonEncodeString(id)
                    + ", \"version\":" + JsonHelper.jsonEncodeString(versionId)
                    + ", \"services\":[" + JsonHelper.jsonEncodeString(url) + "]" +
                      ", \"objectName\":" + JsonHelper.jsonEncodeString(oName.toString()) + "";
            boolean rest = false;
            if (booleanAttribute(oName, "isWADL")) {
                rest = true;
                json += ", \"wadl\":" + JsonHelper.jsonEncodeString(wadlPath);
            }
            if (booleanAttribute(oName, "isSwagger")) {
                rest = true;
                json += ", \"apidocs\":" + JsonHelper.jsonEncodeString(apiDocPath);
            }
            if (booleanAttribute(oName, "isWSDL")) {
                json += ", \"wsdl\":" + JsonHelper.jsonEncodeString(wsdlPath);
            }
            json += "}";

            String path = getPath(container, oName, address, rest);
            LOGGER.info("Registered CXF API at " + path + " JSON: " + json);
            if (!started && !rest) {
                LOGGER.warn("Since the CXF service isn't started, this could really be a REST endpoint rather than WSDL at " + path);
            }
            registeredZkPaths.add(path);
            ZooKeeperUtils.setData(curator.get(), path, json, CreateMode.EPHEMERAL);
        } catch (Exception e) {
            LOGGER.error("Failed to register API endpoint for {}.", actualEndpointUrl, e);
        }
    }

    protected boolean booleanAttribute(ObjectName oName, String name) {
        try {
            Object value = mBeanServer.invoke(oName, name, EMPTY_PARAMS, EMPTY_SIGNATURE);
            if (value != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Name " + oName + " has " + name + " value: " + value);
                }
                if (value instanceof Boolean) {
                    Boolean b = (Boolean) value;
                    return b.booleanValue();
                } else {
                    LOGGER.warn("Got value " + value + " of type " + value.getClass() + " for " + name + " on " + oName);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Name " + oName + " could not find attribute " + name + ". " + e, e);
        }
        return false;
    }

    protected void unregisterApiEndpoint(Container container, ObjectName oName) {
        String address = "";
        String path = null;
        try {
            // TODO there's no way to grok if its a REST or WS API so lets remove both just in case
            path = getPath(container, oName, address, true);
            removeZkPath(path);
            path = getPath(container, oName, address, false);
            removeZkPath(path);
        } catch (Exception e) {
            LOGGER.error("Failed to unregister API endpoint at {}.", path, e);
        }
    }

    protected void removeZkPath(String path) throws Exception {
        CuratorFramework curator = this.curator.get();
        if (curator != null && ZooKeeperUtils.exists(curator, path) != null) {
            LOGGER.info("Unregister API at " + path);
            ZooKeeperUtils.deleteSafe(curator, path);
        }
        registeredZkPaths.remove(path);
    }

    protected String getCxfServletPath(ObjectName oName) throws IOException, URISyntaxException {
        String cxfBus = null;
        // try find it in JMX
        try {
            Object value = mBeanServer.getAttribute(oName, "ServletContext");
            if (value instanceof String) {
                cxfBus = (String) value;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to get CxfServlet attribute on " + oName + ". " + e, e);
        }
        if (Strings.isNullOrBlank(cxfBus)) {
            // lets try find it in OSGi config admin
            try {
                ConfigurationAdmin admin = getConfigAdmin();
                if (admin != null) {
                    Configuration configuration = admin.getConfiguration("org.apache.cxf.osgi");
                    if (configuration != null) {
                        Dictionary<String, Object> properties = configuration.getProperties();
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
        }
        if (Strings.isNullOrBlank(cxfBus)) {
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

    protected String getPath(Container container, ObjectName oName, String address, boolean restApi) {
        String containerId = container.getId();

        String name = oName.getKeyProperty("port");
        if (Strings.isNullOrBlank(name)) {
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
        String fullName = name;
        if (Strings.isNotBlank(endpointPath)) {
            String prefix = endpointPath.startsWith("/") ? "" : "/";
            fullName += prefix + endpointPath;
        }
        if (restApi) {
            return ZkPath.API_REST_ENDPOINTS.getPath(fullName, version, containerId);
        } else {
            return ZkPath.API_WS_ENDPOINTS.getPath(fullName, version, containerId);
        }
    }

    private static ObjectName objectNameFor(String name) {
        try {
            return new ObjectName(name);
        } catch (MalformedObjectNameException e) {
            return null;
        }
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
