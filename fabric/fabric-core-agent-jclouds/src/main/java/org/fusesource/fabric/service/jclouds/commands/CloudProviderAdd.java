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

import java.util.Dictionary;
import java.util.Properties;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.commands.support.FabricCommand;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils;
import org.osgi.service.cm.Configuration;

@Command(name = "cloud-provider-add", scope = "fabric", description = "Registers a cloud provider to the registry.")
public class CloudProviderAdd extends FabricCommand {

    @Argument(index = 0, name = "provider", required = true, description = "JClouds provider name")
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

            Configuration configuration = configurationAdmin.createFactoryConfiguration("org.jclouds.compute", null);
            Dictionary dictionary = configuration.getProperties();
            if (dictionary == null) {
                dictionary = new Properties();
            }
            dictionary.put("provider", provider);
            dictionary.put("credential", credential);
            dictionary.put("identity", identity);
            dictionary.put("credential-store", "zookeeper");
            configuration.update(dictionary);
        } else {

            ZooKeeperUtils.create(getZooKeeper(), ZkPath.CLOUD_PROVIDER.getPath(provider));
            ZooKeeperUtils.set(getZooKeeper(), ZkPath.CLOUD_PROVIDER_IDENTIY.getPath(provider), identity);
            ZooKeeperUtils.set(getZooKeeper(), ZkPath.CLOUD_PROVIDER_CREDENTIAL.getPath(provider), credential);
        }

        return null;
    }
}
