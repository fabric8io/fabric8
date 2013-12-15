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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.google.common.base.Strings;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import io.fabric8.api.Container;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.service.jclouds.CreateJCloudsContainerMetadata;
import io.fabric8.service.jclouds.CreateJCloudsContainerOptions;
import io.fabric8.boot.commands.support.FabricCommand;
import io.fabric8.service.jclouds.firewall.FirewallManager;
import io.fabric8.service.jclouds.firewall.FirewallManagerFactory;
import io.fabric8.service.jclouds.firewall.Rule;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.NodeMetadata;

@Command(name = "cloud-firewall-edit", scope = "fabric", description = "Manages the firewall of a cloud container.")
public class CloudFirewallEdit extends FabricCommand {

    @Option( name = "--port", required = false, multiValued = true, description = "The target port.")
    private int[] port;

    @Option( name = "--flush", required = false, description = "Flush all rules.")
    private boolean flush;

    @Option( name = "--revoke", required = false, description = "Revokes the rule. This will block access to the specified port.")
    private boolean revoke;

    @Option( name = "--source-cidr", required = false, description = "The source cidr to grant or revoke access.")
    private String sourceCidr;

    @Option( name = "--source-container", required = false, description = "The source container to grant or revoke access.")
    private String sourceContainerName;

    @Option( name = "--target-node-id", required = false, description = "The target node id.")
    private String targetNodeId;

    @Option( name = "--target-container", required = false, description = "The target container name.")
    private String targetContainerName;

    @Option(name = "--name", required = false, description = "The service context name. Used to distinct between multiple service of the same provider/api.")
    private String contextName;


    private FirewallManagerFactory firewallManagerFactory;
    private List<ComputeService> computeServices;

    private boolean validateArguments() {
        if (Strings.isNullOrEmpty(contextName) && (Strings.isNullOrEmpty(targetContainerName) || !getCurator().getZookeeperClient().isConnected())) {
            System.out.println("You need to either specify a valid cloud service and a node id or a valid target fabric container name.");
            System.out.println("To use the target container name option you need to be connected to fabric.");
            return false;
        }

        if (!flush && (Strings.isNullOrEmpty(sourceCidr)) && Strings.isNullOrEmpty(sourceContainerName)) {
            System.out.println("You need to specify a source cidr or a source container name, unless you use the --flush option.");
            return false;
        }
        return true;
    }

    /**
     * Returns a {@link Set} of CIDRS based on the the options used.
     * @return
     */
    private Set<String> collectCirds() {
        Set<String> sourceCidrs = new LinkedHashSet<String>();
        //Calculate the source cidrs.
        if (!Strings.isNullOrEmpty(sourceCidr)) {
            sourceCidrs.add(sourceCidr);
        }

        if (getCurator().getZookeeperClient().isConnected() && !Strings.isNullOrEmpty(sourceContainerName)) {
            Container sourceContainer = fabricService.getContainer(sourceContainerName);
            if (sourceContainer != null && !Strings.isNullOrEmpty(sourceContainer.getPublicIp())) {
                sourceCidrs.add(sourceContainer.getPublicIp() + "/32");
            }
            if (sourceContainer != null && !Strings.isNullOrEmpty(sourceContainer.getLocalIp())) {
                sourceCidrs.add(sourceContainer.getLocalIp() + "/32");
            }
        }
        return sourceCidrs;
    }

    /**
     * Finds the {@link ComputeService} to use based on the provider option or the target {@link Container} metadata.
     * @return
     */
    private ComputeService findTargetComputeService() {
        if (!Strings.isNullOrEmpty(targetContainerName) && getCurator().getZookeeperClient().isConnected()) {
            CreateJCloudsContainerMetadata metadata = getContainerCloudMetadata(targetContainerName);
            if (metadata != null) {
            CreateJCloudsContainerOptions options = metadata.getCreateOptions();
                contextName = options.getContextName();
            }
        }

        if (!Strings.isNullOrEmpty(contextName)) {
            for (ComputeService computeService : computeServices) {
                if (computeService.getContext().unwrap().getName().equals(contextName)) {
                    return computeService;
                }
            }
        }

        if (computeServices == null || computeServices.size() == 0) {
            System.out.println("No compute services are available.");
            return null;
        } else if (computeServices != null && computeServices.size() == 0) {
            return computeServices.get(0);
        } else {
            System.out.println("Multiple cloud provider service available. Please select one using the --provider option.");
            return null;
        }
    }

    @Override
    protected Object doExecute() throws Exception {
        if (validateArguments()) {
            ComputeService computeService = findTargetComputeService();

            if (computeService == null) {
                return null;

            }
            Set<String> sourceCidrs = collectCirds();
            FirewallManager firewallManager = firewallManagerFactory.getFirewallManager(computeService);

            NodeMetadata node = null;

            if (!Strings.isNullOrEmpty(targetContainerName) && getCurator().getZookeeperClient().isConnected() && fabricService != null) {
               CreateJCloudsContainerMetadata metadata = getContainerCloudMetadata(targetContainerName);
               if (metadata != null && !Strings.isNullOrEmpty(metadata.getNodeId())) {
                   targetNodeId = metadata.getNodeId();
               }
            }

            if (!Strings.isNullOrEmpty(targetNodeId)) {
              node = computeService.getNodeMetadata(targetNodeId);
            }

            if (node == null) {
                System.err.println("Could not find target node. Make sure you specified either --target-node-id or --target-container using a valid cloud container.");
                return null;
            }
            if (flush) {
                firewallManager.addRule(Rule.create().destination(node).flush());
                return null;
            }
            for (String cidr : sourceCidrs) {
                Rule rule = Rule.create().destination(node).source(cidr);

                if (port != null && port.length > 0) {
                    rule = rule.ports(port);
                }
                if (revoke) {
                    firewallManager.addRule(rule.revoke());
                } else {
                    firewallManager.addRule(rule);
                }
            }
        }
        return null;
    }

    private CreateJCloudsContainerMetadata getContainerCloudMetadata(String name) {
        if (!Strings.isNullOrEmpty(targetContainerName) && getCurator().getZookeeperClient().isConnected() && fabricService != null) {
            Container targetContainer = fabricService.getContainer(targetContainerName);
            if (targetContainer != null && targetContainer.getMetadata() != null) {
                CreateContainerMetadata metadata = targetContainer.getMetadata();
                if (CreateJCloudsContainerMetadata.class.isAssignableFrom(metadata.getClass())) {
                    return (CreateJCloudsContainerMetadata) metadata;
                }
            }
        }
        return null;
    }

    public FirewallManagerFactory getFirewallManagerFactory() {
        return firewallManagerFactory;
    }

    public void setFirewallManagerFactory(FirewallManagerFactory firewallManagerFactory) {
        this.firewallManagerFactory = firewallManagerFactory;
    }

    public List<ComputeService> getComputeServices() {
        return computeServices;
    }

    public void setComputeServices(List<ComputeService> computeServices) {
        this.computeServices = computeServices;
    }
}
