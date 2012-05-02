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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.fusesource.fabric.maven.MavenProxy;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenProxyRegistrationHandler implements LifecycleListener, ConfigurationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenProxyRegistrationHandler.class);

    private String port;
    private final Map<String, Set<String>> registeredProxies = new HashMap<String, Set<String>>();
    private IZKClient zookeeper = null;
    private String name = System.getProperty("karaf.name");

    private HttpService httpService;
    private SecureHttpContext secureHttpContext;
    private MavenDownloadProxyServlet mavenDownloadProxyServlet;
    private MavenUploadProxyServlet mavenUploadProxyServlet;

    private ConfigurationAdmin configurationAdmin;

    public MavenProxyRegistrationHandler() {
        registeredProxies.put(MavenProxy.DOWNLOAD_TYPE, new HashSet<String>());
        registeredProxies.put(MavenProxy.UPLOAD_TYPE, new HashSet<String>());
    }

    public void init() throws ServletException, NamespaceException {
        this.port = getPortFromConfig();
        httpService.registerServlet("/maven/download", mavenDownloadProxyServlet, null, null);
        httpService.registerServlet("/maven/upload", mavenUploadProxyServlet, null, secureHttpContext);
    }

    public void destroy() {
        unregister(MavenProxy.DOWNLOAD_TYPE);
        unregister(MavenProxy.UPLOAD_TYPE);

        try {
            httpService.unregister("/maven/download");
            httpService.unregister("/maven/upload");
        } catch (Exception ex) {
            LOGGER.warn("Http service returned error on servlet unregister. Possibly the service has already been stopped");
        }
    }


    @Override
    public void onConnected() {
        register(MavenProxy.DOWNLOAD_TYPE);
        register(MavenProxy.UPLOAD_TYPE);
    }


    @Override
    public void onDisconnected() {
        unregister(MavenProxy.DOWNLOAD_TYPE);
        unregister(MavenProxy.UPLOAD_TYPE);
    }

    public void register(String type) {
        unregister(type);
        String mavenProxyUrl = "http://${zk:" + name + "/ip}:" + getPortSafe() + "/maven/" + type + "/";
        String parentPath = ZkPath.MAVEN_PROXY.getPath(type);
        String path = parentPath + "/p_";
        if (zookeeper.isConnected()) {
            try {
                if (zookeeper.exists(parentPath) == null) {
                    zookeeper.createWithParents(parentPath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }
                if (zookeeper.exists(path) == null) {
                    registeredProxies.get(type).add(zookeeper.create(path, mavenProxyUrl.getBytes("UTF-8"), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL));
                }
            } catch (Exception e) {
                LOGGER.error("Failed to register maven proxy.", e);
            }
        }
    }

    public void unregister(String type) {
        try {
            Set<String> proxyNodes = registeredProxies.get(type);
            if (proxyNodes != null) {
                for (String entry : registeredProxies.get(type)) {
                    if (zookeeper.isConnected()) {
                        if (zookeeper.exists(entry) != null) {
                            zookeeper.deleteWithChildren(entry);
                        }
                    }
                }
                registeredProxies.get(type).clear();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to remove maven proxy from registry.", e);
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

    public IZKClient getZookeeper() {
        return zookeeper;
    }

    public void setZookeeper(IZKClient zookeeper) {
        this.zookeeper = zookeeper;
    }

    public String getPort() {
        return port;
    }

    public HttpService getHttpService() {
        return httpService;
    }

    public void setHttpService(HttpService httpService) {
        this.httpService = httpService;
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

    public SecureHttpContext getSecureHttpContext() {
        return secureHttpContext;
    }

    public void setSecureHttpContext(SecureHttpContext secureHttpContext) {
        this.secureHttpContext = secureHttpContext;
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }
}
