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

package io.fabric8.service.jclouds;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import com.google.common.base.Strings;
import io.fabric8.api.CreationStateListener;
import io.fabric8.internal.ContainerProviderUtils;
import io.fabric8.service.jclouds.firewall.FirewallManager;
import io.fabric8.service.jclouds.firewall.FirewallManagerFactory;
import io.fabric8.service.jclouds.firewall.FirewallNotSupportedOnProviderException;
import io.fabric8.service.jclouds.firewall.Rule;
import io.fabric8.zookeeper.ZkDefs;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.rest.AuthorizationException;
import org.jclouds.ssh.SshException;
import org.jledit.utils.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static io.fabric8.internal.ContainerProviderUtils.buildInstallAndStartScript;

public class CloudContainerInstallationTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(JcloudsContainerProvider.class);

    private final String containerName;
    private final NodeMetadata nodeMetadata;
    private final CreateJCloudsContainerOptions options;
    private final CreationStateListener listener;

    private final ComputeService computeService;
    private final FirewallManagerFactory firewallManagerFactory;
    private final TemplateOptions templateOptions;

    public CloudContainerInstallationTask(String containerName, NodeMetadata nodeMetadata, CreateJCloudsContainerOptions options, ComputeService computeService, FirewallManagerFactory firewallManagerFactory, TemplateOptions templateOptions, CreationStateListener listener) {
        this.containerName = containerName;
        this.nodeMetadata = nodeMetadata;
        this.options = options;
        this.computeService = computeService;
        this.firewallManagerFactory = firewallManagerFactory;
        this.templateOptions = templateOptions;
        this.listener = listener;
    }

    public CreateJCloudsContainerMetadata install() {
        LoginCredentials credentials = nodeMetadata.getCredentials();
        //For some cloud providers return do not allow shell access to root, so the user needs to be overrided.
        if (!Strings.isNullOrEmpty(options.getUser()) && credentials != null) {
            credentials = credentials.toBuilder().user(options.getUser()).build();
        } else {
            credentials = nodeMetadata.getCredentials();
        }
        String id = nodeMetadata.getId();
        Set<String> publicAddresses = nodeMetadata.getPublicAddresses();

        //Make a copy of the addresses, because we don't want to return back a guice implementation of Set.
        Set<String> copyOfPublicAddresses = new HashSet<String>();
        for (String publicAddress : publicAddresses) {
            copyOfPublicAddresses.add(publicAddress);
        }

        CreateJCloudsContainerMetadata jCloudsContainerMetadata = new CreateJCloudsContainerMetadata();
        jCloudsContainerMetadata.setCreateOptions(options);
        jCloudsContainerMetadata.setNodeId(nodeMetadata.getId());
        jCloudsContainerMetadata.setContainerName(containerName);
        jCloudsContainerMetadata.setPublicAddresses(copyOfPublicAddresses);
        jCloudsContainerMetadata.setHostname(nodeMetadata.getHostname());


        if (credentials != null) {
            jCloudsContainerMetadata.setIdentity(credentials.identity);
            jCloudsContainerMetadata.setCredential(credentials.credential);
        }

        String publicAddress = "";
        Properties addresses = new Properties();
        if (publicAddresses != null && !publicAddresses.isEmpty()) {
            publicAddress = publicAddresses.iterator().next();
            addresses.put(ZkDefs.PUBLIC_IP, publicAddress);
        }

        options.getSystemProperties().put(ContainerProviderUtils.ADDRESSES_PROPERTY_KEY, addresses);
        options.getMetadataMap().put(containerName, jCloudsContainerMetadata);

        //Setup firwall for node
        try {
            FirewallManager firewallManager = firewallManagerFactory.getFirewallManager(computeService);
            if (firewallManager.isSupported()) {
                listener.onStateChange("Configuring firewall.");
                String source = getOriginatingIp();

                Rule httpRule = Rule.create().source("0.0.0.0/0").destination(nodeMetadata).port(8181);
                firewallManager.addRules(httpRule);

                if (source != null) {
                    Rule jmxRule = Rule.create().source(source).destination(nodeMetadata).ports(44444, 1099);
                    Rule sshRule = Rule.create().source(source).destination(nodeMetadata).port(8101);
                    Rule zookeeperRule = Rule.create().source(source).destination(nodeMetadata).port(2181);
                    firewallManager.addRules(jmxRule, sshRule, zookeeperRule);
                }
                //We do add the target node public address to the firewall rules, as a way to make things easier in cases
                //where firewall configuration is shared among nodes of the same groups, e.g. EC2.
                if (!Strings.isNullOrEmpty(publicAddress)) {
                    Rule zookeeperFromTargetRule = Rule.create().source(publicAddress + "/32").destination(nodeMetadata).port(2181);
                    firewallManager.addRule(zookeeperFromTargetRule);
                }
            } else {
                listener.onStateChange(String.format("Skipping firewall configuration. Not supported for provider %s", options.getProviderName()));
            }
        } catch (FirewallNotSupportedOnProviderException e) {
            LOGGER.warn("Firewall manager not supported. Firewall will have to be manually configured.");
        } catch (IOException e) {
            LOGGER.warn("Could not lookup originating ip. Firewall will have to be manually configured.", e);
        } catch (Throwable t) {
            LOGGER.warn("Failed to setup firewall", t);
        }


        try {
            String script = buildInstallAndStartScript(containerName, options);
            listener.onStateChange(String.format("Installing fabric agent on container %s. It may take a while...", containerName));
            ExecResponse response = null;
            try {
                if (credentials != null) {
                    response = computeService.runScriptOnNode(id, script, templateOptions.overrideLoginCredentials(credentials).runAsRoot(false));
                } else {
                    response = computeService.runScriptOnNode(id, script, templateOptions);
                }
            } catch (AuthorizationException ex) {
                throw new Exception("Failed to connect to the container via ssh.");
            } catch (SshException ex) {
                throw new Exception("Failed to connect to the container via ssh.");
            }

            if (response != null && response.getOutput() != null) {
                if (response.getOutput().contains(ContainerProviderUtils.FAILURE_PREFIX)) {
                    jCloudsContainerMetadata.setFailure(new Exception(ContainerProviderUtils.parseScriptFailure(response.getOutput())));
                }
                String overridenResolverValue = ContainerProviderUtils.parseResolverOverride(response.getOutput());
                if (overridenResolverValue != null) {
                    jCloudsContainerMetadata.setOverridenResolver(overridenResolverValue);
                    listener.onStateChange("Overriding resolver to " + overridenResolverValue + ".");
                }
            } else {
                jCloudsContainerMetadata.setFailure(new Exception("No response received for fabric install script."));
            }
        } catch (Throwable t) {
            jCloudsContainerMetadata.setFailure(t);
        }
        //Cleanup addresses.
        options.getSystemProperties().clear();
        return jCloudsContainerMetadata;
    }

    /**
     * @return the IP address of the client on which this code is running.
     * @throws java.io.IOException
     */
    private String getOriginatingIp() throws IOException {
        String ip = null;
        try {
            URL url = new URL("http://checkip.amazonaws.com/");
            ip = Resources.toString(url).trim() + "/32";
        } catch (Throwable t) {
            LOGGER.warn("Failed to lookup public ip of current container.");
        }
        return ip;
    }
}
