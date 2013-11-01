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
import org.apache.karaf.jaas.config.JaasRealm;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.http.server.GitServlet;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.jcip.GuardedBy;
import org.fusesource.fabric.api.jcip.ThreadSafe;
import org.fusesource.fabric.api.scr.AbstractComponent;
import org.fusesource.fabric.api.scr.ValidatingReference;
import org.fusesource.fabric.git.GitNode;
import org.fusesource.fabric.git.GitService;
import org.fusesource.fabric.groups.Group;
import org.fusesource.fabric.groups.GroupListener;
import org.fusesource.fabric.groups.internal.ZooKeeperGroup;
import org.fusesource.fabric.utils.SystemProperties;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getSubstitutedData;

@ThreadSafe
@Component(name = "org.fusesource.fabric.git.server", description = "Fabric Git HTTP Server Registration Handler", immediate = true)
public final class GitHttpServerRegistrationHandler extends AbstractComponent implements GroupListener<GitNode> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitHttpServerRegistrationHandler.class);

    private static final String GIT_PID = "org.fusesource.fabric.git";
    private static final String REALM_PROPERTY_NAME = "realm";
    private static final String ROLE_PROPERTY_NAME = "role";
    private static final String DEFAULT_REALM = "karaf";
    private static final String DEFAULT_ROLE = "admin";

    private static final String KARAF_NAME = System.getProperty(SystemProperties.KARAF_NAME);
    private final GitServlet gitServlet = new GitServlet();

    @Reference(referenceInterface = HttpService.class)
    private final ValidatingReference<HttpService> httpService = new ValidatingReference<HttpService>();
    @Reference(referenceInterface = ConfigurationAdmin.class)
    private final ValidatingReference<ConfigurationAdmin> configAdmin = new ValidatingReference<ConfigurationAdmin>();
    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();
    @Reference(referenceInterface = GitService.class)
    private final ValidatingReference<GitService> gitService = new ValidatingReference<GitService>();

    //Reference not used, but it expresses the dependency on a fully initialized fabric.
    @Reference
    private FabricService fabricService;

    //Reference not used, but it expresses the dependency of the git server on a jaas realm that supports container tokens.
    @Reference(target = "(supports.container.tokens=true)")
    private JaasRealm fabricJaasRelm;


    @GuardedBy("volatile") private volatile Group<GitNode> group;
    @GuardedBy("volatile") private volatile String gitRemoteUrl;

    private String realm;
    private String role;

    @Activate
    void activate(ComponentContext context, Map<String, ?> properties) {
        realm =  properties != null && properties.containsKey(REALM_PROPERTY_NAME) ? (String)properties.get(REALM_PROPERTY_NAME) : DEFAULT_REALM;
        role =  properties != null && properties.containsKey(ROLE_PROPERTY_NAME) ? (String)properties.get(ROLE_PROPERTY_NAME) : DEFAULT_ROLE;

        registerServlet();
        group = new ZooKeeperGroup(curator.get(), ZkPath.GIT.getPath(), GitNode.class);
        group.add(this);
        group.update(createState());
        group.start();


        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        try {
            if (group != null) {
                group.close();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to remove git server from registry.", e);
        }
        unregisterServlet();
    }



    @Override
    public void groupEvent(Group<GitNode> group, GroupEvent event) {
        if (isValid()) {
            switch (event) {
                case CONNECTED:
                case CHANGED:
                    updateMasterUrl(group);
                    break;
            }
        }
    }

    private void updateMasterUrl(Group group) {
        if (group.isMaster()) {
            LOGGER.debug("Git repo is the master");
        } else {
            LOGGER.debug("Git repo is not the master");
        }
        try {
            GitNode state = createState();
            group.update(state);
            String url = state.getUrl();
            gitRemoteUrl = getSubstitutedData(curator.get(), url);
            if (group.isMaster()) {
                updateConfigAdmin();
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private synchronized void registerServlet() {
        try {
            HttpContext base = httpService.get().createDefaultHttpContext();
            HttpContext secure = new SecureHttpContext(base, realm, role);
            String basePath = System.getProperty("karaf.data") + File.separator + "git" + File.separator + "servlet" + File.separator;
            String fabricGitPath = basePath + "fabric";
            File fabricRoot = new File(fabricGitPath);

            //Only need to clone once. If repo already exists, just skip.
            if (!fabricRoot.exists()) {
                Git localGit = gitService.get().get();
                Git.cloneRepository()
                        .setBare(true)
                        .setNoCheckout(true)
                        .setCloneAllBranches(true)
                        .setDirectory(fabricRoot)
                        .setURI(localGit.getRepository().getDirectory().toURI().toString())
                        .call();
            }

            Dictionary<String, Object> initParams = new Hashtable<String, Object>();
            initParams.put("base-path", basePath);
            initParams.put("repository-root", basePath);
            initParams.put("export-all", "true");
            httpService.get().registerServlet("/git", gitServlet, initParams, secure);
            activateComponent();
        } catch (Exception e) {
            FabricException.launderThrowable(e);
        }
    }

    private void unregisterServlet() {
       httpService.get().unregister("/git");
    }

    private void updateConfigAdmin() {
        // lets register the current URL to ConfigAdmin
        try {
            Configuration conf = configAdmin.get().getConfiguration(GIT_PID);
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
        String fabricRepoUrl = "${zk:" + KARAF_NAME + "/http}/git/fabric/";
        GitNode state = new GitNode();
        state.setId("fabric-repo");
        state.setContainer(KARAF_NAME);
        if (group != null && group.isMaster()) {
            state.setUrl(fabricRepoUrl);
        }
        return state;
    }

    void bindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.bind(service);
    }

    void unbindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.unbind(service);
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

    void bindGitService(GitService service) {
        this.gitService.bind(service);
    }

    void unbindGitService(GitService service) {
        this.gitService.unbind(service);
    }
}
