/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.git.hawtio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Dictionary;

import org.apache.curator.framework.CuratorFramework;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hawt.git.GitFacade;
import io.hawt.util.Strings;

/**
 * A Fabric specific extension to hawtio-git which provides a JMX MBean to browsing/editing the configuration
 * which uses a separate git clone of the Fabric's git repo so that it can be viewed/editted by tools such as
 * <a href="http://hawt.io/">hawtio</a> without interfering with the git repo used by the fabric agent.
 * <p/>
 * So this watches the global master git repo URL and updates the hawtio mbean to use it. It should also do frequent
 * pulls to keep in sync and it also stores the git clone within the karaf data directory.
 */
public class FabricGitFacade extends GitFacade implements ConfigurationListener {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabricGitFacade.class);
    public static final String FABRIC_GIT_PID = "org.fusesource.fabric.git";
    private ConfigurationAdmin configurationAdmin;
    private boolean initialised;
    private boolean initCalled;
    private CuratorFramework curator;

    public boolean isCloneRemoteRepoOnStartup() {
        return true;
    }

    public boolean isPullOnStartup() {
        return true;
    }

    public boolean isCloneAllBranches() {
        return true;
    }

    public boolean isPullBeforeOperation() {
        return true;
    }

    public boolean isPushOnCommit() {
        return true;
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public CredentialsProvider getCredentials() {
        try {
            String container = System.getProperty("karaf.name");
            String login = ZooKeeperUtils.getContainerLogin(container);
            String token = ZooKeeperUtils.generateContainerToken(curator, container);
            CredentialsProvider cp = new UsernamePasswordCredentialsProvider(login, token);
            setCredentials(cp);
        } catch (Exception e) {
            LOG.warn("Failed to get container credentials " + e, e);
        }
        return super.getCredentials();
    }

    public void init() throws Exception {
        // default the directory to inside the karaf data directory
        String basePath = System.getProperty("karaf.data", "karaf/data") + File.separator + "git"
                + File.separator;
        String fabricGitPath = basePath + "fabric-edit";
        File fabricRoot = new File(fabricGitPath);
        if (!fabricRoot.exists() && !fabricRoot.mkdirs()) {
            throw new FileNotFoundException("Could not found git root:" + basePath);
        }
        setConfigDirectory(fabricRoot);
        setCloneAllBranches(true);
        setCloneRemoteRepoOnStartup(true);
        setPullBeforeOperation(true);
        setPushOnCommit(true);
        setPullOnStartup(true);

        initCalled = true;
        updateConfiguration();
        initCheck();
    }

    public void setRemoteRepository(String remoteRepository) {
        super.setRemoteRepository(remoteRepository);
        initCheck();
    }

    protected void initCheck() {
        String url = getRemoteRepository();
        if (initCalled && !initialised && Strings.isNotBlank(url)) {
            initialised = true;
            try {
                super.init();
            } catch (Exception e) {
                LOG.warn("Failed to initialise with remote repository: " + url + ". " + e, e);
            }
        }
    }

    public void configurationEvent(ConfigurationEvent event) {
        String pid = event.getPid();
        if (configurationAdmin != null && (FABRIC_GIT_PID.equals(pid) || FABRIC_GIT_PID
                .equals(event.getFactoryPid()))) {
            updateConfiguration();
        }
    }

    protected void updateConfiguration() {
        Configuration config = null;
        try {
            config = configurationAdmin.getConfiguration(FABRIC_GIT_PID);
        } catch (IOException e) {
            LOG.warn("Could not get configuration for " + FABRIC_GIT_PID + ". " + e, e);
        }
        if (config != null) {
            Dictionary<String, Object> values = config.getProperties();
            if (values != null) {
                Object url = values.get("fabric.git.url");
                if (url instanceof String) {
                    String fabricUrl = url.toString();
                    LOG.info("GitFacade setting the hawtio remote git url to: " + fabricUrl);
                    setRemoteRepository(fabricUrl);
                }
            }
        }
    }
}
