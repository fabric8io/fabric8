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
import org.fusesource.fabric.api.jcip.GuardedBy;
import org.fusesource.fabric.api.jcip.ThreadSafe;
import org.fusesource.fabric.api.scr.AbstractComponent;
import org.fusesource.fabric.api.scr.ValidatingReference;
import org.fusesource.fabric.maven.MavenProxy;
import org.fusesource.fabric.utils.SystemProperties;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.repository.RepositoryPolicy;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.deleteSafe;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.create;

@ThreadSafe
@Component(name = "org.fusesource.fabric.maven", description = "Fabric Maven Proxy Registration Handler", immediate = true)
@Service(ConnectionStateListener.class)
public final class MavenProxyRegistrationHandler extends AbstractComponent implements ConnectionStateListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenProxyRegistrationHandler.class);

    private static final String LOCAL_REPOSITORY_PROPERTY = "localRepository";
    private static final String REMOTE_REPOSITORIES_PROPERTY = "remoteRepositories";
    private static final String APPEND_SYSTEM_REPOS_PROPERTY = "appendSystemRepos";
    private static final String UPDATE_POLICY_PROPERTY = "updatePolicy";
    private static final String CHECKSUM_POLICY_PROPERTY = "checksumPolicy";
    private static final String PROXY_PROTOCOL_PROPERTY = "proxy.protocol";
    private static final String PROXY_HOST_PROPERTY = "proxy.host";
    private static final String PROXY_PORT_PROPERTY = "proxy.port";
    private static final String PROXY_USERNAME_PROPERTY = "proxy.username";
    private static final String PROXY_PASSWORD_PROPERTY = "proxy.password";
    private static final String NON_PROXY_HOSTS_PROPERTY = "proxy.nonProxyHosts";

    private static final String REQUIRED_ROLE = "role";
    private static final String REQUIRED_REALM = "realm";

    private static final String DEFAULT_ROLE = "admin";
    private static final String DEFAULT_REALM = "karaf";

    private static final String DEFAULT_LOCAL_REPOSITORY = System.getProperty("karaf.data") + File.separator + "maven" + File.separator + "proxy" + File.separator + "downloads";

    private static final String KARAF_NAME = System.getProperty(SystemProperties.KARAF_NAME);

    @Reference(referenceInterface = HttpService.class)
    private final ValidatingReference<HttpService> httpService = new ValidatingReference<HttpService>();
    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();

    private final Map<String, Set<String>> registeredProxies;

    @GuardedBy("volatile") private volatile MavenDownloadProxyServlet mavenDownloadProxyServlet;
    @GuardedBy("volatile") private volatile MavenUploadProxyServlet mavenUploadProxyServlet;
    @GuardedBy("volatile") private volatile String realm;
    @GuardedBy("volatile") private volatile String role;

    @GuardedBy("AtomicBoolean") private final AtomicBoolean connected = new AtomicBoolean(false);


    public MavenProxyRegistrationHandler() {
        Map<String, Set<String>> proxies = new HashMap<String, Set<String>>();
        proxies.put(MavenProxy.DOWNLOAD_TYPE, new HashSet<String>());
        proxies.put(MavenProxy.UPLOAD_TYPE, new HashSet<String>());
        registeredProxies = Collections.unmodifiableMap(proxies);
    }

    @Activate
    void activate(ComponentContext context, Map<String, ?> properties) throws IOException {
        String localRepository = readProperty(properties, LOCAL_REPOSITORY_PROPERTY, DEFAULT_LOCAL_REPOSITORY);
        String remoteRepositories = readProperty(properties, REMOTE_REPOSITORIES_PROPERTY, "");
        boolean appendSystemRepos = Boolean.parseBoolean(readProperty(properties, APPEND_SYSTEM_REPOS_PROPERTY, "false"));
        String updatePolicy = readProperty(properties, UPDATE_POLICY_PROPERTY, RepositoryPolicy.UPDATE_POLICY_ALWAYS);
        String checksumPolicy = readProperty(properties, CHECKSUM_POLICY_PROPERTY, RepositoryPolicy.CHECKSUM_POLICY_WARN);
        String proxyProtocol = readProperty(properties, PROXY_PROTOCOL_PROPERTY, "");
        String proxyHost = readProperty(properties, PROXY_HOST_PROPERTY, "");
        int proxyPort = Integer.parseInt(readProperty(properties, PROXY_PORT_PROPERTY, "8080"));
        String proxyUsername = readProperty(properties, PROXY_USERNAME_PROPERTY, "");
        String proxyPassword = readProperty(properties, PROXY_PASSWORD_PROPERTY, "");
        String nonProxyHosts = readProperty(properties, NON_PROXY_HOSTS_PROPERTY, "");

        this.role = readProperty(properties, REQUIRED_ROLE, DEFAULT_ROLE);
        this.realm = readProperty(properties, REQUIRED_REALM, DEFAULT_REALM);
        this.mavenDownloadProxyServlet = new MavenDownloadProxyServlet(localRepository, remoteRepositories, appendSystemRepos, updatePolicy, checksumPolicy, proxyProtocol, proxyHost, proxyPort, proxyUsername, proxyPassword, nonProxyHosts);
        this.mavenDownloadProxyServlet.start();
        this.mavenUploadProxyServlet = new MavenUploadProxyServlet(localRepository, remoteRepositories, appendSystemRepos, updatePolicy, checksumPolicy, proxyProtocol, proxyHost, proxyPort, proxyUsername, proxyPassword, nonProxyHosts);
        this.mavenUploadProxyServlet.start();
        try {
            HttpContext base = httpService.get().createDefaultHttpContext();
            HttpContext secure = new MavenSecureHttpContext(base, realm, role);
            httpService.get().registerServlet("/maven/download", mavenDownloadProxyServlet, createParams("maven-download"), base);
            httpService.get().registerServlet("/maven/upload", mavenUploadProxyServlet, createParams("maven-upload"), secure);
        } catch (Throwable t) {
            LOGGER.warn("Failed to register fabric maven proxy servlets, due to:" + t.getMessage());
        }
        activateComponent();
    }

    @Deactivate
    synchronized void deactivate() {
        deactivateComponent();
        if (mavenDownloadProxyServlet != null) {
            mavenDownloadProxyServlet.stop();
        }
        if (mavenUploadProxyServlet != null) {
            mavenUploadProxyServlet.stop();
        }

        try {
            httpService.get().unregister("/maven/download");
            httpService.get().unregister("/maven/upload");
        } catch (Exception ex) {
            LOGGER.warn("Http service returned error on servlet unregister. Possibly the service has already been stopped");
        }

        if (connected.get()) {
            unregister(MavenProxy.DOWNLOAD_TYPE);
            unregister(MavenProxy.UPLOAD_TYPE);
        }
    }

    private Dictionary<String, String> createParams(String name) {
        Dictionary<String, String> d = new Hashtable<String, String>();
        d.put("servlet-name", name);
        return d;
    }

    private void register(String type) {
        unregister(type);
        try {
            String mavenProxyUrl = "${zk:" + KARAF_NAME + "/http}/maven/" + type + "/";
            String parentPath = ZkPath.MAVEN_PROXY.getPath(type);
            String path = parentPath + "/p_";
            registeredProxies.get(type).add(create(curator.get(), path, mavenProxyUrl, CreateMode.EPHEMERAL_SEQUENTIAL));
        } catch (Exception e) {
            LOGGER.warn("Failed to register maven proxy.");
        }
    }

    private void unregister(String type) {
        Set<String> proxyNodes = registeredProxies.get(type);
        if (proxyNodes != null) {
            try {
                for (String entry : registeredProxies.get(type)) {
                    deleteSafe(curator.get(), entry);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to remove maven proxy from registry.");
            }
            registeredProxies.get(type).clear();
        }
    }

    private String readProperty(Map<String, ?> properties, String key, String defaultValue) {
        return properties != null && properties.containsKey(key) ? properties.get(key).toString() : defaultValue;
    }

    @Override
    public void stateChanged(CuratorFramework client, ConnectionState newState) {
        if (isValid()) {
            switch (newState) {
                case CONNECTED:
                case RECONNECTED:
                    connected.set(true);
                    register(MavenProxy.DOWNLOAD_TYPE);
                    register(MavenProxy.UPLOAD_TYPE);
                    break;
                default:
                    connected.set(false);
            }
        }
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }

    void bindHttpService(HttpService service) {
        this.httpService.bind(service);
    }

    void unbindHttpService(HttpService service) {
        this.httpService.unbind(service);
    }
}
