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

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.zookeeper.CreateMode;
import org.fusesource.fabric.maven.MavenProxy;
import org.fusesource.fabric.utils.SystemProperties;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.deleteSafe;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.create;

@Component(name = "org.fusesource.fabric.maven",
        description = "Fabric Maven Proxy Registration Handler",
        immediate = true)
@Service(ConfigurationListener.class)
public class MavenProxyRegistrationHandler implements ConfigurationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenProxyRegistrationHandler.class);

    private static final String LOCAL_REPOSITORY_PROPERTY = "localRepository";
    private static final String REMOTE_REPOSITORIES_PROPERTY = "remoteRepositories";
    private static final String APPEND_SYSTEM_REPOS_PROPERTY = "appendSystemRepos";
    private static final String UPDATE_POLICY_PROPERTY = "updatePolicy";
    private static final String CHECKSUM_POLICY_PROPERTY = "checksumPolicy";
    private static final String PROXY_PROTOCOL_PROPERTY = "proxy.protocol";
    private static final String PROXY_HOST_PROPERTY = "proxy.host";
    private static final String PROXY_USERNAME_PROPERTY = "proxy.username";
    private static final String PROXY_PASSWORD_PROPERTY = "proxy.password";
    private static final String NON_PROXY_HOSTS_PROPERTY = "proxy.nonProxyHosts";

    private static final String DEFAULT_LOCAL_REPOSITORY = System.getProperty("karaf.data") + File.separator + "maven" + File.separator + "proxy" + File.separator + "downloads";


    private final String name = System.getProperty(SystemProperties.KARAF_NAME);
    private final Map<String, Set<String>> registeredProxies = new HashMap<String, Set<String>>();

    @Reference(cardinality = org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY)
    private HttpService httpService;
    @Reference(cardinality = org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY)
    private ConfigurationAdmin configurationAdmin;
    @Reference(cardinality = org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY)
    private CuratorFramework curator;

    private String port;
    private String realm;
    private String role;

    private MavenDownloadProxyServlet mavenDownloadProxyServlet;
    private MavenUploadProxyServlet mavenUploadProxyServlet;

    public MavenProxyRegistrationHandler() {
        registeredProxies.put(MavenProxy.DOWNLOAD_TYPE, new HashSet<String>());
        registeredProxies.put(MavenProxy.UPLOAD_TYPE, new HashSet<String>());
    }


    @Activate
    public void init(Map<String, String> properties) throws IOException {
        String localRepository = readProperty(properties, LOCAL_REPOSITORY_PROPERTY, DEFAULT_LOCAL_REPOSITORY);
        String remoteRepositories = readProperty(properties, REMOTE_REPOSITORIES_PROPERTY, "");
        boolean appendSystemRepos = Boolean.parseBoolean(readProperty(properties, APPEND_SYSTEM_REPOS_PROPERTY, "false"));
        String updatePolicy = readProperty(properties, UPDATE_POLICY_PROPERTY, "always");
        String checksumPolicy = readProperty(properties, CHECKSUM_POLICY_PROPERTY, "fail");
        String proxyProtocol = readProperty(properties, PROXY_PROTOCOL_PROPERTY, "");
        String proxyHost = readProperty(properties, PROXY_HOST_PROPERTY, "");
        String proxyUsername = readProperty(properties, PROXY_USERNAME_PROPERTY, "");
        String proxyPassword = readProperty(properties, PROXY_PASSWORD_PROPERTY, "");
        String nonProxyHosts = readProperty(properties, NON_PROXY_HOSTS_PROPERTY, "");

        this.port = getPortFromConfig();
        this.mavenDownloadProxyServlet = new MavenDownloadProxyServlet(localRepository, remoteRepositories, appendSystemRepos, updatePolicy, checksumPolicy,proxyProtocol,proxyHost, getPortSafe(), proxyUsername, proxyPassword, nonProxyHosts);
        this.mavenDownloadProxyServlet.start();
        this.mavenUploadProxyServlet = new MavenUploadProxyServlet(localRepository, remoteRepositories, appendSystemRepos, updatePolicy, checksumPolicy,proxyProtocol,proxyHost, getPortSafe(), proxyUsername, proxyPassword, nonProxyHosts);
        this.mavenUploadProxyServlet.start();

        try {
            HttpContext base = httpService.createDefaultHttpContext();
            HttpContext secure = new MavenSecureHttpContext(base, realm, role);
            httpService.registerServlet("/maven/download", mavenDownloadProxyServlet, createParams("maven-download"), base);
            httpService.registerServlet("/maven/upload", mavenUploadProxyServlet, createParams("maven-upload"), secure);
        } catch (Throwable t) {
            LOGGER.warn("Failed to register fabric maven proxy servlets, due to:" + t.getMessage());
        }
        register(MavenProxy.DOWNLOAD_TYPE);
        register(MavenProxy.UPLOAD_TYPE);

    }

    @Deactivate
    public void destroy() {
        this.mavenDownloadProxyServlet.stop();
        this.mavenUploadProxyServlet.stop();

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
    }

    public void unbindHttpService(HttpService httpService) {
        this.httpService = null;
    }

    private Dictionary createParams(String name) {
        Dictionary d = new Hashtable();
        d.put("servlet-name", name);
        return d;
    }

    public void register(String type) {
        unregister(type);
        try {
            String mavenProxyUrl = "http://${zk:" + name + "/ip}:" + getPortSafe() + "/maven/" + type + "/";
            String parentPath = ZkPath.MAVEN_PROXY.getPath(type);
            String path = parentPath + "/p_";
            registeredProxies.get(type).add(create(curator, path, mavenProxyUrl, CreateMode.EPHEMERAL_SEQUENTIAL));
        } catch (Exception e) {
            LOGGER.warn("Failed to register maven proxy.");
        }
    }

    public void unregister(String type) {
        Set<String> proxyNodes = registeredProxies.get(type);
        if (proxyNodes != null) {
            try {
                for (String entry : registeredProxies.get(type)) {
                    deleteSafe(curator, entry);
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

    private String readProperty(Map<String, String> properties, String key, String defaultValue) {
        return properties != null && properties.containsKey(key) ? properties.get(key) : defaultValue;
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
