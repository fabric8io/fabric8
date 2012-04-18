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

import java.io.PrintStream;
import java.util.List;
import java.util.Set;
import org.apache.felix.gogo.commands.Command;
import org.apache.zookeeper.KeeperException;
import org.fusesource.fabric.commands.support.FabricCommand;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.jclouds.compute.ComputeService;
import org.jclouds.karaf.core.ComputeProviderListener;

@Command(name = "cloud-provider-list", scope = "fabric", description = "Registers a cloud provider to the registry.")
public class CloudProviderList extends FabricCommand {

    public static final String PROVIDERFORMAT = "%-24s %-12s %-12s";

    private ComputeProviderListener computeProviderListener;
    private List<ComputeService> computeServices;

    @Override
    protected Object doExecute() throws Exception {
        Set<String> providers = computeProviderListener.getInstalledProviders();
        if (providers != null && !providers.isEmpty()) {
            printComputeProviders(providers,computeServices, "", System.out);
        } else {
            System.out.print("No providers have been found.");
        }
        return null;
    }

    protected void printComputeProviders(Set<String> providers, List<ComputeService> computeServices, String indent, PrintStream out) {
        out.println(String.format(PROVIDERFORMAT, "[id]", "[type]", "[service registration]"));
        for (String provider : providers) {
            boolean registered = false;
            String registrationType = "none";

            for (ComputeService computeService:computeServices) {
                if (computeService.getContext().getProviderSpecificContext().getId().equals(provider)) {
                    registered = true;
                    break;
                }
            }

            if (getZooKeeper() != null && getZooKeeper().isConnected()) {
                try {
                    if (getZooKeeper().exists(ZkPath.CLOUD_PROVIDER.getPath(provider)) == null) {
                        registrationType = "local";
                    } else {
                        registrationType = "fabric";
                    }
                } catch (Exception e) {
                    //noop
                }
            } else if (registered) {
                registrationType = "local";
            }
            out.println(String.format(PROVIDERFORMAT, provider, "compute", registrationType));
        }
    }

    public ComputeProviderListener getComputeProviderListener() {
        return computeProviderListener;
    }

    public void setComputeProviderListener(ComputeProviderListener computeProviderListener) {
        this.computeProviderListener = computeProviderListener;
    }

    public List<ComputeService> getComputeServices() {
        return computeServices;
    }

    public void setComputeServices(List<ComputeService> computeServices) {
        this.computeServices = computeServices;
    }
}
