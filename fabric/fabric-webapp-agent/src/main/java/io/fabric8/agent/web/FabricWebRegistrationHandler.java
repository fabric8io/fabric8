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
package io.fabric8.agent.web;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Set;
import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.QueryExp;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.zookeeper.CreateMode;
import io.fabric8.api.FabricService;
import io.fabric8.zookeeper.ZkPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.setData;

public class FabricWebRegistrationHandler implements ConnectionStateListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(FabricWebRegistrationHandler.class);

    private FabricService fabricService;
    private CuratorFramework curator;
    private NotificationListener listener;
    private NotificationFilter filter;
    private MBeanServer mBeanServer;
    private int port = -1;

    // TODO load from system properties
    String containerId = "tomcat1";
    String host = "localhost";


    public void init() throws Exception {
        LOGGER.info("Initialising " + this + " with fabricService: " + fabricService + " and curator: "
                + curator);

        if (mBeanServer == null) {
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
        }

        if (mBeanServer != null) {
            Object handback = null;
            listener = getNotificationListener();
            filter = getNotificationFilter();

            mBeanServer
                    .addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, listener, filter, handback);
        }
        findWebAppMBeans();
    }

    public void destroy() throws Exception {
        if (mBeanServer != null) {
            if (listener != null) {
                mBeanServer.removeNotificationListener(MBeanServerDelegate.DELEGATE_NAME, listener);
            }
        }
    }

    /**
     * Lets query JMX for any MBeans to define web apps in containers like Tomcat, Jetty, JBoss
     */
    protected void findWebAppMBeans() {
        try {
            if (port < 0) {
                // lets try find the port
                Set<ObjectName> objectNames = findObjectNames(null, "Catalina:type=Connector,*",
                        "Tomcat:type=Connector,*");

                for (ObjectName objectName : objectNames) {
                    Object protocol = mBeanServer.getAttribute(objectName, "protocol");
                    if (protocol != null && protocol instanceof String && protocol.toString().toLowerCase()
                            .startsWith("http/")) {
                        Object portValue = mBeanServer.getAttribute(objectName, "port");
                        if (portValue instanceof Number) {
                            port = ((Number)portValue).intValue();
                            if (port > 0) {
                                break;
                            }
                        }
                    }
                }
            }

            if (port > 0 && curator != null) {
                Set<ObjectName> objectNames = findObjectNames(null, "Catalina:j2eeType=WebModule,*", "Tomcat:j2eeType=WebModule,*");
                for (ObjectName objectName : objectNames) {
                    Object pathValue = mBeanServer.getAttribute(objectName, "path");
                    if (pathValue != null && pathValue instanceof String) {
                        String path = pathValue.toString();

                        String url = "http://" + host + ":" + port + path;
                        String name = path;
                        while (name.startsWith("/")) {
                            name = name.substring(1);
                        }

                        // lets try figure out the group / version...
                        String version = "unknown";
                        String[] versionSplit = name.split("-\\d\\.\\d");
                        if (versionSplit.length < 2) {
                            versionSplit = name.split("-SNAPSHOT");
                        }
                        if (versionSplit.length > 1) {
                            String justName =versionSplit[0];
                            version = name.substring(justName.length() + 1);
                            name = justName;
                        }
                        LOGGER.info("Found name " + name + " version " + version + " url " + url + " from path " + path);
                        if (name.length() > 0) {
                            registerWebapp(name, version, url);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            LOGGER.warn("Failed to poll for web app mbeans " + e, e);
        }
    }

    protected Set<ObjectName> findObjectNames(QueryExp queryExp, String... objectNames)
            throws MalformedObjectNameException {
        Set<ObjectName> answer = new HashSet<ObjectName>();
        for (String objectName : objectNames) {
            Set<ObjectName> set = mBeanServer.queryNames(new ObjectName(objectName), queryExp);
            if (set != null) {
                answer.addAll(set);
            }
        }
        return answer;
    }

    protected NotificationListener getNotificationListener() {
        return new NotificationListener() {
            @Override
            public void handleNotification(Notification notification, Object handback) {
                findWebAppMBeans();
            }
        };
    }

    protected NotificationFilter getNotificationFilter() {
        return new NotificationFilter() {
            @Override
            public boolean isNotificationEnabled(Notification notification) {
                return true;
            }
        };
    }


    /**
     * Registers a webapp to the registry.
     */
    protected void registerWebapp(String name, String version, String url) {
        String json = "{\"containerId\":\"" + containerId + "\", \"services\":[\"" + url
                + "\"],\"container\":\"" + containerId + "\"}";
        try {
            String zkPath = ZkPath.WEBAPPS_CONTAINER.getPath(name, version, containerId);
            setData(curator, zkPath, json, CreateMode.EPHEMERAL);
        } catch (Exception e) {
            LOGGER.error("Failed to register webapp {}.", json, e);
        }
    }


    @Override
    public void stateChanged(CuratorFramework client, ConnectionState newState) {
        switch (newState) {
        case CONNECTED:
        case RECONNECTED:
            this.curator = client;
            onConnected();
            break;
        default:
            onDisconnected();
            this.curator = null;
            break;
        }
    }

    public void onConnected() {
    }

    public void onDisconnected() {
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }
}
