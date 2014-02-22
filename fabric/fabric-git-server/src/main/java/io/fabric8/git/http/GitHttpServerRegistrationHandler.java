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

package io.fabric8.git.http;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.eclipse.jgit.api.Git;
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricService;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.git.GitNode;
import io.fabric8.git.GitService;
import io.fabric8.groups.Group;
import io.fabric8.groups.GroupListener;
import io.fabric8.groups.internal.ZooKeeperGroup;
import io.fabric8.zookeeper.ZkPath;
import io.fabric8.zookeeper.utils.ZooKeeperUtils;
import io.fabric8.api.TargetContainer;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.utils.SystemProperties;
import org.eclipse.jgit.http.server.GitServlet;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;


@ThreadSafe
@Component(name = "io.fabric8.git.server", label = "Fabric8 Git HTTP Server Registration Handler", policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = true)
public final class GitHttpServerRegistrationHandler extends AbstractComponent implements GroupListener<GitNode> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitHttpServerRegistrationHandler.class);

    private static final String GIT_PID = "io.fabric8.git";
    private static final String REALM_PROPERTY_NAME = "realm";
    private static final String ROLE_PROPERTY_NAME = "role";
    private static final String DEFAULT_ROLE = "admin";

    private static final Map<TargetContainer, String> defaultRealms;
    static {
        Map<TargetContainer, String> realms = new HashMap<TargetContainer, String>();
        realms.put(TargetContainer.KARAF, "karaf");
        realms.put(TargetContainer.TOMCAT, "fabric");
        realms.put(TargetContainer.WILDFLY, "fabric-domain");
        defaultRealms = Collections.unmodifiableMap(realms);
    }

    @Reference(referenceInterface = HttpService.class)
    private final ValidatingReference<HttpService> httpService = new ValidatingReference<HttpService>();
    @Reference(referenceInterface = ConfigurationAdmin.class)
    private final ValidatingReference<ConfigurationAdmin> configAdmin = new ValidatingReference<ConfigurationAdmin>();
    @Reference(referenceInterface = RuntimeProperties.class)
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<RuntimeProperties>();
    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();
    @Reference(referenceInterface = GitService.class)
    private final ValidatingReference<GitService> gitService = new ValidatingReference<GitService>();

    //Reference not used, but it expresses the dependency on a fully initialized fabric.
    @Reference
    private FabricService fabricService;

    private volatile Group<GitNode> group;
    private volatile String gitRemoteUrl;

    @Activate
    void activate(Map<String, ?> configuration) {

        RuntimeProperties sysprops = runtimeProperties.get();
        String realm = getConfiguredRealm(sysprops, configuration);
        String role = getConfiguredRole(sysprops, configuration);
        registerServlet(sysprops, realm, role);

        activateComponent();

        group = new ZooKeeperGroup<GitNode>(curator.get(), ZkPath.GIT.getPath(), GitNode.class);
        group.add(this);
        group.update(createState());
        group.start();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        unregisterServlet();
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
        if (isValid()) {
            switch (event) {
            case CONNECTED:
            case CHANGED:
                updateMasterUrl(group);
                break;
            default:
                // do nothing
            }
        }
    }

    private String getConfiguredRealm(RuntimeProperties sysprops, Map<String, ?> configuration) {
        String realm = (String)configuration.get(REALM_PROPERTY_NAME);
        if (realm == null) {
            TargetContainer targetContainer = TargetContainer.getTargetContainer(sysprops);
            realm = defaultRealms.get(targetContainer);
        }
        return realm;
    }

    private String getConfiguredRole(RuntimeProperties sysprops, Map<String, ?> configuration) {
        return configuration.containsKey(ROLE_PROPERTY_NAME) ? (String)configuration.get(ROLE_PROPERTY_NAME) : DEFAULT_ROLE;
    }

    private void updateMasterUrl(Group<GitNode> group) {
        if (group.isMaster()) {
            LOGGER.debug("Git repo is the master");
        } else {
            LOGGER.debug("Git repo is not the master");
        }
        try {
            GitNode state = createState();
            group.update(state);
            String url = state.getUrl();
            gitRemoteUrl = ZooKeeperUtils.getSubstitutedData(curator.get(), url);
            if (group.isMaster()) {
                updateConfigAdmin();
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private void registerServlet(RuntimeProperties sysprops, String realm, String role) {
        String servletBasePath = sysprops.getProperty(SystemProperties.KARAF_DATA) + File.separator + "git" + File.separator + "servlet" + File.separator;
        String fabricRepoPath = servletBasePath + "fabric";
        try {
            HttpContext base = httpService.get().createDefaultHttpContext();
            HttpContext secure = new GitSecureHttpContext(base, realm, role);

            File fabricRoot = new File(fabricRepoPath);

            //Only need to clone once. If repo already exists, just skip.
            if (!fabricRoot.exists()) {
                Git localGit = gitService.get().get();
                Git.cloneRepository()
                        .setTimeout(10)
                        .setBare(true)
                        .setNoCheckout(true)
                        .setCloneAllBranches(true)
                        .setDirectory(fabricRoot)
                        .setURI(localGit.getRepository().getDirectory().toURI().toString())
                        .call().getRepository().close();
            }

            Dictionary<String, Object> initParams = new Hashtable<String, Object>();
            initParams.put("base-path", servletBasePath);
            initParams.put("repository-root", servletBasePath);
            initParams.put("export-all", "true");
            httpService.get().registerServlet("/git", new GitServlet(), initParams, secure);
        } catch (Throwable t) {
            throw FabricException.launderThrowable(t);
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
        RuntimeProperties sysprops = runtimeProperties.get();
        TargetContainer runtimeType = TargetContainer.getTargetContainer(sysprops);
        String context = runtimeType == TargetContainer.KARAF ? "" : "/fabric";
        String karafName = sysprops.getProperty(SystemProperties.KARAF_NAME);
        String fabricRepoUrl = "${zk:" + karafName + "/http}" + context + "/git/fabric/";
        GitNode state = new GitNode();
        state.setId("fabric-repo");
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

    void bindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.bind(service);
    }

    void unbindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.unbind(service);
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
