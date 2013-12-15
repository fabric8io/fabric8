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

import java.io.PrintStream;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import org.apache.felix.gogo.commands.Command;
import io.fabric8.boot.commands.support.FabricCommand;
import io.fabric8.service.jclouds.ComputeRegistry;
import org.jclouds.apis.ApiMetadata;
import org.jclouds.apis.Apis;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.javax.annotation.Nullable;
import org.jclouds.providers.ProviderMetadata;
import org.jclouds.providers.Providers;

@Command(name = "cloud-service-list", scope = "fabric", description = "Lists the cloud providers registered with the fabric.")
public class CloudServiceList extends FabricCommand {

	public static final String PROVIDERFORMAT = "%-24s %-12s %-24s %-24s";
	private ComputeRegistry computeRegistry;

	@Override
	protected Object doExecute() throws Exception {
		Iterable<ProviderMetadata> providers = Providers.viewableAs(TypeToken.of(ComputeServiceContext.class));
		Iterable<ApiMetadata> apis = Apis.viewableAs(TypeToken.of(ComputeServiceContext.class));

		Iterable<String> providerIds =  Iterables.transform(providers, new Function<ProviderMetadata, String>() {
			@Override
			public String apply(@Nullable ProviderMetadata input) {
				return input.getId();
			}
		});

		Iterable<String> apiIds =  Iterables.transform(apis, new Function<ApiMetadata, String>() {
			@Override
			public String apply(@Nullable ApiMetadata input) {
				return input.getId();
			}
		});


		boolean providerOrApiFound = false;

		if (apiIds != null) {
			providerOrApiFound = true;
			System.out.println("Compute APIs:");
			System.out.println("-------------");
			printComputeProvidersOrApis(apiIds,  computeRegistry.list(), "", System.out);
		}

		if (providers != null) {
			providerOrApiFound = true;
			System.out.println("Compute Providers:");
			System.out.println("-------------");
			printComputeProvidersOrApis(providerIds, computeRegistry.list(), "", System.out);
		}

		if (!providerOrApiFound) {
			System.out.println("No providers or apis have been found.");
		}
		return null;
	}

	protected void printComputeProvidersOrApis(Iterable<String> providersOrApis, List<ComputeService> computeServices, String indent, PrintStream out) {
		out.println(String.format(PROVIDERFORMAT, "[id]", "[type]", "[local services]", "[fabric services]"));
		for (String providerOrApi : providersOrApis) {
			boolean registered = false;
			StringBuffer localServices = new StringBuffer();
			StringBuffer fabricServices = new StringBuffer();

			localServices.append("[ ");
			fabricServices.append("[ ");


			for (ComputeService computeService : computeServices) {
				if (computeService.getContext().unwrap().getId().equals(providerOrApi)) {
					String name = (String) computeService.getContext().unwrap().getName();
					if (getCurator() != null && getCurator().getZookeeperClient().isConnected()) {
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

    public ComputeRegistry getComputeRegistry() {
        return computeRegistry;
    }

    public void setComputeRegistry(ComputeRegistry computeRegistry) {
        this.computeRegistry = computeRegistry;
    }
}
