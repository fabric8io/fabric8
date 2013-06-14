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

package org.fusesource.fabric.git.http;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.eclipse.jgit.http.server.GitServlet;
import org.fusesource.fabric.git.GitNode;
import org.fusesource.fabric.groups.GroupListener;
import org.fusesource.fabric.groups.Group;
import org.fusesource.fabric.groups.internal.ZooKeeperGroup;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getSubstitutedData;

public class GitHttpServerRegistrationHandler implements ConnectionStateListener, ConfigurationListener, GroupListener<GitNode> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitHttpServerRegistrationHandler.class);

    private CuratorFramework curator = null;
    private boolean connected = false;
    private final String name = System.getProperty(SystemProperties.KARAF_NAME);

    private Group<GitNode> group;

    private HttpService httpService;
    private GitServlet gitServlet;
    private String port;
    private String realm;
    private String role;

    private ConfigurationAdmin configurationAdmin;

    private List<GitListener> listeners = new ArrayList<GitListener>();
    private String gitRemoteUrl;

    public GitHttpServerRegistrationHandler() {
    }


    public void init() {
    }

    public void destroy() {
        onDisconnected();
        unbindHttpService(null);
    }

    public synchronized void addGitListener(GitListener listener) {
        listeners.add(listener);
    }

    public synchronized void removeGitListener(GitListener listener) {
        listeners.remove(listener);
    }

    public synchronized void bindHttpService(HttpService httpService) {
        unbindHttpService(null);
        this.httpService = httpService;
        this.port = getPortFromConfig();
        try {
            HttpContext base = httpService.createDefaultHttpContext();
            HttpContext secure = new SecureHttpContext(base, realm, role);
            String basePath = System.getProperty("karaf.data") + File.separator + "git" + File.separator;
            String fabricGitPath = basePath + "fabric";
            File fabricRoot = new File(fabricGitPath);
            if (!fabricRoot.exists() && !fabricRoot.mkdirs()) {
                throw new FileNotFoundException("Could not found git root:" + basePath);
            }
            Dictionary<String, Object> initParams = new Hashtable<String, Object>();
            initParams.put("base-path", basePath);
            initParams.put("repository-root", basePath);
            initParams.put("export-all", "true");
            httpService.registerServlet("/git", gitServlet, initParams, secure);

            if (connected) {
                group.update(createState());
            }
        } catch (Exception e) {
            LOGGER.error("Error while registering git servlet", e);
        }
    }

    public synchronized void unbindHttpService(HttpService oldService) {
        try {
            if (httpService != null) {
                if (connected) {
                    try {
                        group.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
                httpService.unregister("/git");
            }
        } catch (Exception ex) {
            LOGGER.warn("Http service returned error on servlet unregister. Possibly the service has already been stopped");
        }
        this.httpService = null;
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
        }
    }

    public synchronized void onConnected() {
        connected = true;
        group = new ZooKeeperGroup(curator, ZkPath.GIT.getPath(), GitNode.class);
        group.add(this);
        if (httpService != null) {
            group.update(createState());
        }
        group.start();
    }

    public synchronized void onDisconnected() {
        connected = false;
        try {
            if (group != null) {
                group.close();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to remove git server from registry.", e);
        }
    }

    @Override
    public void groupEvent(Group<GitNode> group, GroupEvent event) {
        if (group.isMaster()) {
            LOGGER.info("Git repo is the master");
        } else {
            LOGGER.info("Git repo is not the master");
        }
        try {
            GitNode state = createState();
            group.update(state);

            String url = state.getUrl();
            try {
                String actualUrl = getSubstitutedData(curator, url);
                if (actualUrl != null && (this.gitRemoteUrl == null || !this.gitRemoteUrl.equals(actualUrl))) {
                    // lets notify listeners
                    this.gitRemoteUrl = actualUrl;
                    fireGitRemoteUrlChanged(actualUrl);
                }

                if (group.isMaster()) {
                    // lets register the current URL to ConfigAdmin
                    String pid = "org.fusesource.fabric.git";
                    try {
                        Configuration conf = configurationAdmin.getConfiguration(pid);
                        if (conf == null) {
                            LOGGER.warn("No configuration for pid " + pid);
                        } else {
                            Dictionary<String, Object> properties = conf.getProperties();
                            if (properties == null) {
                                properties = new Hashtable<String, Object>();
                            }
                            properties.put("fabric.git.url", actualUrl);
                            conf.update(properties);
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Setting pid " + pid + " config admin to: " + properties);
                            }
                        }
                    } catch (Throwable e) {
                        LOGGER.error("Could not load config admin for pid " + pid + ". Reason: " + e, e);
                    }
                }
            } catch (URISyntaxException e) {
                LOGGER.error("Could not resolve actual URL from " + url + ". Reason: " + e, e);
            }

        } catch (IllegalStateException e) {
            // Ignore
        }
    }

    protected void fireGitRemoteUrlChanged(String remoteUrl) {
        for (GitListener listener : listeners) {
            listener.onRemoteUrlChanged(remoteUrl);
        }
    }

    public String getGitRemoteUrl() {
        return gitRemoteUrl;
    }

    @Override
    public void configurationEvent(ConfigurationEvent event) {
        if (event.getPid().equals("org.ops4j.pax.web") && event.getType() == ConfigurationEvent.CM_UPDATED) {
            this.port = getPortFromConfig();
            if (httpService != null && connected) {
                group.update(createState());
            }
        }
    }

    GitNode createState() {
        String fabricRepoUrl = "http://${zk:" + name + "/ip}:" + getPortSafe() + "/git/fabric/";
        GitNode state = new GitNode();
        state.setId("fabric-repo");
        state.setUrl(fabricRepoUrl);
        state.setContainer(name);
        return state;
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

    public GitServlet getGitServlet() {
        return gitServlet;
    }

    public void setGitServlet(GitServlet gitServlet) {
        this.gitServlet = gitServlet;
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }


    public CuratorFramework getCurator() {
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }
}
