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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.eclipse.jgit.http.server.GitServlet;
import org.fusesource.fabric.git.GitNode;
import org.fusesource.fabric.git.GitService;
import org.fusesource.fabric.groups.Group;
import org.fusesource.fabric.groups.GroupListener;
import org.fusesource.fabric.groups.internal.ZooKeeperGroup;
import org.fusesource.fabric.utils.SystemProperties;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getSubstitutedData;

@Component(name = "org.fusesource.fabric.git.server",
           description = "Fabric Git HTTP Server Registration Handler",
           immediate = true)
public class GitHttpServerRegistrationHandler implements GroupListener<GitNode> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitHttpServerRegistrationHandler.class);
    private static final String GIT_PID = "org.fusesource.fabric.git";
    private static final String REALM_PROPERTY_NAME = "realm";
    private static final String ROLE_PROPERTY_NAME = "role";
    private static final String DEFAULT_REALM = "karaf";
    private static final String DEFAULT_ROLE = "admin";


    private final String name = System.getProperty(SystemProperties.KARAF_NAME);
    private final GitServlet gitServlet = new GitServlet();

    @Reference
    private HttpService httpService;
    @Reference
    private ConfigurationAdmin configurationAdmin;
    @Reference
    private CuratorFramework curator;
    @Reference
    private GitService gitService;

    private Group<GitNode> group;
    private String realm;
    private String role;
    private String gitRemoteUrl;

    public GitHttpServerRegistrationHandler() {
    }


    @Activate
    public void init(Map<String, String> properties) {
        this.realm =  properties != null && properties.containsKey(REALM_PROPERTY_NAME) ? properties.get(REALM_PROPERTY_NAME) : DEFAULT_REALM;
        this.role =  properties != null && properties.containsKey(ROLE_PROPERTY_NAME) ? properties.get(ROLE_PROPERTY_NAME) : DEFAULT_ROLE;

        group = new ZooKeeperGroup(curator, ZkPath.GIT.getPath(), GitNode.class);
        group.add(this);
        group.update(createState());
        group.start();

        gitServlet.addReceivePackFilter(new Filter() {
            @Override
            public void init(FilterConfig filterConfig) throws ServletException {
            }

            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                chain.doFilter(request, response);
                if (gitService != null) {
                    gitService.notifyReceivePacket();
                }
            }

            @Override
            public void destroy() {
            }
        });

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
        } catch (Exception e) {
            LOGGER.error("Error while registering git servlet", e);
        }
    }

    @Deactivate
    public void destroy() {
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
                gitRemoteUrl = getSubstitutedData(curator, url);
                if (group.isMaster()) {
                    updateConfigAdmin();
                }
            } catch (URISyntaxException e) {
                LOGGER.error("Could not resolve actual URL from " + url + ". Reason: " + e, e);
            }

        } catch (IllegalStateException e) {
            // Ignore
        }
    }


    private void updateConfigAdmin() {
        // lets register the current URL to ConfigAdmin
        try {
            Configuration conf = configurationAdmin.getConfiguration(GIT_PID);
            if (conf == null) {
                LOGGER.warn("No configuration for pid " + GIT_PID);
            } else {
                Dictionary<String, Object> properties = conf.getProperties();
                if (properties == null) {
                    properties = new Hashtable<String, Object>();
                }
                properties.put("fabric.git.url", gitRemoteUrl);
                conf.update(properties);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Setting pid " + GIT_PID + " config admin to: " + properties);
                }
            }
        } catch (Throwable e) {
            LOGGER.error("Could not load config admin for pid " + GIT_PID + ". Reason: " + e, e);
        }
    }

    private GitNode createState() {
        String fabricRepoUrl = "${zk:" + name + "/http}/git/fabric/";
        GitNode state = new GitNode();
        state.setId("fabric-repo");
        state.setUrl(fabricRepoUrl);
        state.setContainer(name);
        if (group != null && group.isMaster()) {
            state.setServices(new String[] { fabricRepoUrl });
        }
        return state;
    }
}
