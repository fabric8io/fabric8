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
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.boot.commands.support.FabricCommand;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.osgi.service.cm.Configuration;

@Command(name = "cloud-provider-remove", scope = "fabric", description = "Removes a cloud provider from the fabric's registry.")
public class CloudProviderRemove extends FabricCommand {
    private static final String PID_FILTER = "(service.pid=%s*)";

    @Argument(index = 0, name = "provider", required = true, description = "JClouds provider name")
    private String provider;

    @Override
    protected Object doExecute() throws Exception {
        boolean connected = getZooKeeper().isConnected();
        Container current = null;
        if (connected) {
            if (getZooKeeper().exists(ZkPath.CLOUD_PROVIDER.getPath(provider)) != null) {
                getZooKeeper().deleteWithChildren(ZkPath.CLOUD_PROVIDER.getPath(provider));
            }
            current = fabricService.getCurrentContainer();
        }
        //Remove compute configurations for the provider.
        Configuration[] computeConfigs = findConfigurationByFactoryPid("org.jclouds.compute");
        if (computeConfigs != null) {
            for (Configuration configuration : computeConfigs) {
                Dictionary props = configuration.getProperties();
                if (props != null) {
                    String fabricPid = (String) props.get("fabric.zookeeper.pid");
                    if (fabricPid.equals("org.jclouds.compute-" + provider.replaceAll("-", ""))) {
                        configuration.delete();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Finds configurations based on their factoryPid.
     *
     * @param factoryPid
     * @return
     */
    private Configuration[] findConfigurationByFactoryPid(String factoryPid) {
        Configuration[] configurations = new Configuration[0];
        try {
            configurations = configurationAdmin.listConfigurations(String.format(PID_FILTER, factoryPid));
        } catch (Exception e) {
            //noop
        }
        return configurations;
    }
}
