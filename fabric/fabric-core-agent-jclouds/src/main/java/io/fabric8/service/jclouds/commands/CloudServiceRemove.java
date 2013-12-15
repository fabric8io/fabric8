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

package io.fabric8.service.jclouds.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import io.fabric8.api.Container;
import io.fabric8.boot.commands.support.FabricCommand;
import io.fabric8.zookeeper.ZkPath;
import org.jclouds.karaf.core.Constants;
import org.osgi.service.cm.Configuration;

import java.util.Dictionary;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.deleteSafe;

@Command(name = "cloud-service-remove", scope = "fabric", description = "Removes a cloud provider from the fabric's registry.")
public class CloudServiceRemove extends FabricCommand {
    private static final String PID_FILTER = "(service.pid=%s*)";

    @Argument(index = 0, name = "name", required = true, description = "JClouds context name.")
    private String name;

    @Override
    protected Object doExecute() throws Exception {
        boolean connected = getCurator().getZookeeperClient().isConnected();
        Container current = null;
        if (connected) {
            deleteSafe(getCurator(), ZkPath.CLOUD_SERVICE.getPath(name));
            current = fabricService.getCurrentContainer();
        }
        //Remove compute configurations for the service.
        Configuration[] computeConfigs = findConfigurationByFactoryPid("org.jclouds.compute");
        if (computeConfigs != null) {
            for (Configuration configuration : computeConfigs) {
                Dictionary props = configuration.getProperties();
                if (props != null) {
                    String contextName = (String) props.get(Constants.NAME);
                    if (name.equals(contextName)) {
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
