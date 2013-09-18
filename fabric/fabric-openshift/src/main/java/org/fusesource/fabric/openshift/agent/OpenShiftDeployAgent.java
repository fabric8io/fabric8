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

package org.fusesource.fabric.openshift.agent;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.openshift.client.IApplication;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.IOpenShiftSSHKey;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.fusesource.common.util.Maps;
import org.fusesource.common.util.Strings;
import org.fusesource.fabric.agent.download.DownloadManager;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.DataStore;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.groups.Group;
import org.fusesource.fabric.groups.GroupListener;
import org.fusesource.fabric.groups.internal.ZooKeeperGroup;
import org.fusesource.fabric.openshift.CreateOpenshiftContainerOptions;
import org.fusesource.fabric.openshift.OpenShiftConstants;
import org.fusesource.fabric.openshift.OpenShiftUtils;
import org.fusesource.fabric.utils.SystemProperties;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Fabric agent which manages arbitrary Java cartridges on OpenShift; so that changes to the Fabric profile
 * metadata (such as WARs, bundles, features) leads to the git configuration being updated for the managed
 * cartridges.
 */
@Component(name = "org.fusesource.fabric.openshift.agent",
        description = "Fabric agent for deploying applications into external OpenShift cartridges",
        immediate = true)
public class OpenShiftDeployAgent implements GroupListener<ControllerNode> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenShiftDeployAgent.class);
    private static final String REALM_PROPERTY_NAME = "realm";
    private static final String ROLE_PROPERTY_NAME = "role";
    private static final String DEFAULT_REALM = "karaf";
    private static final String DEFAULT_ROLE = "admin";


    private final String name = System.getProperty(SystemProperties.KARAF_NAME);

    private Group<ControllerNode> group;

    @Reference(cardinality = org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY)
    private ConfigurationAdmin configurationAdmin;
    @Reference(cardinality = org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY)
    private CuratorFramework curator;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private FabricService fabricService;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            onConfigurationChanged();
        }
    };


    public OpenShiftDeployAgent() {
    }


    @Activate
    public void init(Map<String, String> properties) {
/*
        this.realm =  properties != null && properties.containsKey(REALM_PROPERTY_NAME) ? properties.get(REALM_PROPERTY_NAME) : DEFAULT_REALM;
        this.role =  properties != null && properties.containsKey(ROLE_PROPERTY_NAME) ? properties.get(ROLE_PROPERTY_NAME) : DEFAULT_ROLE;
*/

        group = new ZooKeeperGroup(curator, ZkPath.OPENSHIFT.getPath(), ControllerNode.class);
        group.add(this);
        group.update(createState());
        group.start();
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
    public void groupEvent(Group<ControllerNode> group, GroupEvent event) {
        if (group.isMaster()) {
            LOGGER.info("OpenShiftController repo is the master");
        } else {
            LOGGER.info("OpenShiftController repo is not the master");
        }
        try {
            DataStore dataStore = null;
            if (fabricService != null) {
                dataStore = fabricService.getDataStore();
            } else {
                LOGGER.warn("No fabricService yet!");
            }
            if (group.isMaster()) {
                ControllerNode state = createState();
                group.update(state);
            }
            if (dataStore != null) {
                if (group.isMaster()) {
                    dataStore.trackConfiguration(runnable);
                    onConfigurationChanged();
                } else {
                    dataStore.unTrackConfiguration(runnable);
                }
            }
        } catch (IllegalStateException e) {
            // Ignore
        }
    }


    protected void onConfigurationChanged() {
        LOGGER.info("Configuration has changed; so checking the external Java containers are up to date");
        Container[] containers = fabricService.getContainers();
        for (Container container : containers) {
            Profile profile = container.getOverlayProfile();
            Map<String, Map<String, String>> configurations = profile.getConfigurations();
            Map<String, String> openshiftConfiguration = configurations
                    .get(OpenShiftConstants.OPENSHIFT_PID);
            if (openshiftConfiguration != null) {
                DeploymentTask deployTask = null;
                try {
                    deployTask = createDeployTask(container, openshiftConfiguration);
                } catch (MalformedURLException e) {
                    LOGGER.error("Failed to create DeploymentTask. " + e, e);
                }
                if (deployTask != null && OpenShiftUtils.isFabricManaged(openshiftConfiguration)) {
                    String containerId = container.getId();
                    IOpenShiftConnection connection = OpenShiftUtils.createConnection(container);
                    CreateOpenshiftContainerOptions options = OpenShiftUtils.getCreateOptions(container);
                    if (connection == null || options == null) {
                        LOGGER.warn(
                                "Ignoring container which has no openshift connection or options. connection: "
                                        + connection + " options: " + options);
                    } else {
                        try {
                            IApplication application = OpenShiftUtils.getApplication(container, connection);
                            if (application != null) {
                                final String gitUrl = application.getGitUrl();
                                if (gitUrl != null) {
                                    LOGGER.info("Git URL is " + gitUrl);

                                    final CartridgeGitRepository repo = new CartridgeGitRepository(containerId);

                                    final List<IOpenShiftSSHKey> sshkeys = application.getDomain().getUser()
                                            .getSSHKeys();

                                    final CredentialsProvider credentials = new CredentialsProvider() {
                                        @Override
                                        public boolean supports(CredentialItem... items) {
                                            return true;
                                        }

                                        @Override
                                        public boolean isInteractive() {
                                            return true;
                                        }

                                        @Override
                                        public boolean get(URIish uri, CredentialItem... items)
                                                throws UnsupportedCredentialItem {

                                            LOGGER.info("Credential request " + uri + " items " + Arrays.asList(items));
                                            int i = -1;
                                            for (CredentialItem item : items) {
                                                if (item instanceof CredentialItem.StringType) {
                                                    CredentialItem.StringType stringType
                                                            = (CredentialItem.StringType)item;

                                                    int idx = ++i < sshkeys.size() ? i : 0;
                                                    if (idx < sshkeys.size()) {
                                                        IOpenShiftSSHKey sshKey = sshkeys
                                                                .get(idx);
                                                        String passphrase = sshKey.getPublicKey();
                                                        LOGGER.info("For item " + item + " index " + i
                                                                + " using passphrase: " + passphrase);
                                                        stringType.setValue(passphrase);
                                                    } else {
                                                        LOGGER.warn("No ssh keys we can pass into git!");
                                                    }
                                                    continue;
                                                } else {
                                                    LOGGER.warn("Unknown CredentialItem " + item);
                                                }
                                            }
                                            return true;
                                        }
                                    };

                                    final DeploymentTask finalDeployTask = deployTask;
                                    SshSessionFactoryUtils.useOpenShiftSessionFactory(new Callable<Object>() {

                                        @Override
                                        public Object call()
                                                throws Exception {
                                            repo.cloneOrPull(gitUrl, credentials);
                                            finalDeployTask.updateDeployment(repo.getGit(), repo.getLocalRepo(),
                                                            credentials);
                                            return null;
                                        }
                                    });
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error("Failed to update container " + containerId + ". Reason: " + e, e);
                        } finally {
                            OpenShiftUtils.close(connection);
                        }
                    }
                }
            }
        }
    }

    protected DeploymentTask createDeployTask(Container container, Map<String,String> openshiftConfiguration)
            throws MalformedURLException {
        String webappDir = relativePath(container, openshiftConfiguration, OpenShiftConstants.PROPERTY_DEPLOY_WEBAPPS);
        String deployDir = relativePath(container, openshiftConfiguration, OpenShiftConstants.PROPERTY_DEPLOY_JARS);
        if (webappDir != null || deployDir != null) {
            DownloadManager downloadManager = DownloadManagers.createDownloadManager(fabricService, container.getOverlayProfile(), executorService);
            return new DeploymentTask(downloadManager, container, webappDir, deployDir);
        }
        return null;
    }


    protected static String relativePath(Container container, Map<String, String> configuration,
                                         String propertyName) {
        String value = Maps.stringValue(configuration, propertyName);
        if (Strings.isNotBlank(value)) {
            if (value.startsWith("..") || value.startsWith("/") || value.startsWith(File.separator)) {
                throw new IllegalStateException("Invalid relative path '" + value + "' for property " + propertyName + " for container " + container.getId());
            } else {
                return value;
            }
        }
        return null;
    }


    ControllerNode createState() {
        ControllerNode state = new ControllerNode();
        state.setContainer(name);
        return state;
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
