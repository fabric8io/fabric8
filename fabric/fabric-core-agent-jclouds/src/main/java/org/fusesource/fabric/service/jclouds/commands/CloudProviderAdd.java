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

package org.fusesource.fabric.service.jclouds.commands;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Properties;
import com.google.common.base.Strings;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.boot.commands.support.FabricCommand;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(name = "cloud-provider-add", scope = "fabric", description = "Registers a cloud provider to the registry.")
public class CloudProviderAdd extends FabricCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudProviderAdd.class);

    private static final String FACTORY_FILTER = "(service.factoryPid=%s)";

    @Argument(index = 0, name = "provider", required = true, description = "The cloud provider name")
    private String provider;
    @Argument(index = 1, name = "identity", required = false, description = "The cloud identity to use")
    private String identity;
    @Argument(index = 2, name = "credential", required = true, description = "The cloud credential to use")
    private String credential;

    @Option(name = "--owner", required = false, description = "EC2 AMI owner")
    private String owner;

    @Override
    protected Object doExecute() throws Exception {

        Container current = fabricService.getCurrentContainer();
        if (!getZooKeeper().isConnected() || !current.isManaged()) {
            Configuration configuration = findOrCreateFactoryConfiguration("org.jclouds.compute", provider);
            Dictionary dictionary = configuration.getProperties();
            if (dictionary == null) {
                dictionary = new Properties();
            }
            dictionary.put("provider", provider);
            dictionary.put("credential", credential);
            dictionary.put("identity", identity);
            dictionary.put("credential-store", "zookeeper");
            if (!Strings.isNullOrEmpty(owner) && "aws-ec2".equals(provider)) {
                dictionary.put("jclouds.ec2.ami-owners", owner);

            }
            configuration.update(dictionary);
        }

        if (getZooKeeper().isConnected()) {
            if (getZooKeeper().exists(ZkPath.CLOUD_PROVIDER.getPath(provider)) == null) {
                ZooKeeperUtils.create(getZooKeeper(), ZkPath.CLOUD_PROVIDER.getPath(provider));
            }
            ZooKeeperUtils.set(getZooKeeper(), ZkPath.CLOUD_PROVIDER_IDENTIY.getPath(provider), identity);
            ZooKeeperUtils.set(getZooKeeper(), ZkPath.CLOUD_PROVIDER_CREDENTIAL.getPath(provider), credential);
        } else {
            System.out.println("Fabric has not been initialized. Provider registration is local to the current container.");
        }

        return null;
    }

    /**
     * Search the configuration admin for the specified factoryPid that refers to the provider.
     * @param factoryPid
     * @param provider
     * @return
     * @throws IOException
     */
    protected Configuration findOrCreateFactoryConfiguration(String factoryPid, String provider) throws IOException {
        Configuration configuration = null;
        try {
            Configuration[] configurations = configurationAdmin.listConfigurations(String.format(FACTORY_FILTER,factoryPid));
            if (configurations != null) {
                for (Configuration conf : configurations) {
                    Dictionary dictionary = configuration.getProperties();
                    if (dictionary != null && provider.equals(dictionary.get("provider"))) {
                        return conf;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to lookup configuration admin for existing cloud providers.",e);
        }
        LOGGER.debug("No configuration found with factoryPid org.jclouds.compute. Creating new one.");
        configuration = configurationAdmin.createFactoryConfiguration(factoryPid, null);
        return configuration;
    }

}
