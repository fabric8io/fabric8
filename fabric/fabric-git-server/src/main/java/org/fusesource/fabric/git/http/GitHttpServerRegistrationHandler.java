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

import org.eclipse.jgit.http.server.GitServlet;
import org.fusesource.fabric.git.GitNode;
import org.fusesource.fabric.groups.ChangeListener;
import org.fusesource.fabric.groups.ClusteredSingleton;
import org.fusesource.fabric.groups.Group;
import org.fusesource.fabric.groups.ZooKeeperGroupFactory;
import org.fusesource.fabric.utils.SystemProperties;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.Hashtable;

public class GitHttpServerRegistrationHandler implements LifecycleListener, ConfigurationListener, ChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitHttpServerRegistrationHandler.class);

    private final ClusteredSingleton<GitNode> singleton = new ClusteredSingleton<GitNode>(GitNode.class);
    private IZKClient zookeeper = null;
    private boolean connected = false;
    private final String name = System.getProperty(SystemProperties.KARAF_NAME);

    private Group group;

    private HttpService httpService;
    private GitServlet gitServlet;
    private String port;
    private String realm;
    private String role;

    private ConfigurationAdmin configurationAdmin;

    public GitHttpServerRegistrationHandler() {
        singleton.add(this);
    }


    public void init() {
    }

    public void destroy() {
        onDisconnected();
        unbindHttpService(null);
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
                singleton.join(createState());
            }
        } catch (Exception e) {
            LOGGER.error("Error while registering git servlet", e);
        }
    }

    public synchronized void unbindHttpService(HttpService oldService) {
        try {
            if (httpService != null) {
                if (connected) {
                    singleton.leave();
                }
                httpService.unregister("/git");
            }
        } catch (Exception ex) {
            LOGGER.warn("Http service returned error on servlet unregister. Possibly the service has already been stopped");
        }
        this.httpService = null;
    }


    @Override
    public synchronized void onConnected() {
        connected = true;
        group = ZooKeeperGroupFactory.create(zookeeper, ZkPath.GIT.getPath());
        singleton.start(group);

        if (httpService != null) {
            singleton.join(createState());
        }
    }

    @Override
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
    public void connected() {
        changed();
    }

    @Override
    public void disconnected() {
        changed();
    }

    @Override
    public void changed() {
        if (singleton.isMaster()) {
            LOGGER.info("Git repo is the master");
        } else {
            LOGGER.info("Git repo is not the master");
        }
        try {
            GitNode state = createState();
            singleton.update(state);

            if (singleton.isMaster()) {
                // lets register the current URL to ConfigAdmin
                String pid = "org.fusesource.fabric.git";
                String url = state.getUrl();
                try {
                    String actualUrl = ZooKeeperUtils.getSubstitutedData(zookeeper, url);
                    Configuration conf = configurationAdmin.getConfiguration(pid);
                    if (conf == null) {
                        LOGGER.warn("No configuration for pid " + pid);
                    } else {
                        Dictionary<String, Object> properties = conf.getProperties();
                        if (properties == null) {
                            properties = new Hashtable<String, Object>();
                        }
                        properties.put("fabric-git-url", actualUrl);
                        conf.update(properties);
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Setting pid " + pid + " config admin to: " + properties);
                        }
                    }
                } catch (URISyntaxException e) {
                    LOGGER.error("Could not resolve actual URL from " + url + ". Reason: " + e, e);
                } catch (Throwable e) {
                    LOGGER.error("Could not load config admin for pid " + pid + ". Reason: " + e, e);
                }
            }
        } catch (IllegalStateException e) {
            // Ignore
        }
    }

    @Override
    public void configurationEvent(ConfigurationEvent event) {
        if (event.getPid().equals("org.ops4j.pax.web") && event.getType() == ConfigurationEvent.CM_UPDATED) {
            this.port = getPortFromConfig();
            if (httpService != null && connected) {
                singleton.update(createState());
            }
        }
    }

    GitNode createState() {
        String fabricRepoUrl = "http://${zk:" + name + "/ip}:" + getPortSafe() + "/git/fabric/";
        GitNode state = new GitNode();
        state.setId("fabric-repo");
        state.setUrl(fabricRepoUrl);
        state.setAgent(name);
        if (singleton.isMaster()) {
            state.setServices(new String[] { "git" });
        }
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

    public IZKClient getZookeeper() {
        return zookeeper;
    }

    public void setZookeeper(IZKClient zookeeper) {
        this.zookeeper = zookeeper;
    }
}
