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
import java.util.Map;
import java.util.Set;
import org.apache.felix.gogo.commands.Command;
import org.fusesource.fabric.boot.commands.support.FabricCommand;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.jclouds.apis.ApiMetadata;
import org.jclouds.compute.ComputeService;
import org.jclouds.karaf.core.ComputeProviderOrApiRegistry;
import org.jclouds.karaf.core.Constants;
import org.jclouds.providers.ProviderMetadata;

@Command(name = "cloud-service-list", scope = "fabric", description = "Lists the cloud providers registered with the fabric.")
public class CloudServiceList extends FabricCommand {

    public static final String PROVIDERFORMAT = "%-24s %-12s %-24s %-24s";

    private ComputeProviderOrApiRegistry computeProviderOrApiRegistry;
    private List<ComputeService> computeServices;

    @Override
    protected Object doExecute() throws Exception {
        Map<String, ProviderMetadata> providers = computeProviderOrApiRegistry.getInstalledProviders();
        Map<String, ApiMetadata> apis = computeProviderOrApiRegistry.getInstalledApis();

        boolean providerOrApiFound = false;

        if (apis != null && !apis.isEmpty()) {
            providerOrApiFound = true;
            System.out.println("Compute APIs:");
            System.out.println("-------------");
            printComputeProvidersOrApis(apis.keySet(), computeServices, "", System.out);
        }

        if (providers != null && !providers.isEmpty()) {
            providerOrApiFound = true;
            System.out.println("Compute Providers:");
            System.out.println("-------------");
            printComputeProvidersOrApis(providers.keySet(), computeServices, "", System.out);
        }

        if (!providerOrApiFound)  {
            System.out.println("No providers or apis have been found.");
        }
        return null;
    }

    protected void printComputeProvidersOrApis(Set<String> providersOrApis, List<ComputeService> computeServices, String indent, PrintStream out) {
        out.println(String.format(PROVIDERFORMAT, "[id]", "[type]", "[local services]","[fabric services]"));
        for (String providerOrApi : providersOrApis) {
            boolean registered = false;
            StringBuffer localServices = new StringBuffer();
            StringBuffer fabricServices = new StringBuffer();

            localServices.append("[ ");
            fabricServices.append("[ ");


            for (ComputeService computeService : computeServices) {
                if (computeService.getContext().unwrap().getId().equals(providerOrApi)) {
                    String name = (String) computeService.getContext().unwrap().getName();
                    if (getZooKeeper() != null && getZooKeeper().isConnected()) {
                         fabricServices.append(name).append(" ");
                    } else {
                        localServices.append(name).append(" ");
                    }

                }
            }
            localServices.append("]");
            fabricServices.append("]");
            out.println(String.format(PROVIDERFORMAT, providerOrApi, "compute", localServices.toString(), fabricServices.toString()));
        }
    }

    public ComputeProviderOrApiRegistry getComputeProviderOrApiRegistry() {
        return computeProviderOrApiRegistry;
    }

    public void setComputeProviderOrApiRegistry(ComputeProviderOrApiRegistry computeProviderOrApiRegistry) {
        this.computeProviderOrApiRegistry = computeProviderOrApiRegistry;
    }

    public List<ComputeService> getComputeServices() {
        return computeServices;
    }

    public void setComputeServices(List<ComputeService> computeServices) {
        this.computeServices = computeServices;
    }
}
