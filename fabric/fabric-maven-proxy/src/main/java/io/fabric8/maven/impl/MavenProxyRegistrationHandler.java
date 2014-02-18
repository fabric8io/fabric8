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

package io.fabric8.maven.impl;

import io.fabric8.api.jcip.GuardedBy;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.Configurer;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.maven.MavenProxy;
import io.fabric8.utils.SystemProperties;
import io.fabric8.zookeeper.ZkPath;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.felix.scr.annotations.*;
import org.apache.zookeeper.CreateMode;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.create;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.deleteSafe;

@ThreadSafe
@Component(name = "io.fabric8.maven", label = "Fabric8 Maven Proxy Registration Handler", policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = true)
@Service(ConnectionStateListener.class)
public final class MavenProxyRegistrationHandler extends AbstractComponent implements ConnectionStateListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenProxyRegistrationHandler.class);

    private static final String DEFAULT_ROLE = "admin";
    private static final String DEFAULT_REALM = "karaf";

    private static final String DEFAULT_LOCAL_REPOSITORY = System.getProperty("karaf.data") + File.separator + "maven" + File.separator + "proxy" + File.separator + "downloads";

    @Reference
    private Configurer configurer;
    @Reference(referenceInterface = HttpService.class)
    private final ValidatingReference<HttpService> httpService = new ValidatingReference<HttpService>();
    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();

    private final Map<String, Set<String>> registeredProxies;

    @GuardedBy("volatile") private volatile MavenDownloadProxyServlet mavenDownloadProxyServlet;
    @GuardedBy("volatile") private volatile MavenUploadProxyServlet mavenUploadProxyServlet;

    @GuardedBy("volatile")
    @Property(name = "realm", label = "Jaas Realm", description = "The Jaas Realm to use for uploads", value = DEFAULT_REALM)
    private volatile String realm;

    @GuardedBy("volatile")
    @Property(name = "role", label = "Jaas Role", description = "The Jaas Role to use for uploads", value = DEFAULT_ROLE)
    private volatile String role;

    @Property(name = "localRepository", label = "Local Repository", description = "The path to the local maven repository")
    private String localRepository = DEFAULT_LOCAL_REPOSITORY;
    @Property(name = "remoteRepositories", label = "Remote Repositories", cardinality = -1, description = "A comma spearated list to the remote repositories")
    private List<String> remoteRepositories;

    @Property(name = "appendSystemRepos", label = "Append System Repositories", description = "Flag to enable use of local maven repositories (defined in maven settings.xml)")
    private boolean appendSystemRepos;
    @Property(name = "updatePolicy", label = "Update Policy", description = "The update policy", value = "always", options = {
            @PropertyOption(name = "Always", value = "always"),
            @PropertyOption(name = "Daily", value = "daily"),
            @PropertyOption(name = "Never", value = "never")})
    private String updatePolicy;
    @Property(name = "checksumPolicy", label = "Checksum Policy", description = "The checksum policy", value = "warn", options = {
            @PropertyOption(name = "Ignore", value = "ignore"),
            @PropertyOption(name = "Fail", value = "fail"),
            @PropertyOption(name = "Warn", value = "warn")})
    private String checksumPolicy;
    @Property(name = "proxyProtocol", label = "Proxy Protocol", description = "The protocol of the Proxy")
    private String proxyProtocol;
    @Property(name = "proxyHost", label = "Proxy Host", description = "The host of the Proxy")
    private String proxyHost;
    @Property(name = "proxyPort", label = "Proxy Port", description = "The port of the Proxy", intValue = 3128)
    private int proxyPort;
    @Property(name = "proxyUsername", label = "Proxy Username", description = "The username of the Proxy")
    private String proxyUsername;
    @Property(name = "proxyPassword", label = "Proxy Password", description = "The password to the Proxy")
    private String proxyPassword;
    @Property(name = "nonProxyHosts", label = "Non Proxy Hosts", description = "Hosts that should be reached without using a Proxy")
    private String nonProxyHosts;
    @Property(name = "name", label = "Container Name", description = "The name of the container", value = "${karaf.name}")
    private String name;

    @GuardedBy("AtomicBoolean") private final AtomicBoolean connected = new AtomicBoolean(false);


    public MavenProxyRegistrationHandler() {
        Map<String, Set<String>> proxies = new HashMap<String, Set<String>>();
        proxies.put(MavenProxy.DOWNLOAD_TYPE, new HashSet<String>());
        proxies.put(MavenProxy.UPLOAD_TYPE, new HashSet<String>());
        registeredProxies = Collections.unmodifiableMap(proxies);
    }

    @Activate
    void init(Map<String, ?> configuration) throws Exception {
        configurer.configure(configuration, this);
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
    void destroy() {
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
            String mavenProxyUrl = "${zk:" + name + "/http}/maven/" + type + "/";
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
        switch (newState) {
            case CONNECTED:
            case RECONNECTED:
                connected.set(true);
                if (isValid()) {
                    register(MavenProxy.DOWNLOAD_TYPE);
                    register(MavenProxy.UPLOAD_TYPE);
                }
                break;
            default:
                connected.set(false);
                break;
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
