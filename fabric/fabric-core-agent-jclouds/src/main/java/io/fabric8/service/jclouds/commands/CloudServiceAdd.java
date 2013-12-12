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

import java.util.Map;
import com.google.common.base.Strings;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import io.fabric8.boot.commands.support.FabricCommand;
import io.fabric8.service.jclouds.ComputeRegistry;
import io.fabric8.service.jclouds.internal.CloudUtils;
import org.jclouds.karaf.utils.EnvHelper;

@Command(name = "cloud-service-add", scope = "fabric", description = "Registers a cloud provider with the fabric.")
public class CloudServiceAdd extends FabricCommand {

    @Option(name = "--name", required = false, description = "The service context name. Used to distinct between multiple service of the same provider/api.")
    protected String contextName;

    @Option(name = "--provider", required = false, description = "The cloud provider name. Example: aws-ec2.")
    private String provider;

    @Option(name = "--api", required = false, description = "The cloud api name. Example: openstack, cloudstack.")
    private String api;

    @Option(name = "--endpoint", required = false, description = "The cloud endpoint.")
    private String endpoint;

    @Option(name = "--identity", required = false, description = "The identity used to access the cloud provider.")
    private String identity;

    @Option(name = "--credential", required = false, description = "The credential used to access the cloud provider.")
    private String credential;

    @Option(name = "--async-registration", required = false, description = "Do not wait for the provider registration.")
    private Boolean registerAsync = false;

    @Option(name = "--owner", required = false, description = "EC2 AMI owner.")
    private String owner;

    @Option(name = "--option", required = false, multiValued = true, description = "Provider specific properties. Example: --option jclouds.regions=us-east-1.")
    private String[] options;

    private ComputeRegistry computeRegistry;

    @Override
    protected Object doExecute() throws Exception {
        String serviceName = null;
        String providerValue = EnvHelper.getComputeProvider(provider);
        String apiValue = EnvHelper.getBlobStoreApi(api);
        String endpointValue = EnvHelper.getComputeEndpoint(endpoint);
        String identityValue = EnvHelper.getComputeIdentity(identity);
        String credentialValue = EnvHelper.getComputeCredential(credential);

        if (contextName == null && providerValue != null) {
            contextName = providerValue;
        } else if (contextName == null && apiValue != null) {
            contextName = apiValue;
        }

        Map<String, String> props = CloudUtils.parseProviderOptions(options);
        if (options != null && options.length > 1) {
            for (String option : options) {
                if (option.contains("=")) {
                    String key = option.substring(0, option.indexOf("="));
                    String value = option.substring(option.lastIndexOf("=") + 1);
                    props.put(key, value);
                }
            }
        }

        if (!Strings.isNullOrEmpty(owner)) {
            props.put("owner", owner);
        }

        //No Provider or Api defined.
        if (Strings.isNullOrEmpty(providerValue) && Strings.isNullOrEmpty(apiValue)) {
            System.out.println("Need to specify at least a provider or an api as an option (--provider or --api) or as an environmental variable.");
            System.out.println("Supported environmental variables for provider and api are JCLOUDS_COMPUTE_PROVIDER JCLOUDS_COMPUTE_API respectively.");
            return null;
        }
        // Api without Endpoint specified.
        else if (Strings.isNullOrEmpty(providerValue) && !Strings.isNullOrEmpty(apiValue) && Strings.isNullOrEmpty(endpointValue)) {
            System.out.println("You specified an api but no endpoint is found. To use an api, you need an endpoint as option or as environmental variable");
            return null;
        }
        // Only Provider is specified.
        else if (!Strings.isNullOrEmpty(providerValue) && (Strings.isNullOrEmpty(apiValue) || Strings.isNullOrEmpty(endpointValue))) {
            serviceName = providerValue;
            computeRegistry.remove(serviceName);
            CloudUtils.registerProvider(getCurator(), configurationAdmin, contextName, providerValue, identityValue, credentialValue, props);
        }
        //Only Api specified
        else if (Strings.isNullOrEmpty(providerValue) && (!Strings.isNullOrEmpty(apiValue) && !Strings.isNullOrEmpty(endpointValue))) {
            serviceName = apiValue;
            computeRegistry.remove(serviceName);
            CloudUtils.registerApi(getCurator(), configurationAdmin, contextName, apiValue, endpointValue, identityValue, credentialValue, props);
        }
        //Both are specified but Api is passed as an option, so it gains priority.
        else if (Strings.isNullOrEmpty(api)) {
            serviceName = apiValue;
            computeRegistry.remove(serviceName);
            CloudUtils.registerApi(getCurator(), configurationAdmin, contextName, apiValue, endpointValue, identityValue, credentialValue, props);
        }
        //In all other cases we assume the user wants to use a provider.
        else {
            serviceName = providerValue;
            computeRegistry.remove(serviceName);
            CloudUtils.registerProvider(getCurator(), configurationAdmin, contextName, providerValue, identityValue, credentialValue, props);
        }


        if (!registerAsync) {
            System.out.println("Waiting for " + serviceName + " service to initialize.");
            computeRegistry.getOrWait(serviceName);
        }
        return null;
    }

    public ComputeRegistry getComputeRegistry() {
        return computeRegistry;
    }

    public void setComputeRegistry(ComputeRegistry computeRegistry) {
        this.computeRegistry = computeRegistry;
    }
}
