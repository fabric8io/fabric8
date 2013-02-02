/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.fusesource.fabric.maven.impl;

import org.apache.zookeeper.CreateMode;
import org.fusesource.fabric.maven.MavenProxy;
import org.fusesource.fabric.utils.SystemProperties;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MavenProxyRegistrationHandler implements LifecycleListener, ConfigurationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenProxyRegistrationHandler.class);

    private String port;
    private final Map<String, Set<String>> registeredProxies = new HashMap<String, Set<String>>();
    private IZKClient zookeeper = null;
    private boolean connected = false;
    private String name = System.getProperty(SystemProperties.KARAF_NAME);

    private String realm;
    private String role;

    private HttpService httpService;
    private MavenDownloadProxyServlet mavenDownloadProxyServlet;
    private MavenUploadProxyServlet mavenUploadProxyServlet;

    private ConfigurationAdmin configurationAdmin;

    public MavenProxyRegistrationHandler() {
        registeredProxies.put(MavenProxy.DOWNLOAD_TYPE, new HashSet<String>());
        registeredProxies.put(MavenProxy.UPLOAD_TYPE, new HashSet<String>());
    }


    public void init() {
    }

    public void destroy() {
        unregister(MavenProxy.DOWNLOAD_TYPE);
        unregister(MavenProxy.UPLOAD_TYPE);
        try {
            if (httpService != null) {
                httpService.unregister("/maven/download");
                httpService.unregister("/maven/upload");
            }
        } catch (Exception ex) {
            LOGGER.warn("Http service returned error on servlet unregister. Possibly the service has already been stopped");
        }
    }

    public void bindHttpService(HttpService httpService) {
        this.httpService = httpService;
        this.port = getPortFromConfig();

        if (httpService != null && mavenDownloadProxyServlet != null && mavenUploadProxyServlet != null) {

            try {
                HttpContext base = httpService.createDefaultHttpContext();
                HttpContext secure = new SecureHttpContext(base, realm, role);
                httpService.registerServlet("/maven/download", mavenDownloadProxyServlet, null, base);
                httpService.registerServlet("/maven/upload", mavenUploadProxyServlet, null, secure);
            } catch (Throwable t) {
                LOGGER.warn("Failed to register fabric maven proxy servlets, due to:" + t.getMessage());
            }

            register(MavenProxy.DOWNLOAD_TYPE);
            register(MavenProxy.UPLOAD_TYPE);
        }
    }

    public void unbindHttpService(HttpService httpService) {
        unregister(MavenProxy.DOWNLOAD_TYPE);
        unregister(MavenProxy.UPLOAD_TYPE);
        this.httpService = null;
    }

    public void bindZooKeeper(IZKClient zookeeper) {
        this.zookeeper = zookeeper;
        if (zookeeper != null) {
            zookeeper.registerListener(this);
        }
    }

    public void unbindZooKeeper(IZKClient zookeeper) {
        if (zookeeper != null) {
            zookeeper.removeListener(this);
        }
        this.connected = false;
        this.zookeeper = null;
    }


    @Override
    public void onConnected() {
        connected = true;
        register(MavenProxy.DOWNLOAD_TYPE);
        register(MavenProxy.UPLOAD_TYPE);
    }


    @Override
    public void onDisconnected() {
        connected = false;
    }

    public void register(String type) {
        unregister(type);
        try {
            if (connected && httpService != null) {
                String mavenProxyUrl = "http://${zk:" + name + "/ip}:" + getPortSafe() + "/maven/" + type + "/";
                String parentPath = ZkPath.MAVEN_PROXY.getPath(type);
                String path = parentPath + "/p_";
                if (zookeeper.exists(parentPath) == null) {
                    zookeeper.createWithParents(parentPath, CreateMode.PERSISTENT);
                }
                if (zookeeper.exists(path) == null) {
                    registeredProxies.get(type).add(zookeeper.create(path, mavenProxyUrl, CreateMode.EPHEMERAL_SEQUENTIAL));
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to register maven proxy.");
        }
    }

    public void unregister(String type) {
        Set<String> proxyNodes = registeredProxies.get(type);
        if (proxyNodes != null) {
            try {
                if (connected) {
                    for (String entry : registeredProxies.get(type)) {
                        if (zookeeper.exists(entry) != null) {
                            zookeeper.deleteWithChildren(entry);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to remove maven proxy from registry.");
            }
            registeredProxies.get(type).clear();
        }
    }

    @Override
    public void configurationEvent(ConfigurationEvent event) {
        if (event.getPid().equals("org.ops4j.pax.web") && event.getType() == ConfigurationEvent.CM_UPDATED) {
            this.port = getPortFromConfig();
            register(MavenProxy.DOWNLOAD_TYPE);
            register(MavenProxy.UPLOAD_TYPE);
        }
    }

    public String getPortFromConfig() {
        String port = "8181";
        try {
            Configuration[] configurations = configurationAdmin.listConfigurations("(" + Constants.SERVICE_PID + "=org.ops4j.pax.web)");
            if (configurations != null && configurations.length > 0) {
                Configuration configuration = configurations[0];
                Dictionary properties = configuration.getProperties();
                if (properties != null && properties.get("org.osgi.service.http.port") != null) {
                    port = String.valueOf(properties.get("org.osgi.service.http.port"));
                }
            }
        } catch (Exception e) {
            //noop
        }
        return port;
    }

    private int getPortSafe() {
        int port = 8181;
        try {
            port = Integer.parseInt(getPort());
        } catch (NumberFormatException ex) {
            //noop
        }
        return port;
    }

    public String getPort() {
        return port;
    }

    public MavenDownloadProxyServlet getMavenDownloadProxyServlet() {
        return mavenDownloadProxyServlet;
    }

    public void setMavenDownloadProxyServlet(MavenDownloadProxyServlet mavenDownloadProxyServlet) {
        this.mavenDownloadProxyServlet = mavenDownloadProxyServlet;
    }

    public MavenUploadProxyServlet getMavenUploadProxyServlet() {
        return mavenUploadProxyServlet;
    }

    public void setMavenUploadProxyServlet(MavenUploadProxyServlet mavenUploadProxyServlet) {
        this.mavenUploadProxyServlet = mavenUploadProxyServlet;
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
