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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenProxyRegistrationHandler implements LifecycleListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenProxyRegistrationHandler.class);

    private int port;
    private final Map<String, String> node = new HashMap<String, String>();
    private IZKClient zookeeper = null;
    private String name = System.getProperty("karaf.name");

    private HttpService httpService;
    private SecureHttpContext secureHttpContext;
    private MavenDownloadProxyServlet mavenDownloadProxyServlet;
    private MavenUploadProxyServlet mavenUploadProxyServlet;

    public void init() throws ServletException, NamespaceException {
        httpService.registerServlet("/maven/download", mavenDownloadProxyServlet, null, null);
        httpService.registerServlet("/maven/upload", mavenUploadProxyServlet, null, secureHttpContext);
    }

    public void destroy() {
        httpService.unregister("/maven/download");
        unregister("download");
        httpService.unregister("/maven/upload");
        unregister("upload");
    }


    @Override
    public void onConnected() {
        register("download");
        register("upload");
    }


    @Override
    public void onDisconnected() {
        unregister("download");
        unregister("upload");
    }

    public void register(String type) {
        String mavenProxyUrl = "http://${zk:" + name + "/ip}:" + port + "/maven/"+type+"/";
        String parentPath = ZkPath.MAVEN_PROXY.getPath(type);
        String path = parentPath + "/p_";
        if (zookeeper.isConnected()) {
            try {
                if (zookeeper.exists(parentPath) == null) {
                    zookeeper.createWithParents(parentPath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }

                if (zookeeper.exists(path) == null || (node.get(type) != null && zookeeper.exists(node.get(type)) == null)) {
                    node.put(type, zookeeper.create(path, mavenProxyUrl.getBytes("UTF-8"), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL));
                }
            } catch (Exception e) {
                LOGGER.error("Failed to register maven proxy.", e);
            }
        }
    }

    public void unregister(String type) {
        try {
            if (node != null) {
                if (zookeeper.isConnected()) {
                    zookeeper.delete(node.get(type));
                }
                node.remove(type);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to remove maven proxy from registry.", e);
        }
    }

    public void update() {
        unregister("download");
        unregister("upload");
        register("upload");
        register("upload");
    }

    private String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException("Unable to get address", e);
        }
    }

    public IZKClient getZookeeper() {
        return zookeeper;
    }

    public void setZookeeper(IZKClient zookeeper) {
        this.zookeeper = zookeeper;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
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
}
