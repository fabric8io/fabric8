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

package org.fusesource.fabric.service.jclouds.firewall.internal;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.fabric.service.jclouds.firewall.ApiFirewallSupport;
import org.jclouds.aws.util.AWSUtils;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.ec2.EC2ApiMetadata;
import org.jclouds.ec2.EC2Client;
import org.jclouds.ec2.domain.IpPermission;
import org.jclouds.ec2.domain.IpProtocol;
import org.jclouds.ec2.domain.SecurityGroup;

import java.util.Set;

@Component(name = "org.fusesource.fabric.jclouds.firewall.ec2",
        description = "Fabric Firewall Support for EC2",
        immediate = true)
@Service(ApiFirewallSupport.class)
public class Ec2FirewallSupport implements ApiFirewallSupport {

    ApiFirewallSupport delegate;

    /**
     * Authorizes access to the specified ports of the node, from the specified source.
     *
     * @param service
     * @param node
     * @param source
     * @param ports
     */
    @Override
    public void authorize(ComputeService service, NodeMetadata node, String source, int... ports) {
        try {
            getDelegate().authorize(service, node, source, ports);
        } catch (NoClassDefFoundError ex) {
            //ignore
        }
    }

    /**
     * Revokes access to the specified ports of the node, from the specified source.
     *
     * @param service
     * @param node
     * @param source
     * @param ports
     */
    @Override
    public void revoke(ComputeService service, NodeMetadata node, String source, int... ports) {
        try {
            getDelegate().revoke(service, node, source, ports);
        } catch (NoClassDefFoundError ex) {
            //ignore
        }
    }

    /**
     * Removes all rules.
     *
     * @param service
     * @param node
     */
    @Override
    public void flush(ComputeService service, NodeMetadata node) {
        getDelegate().flush(service, node);
    }

    @Override
    public boolean supports(ComputeService computeService) {
        try {
            return getDelegate().supports(computeService);
        } catch (NoClassDefFoundError ex) {
            return false;
        }
    }

    private synchronized ApiFirewallSupport getDelegate() {
        if (this.delegate == null) {
            this.delegate = new Ec2SupportDelegate();
        }
        return delegate;
    }


    private static final class Ec2SupportDelegate implements ApiFirewallSupport {

        /**
         * Authorizes access to the specified ports of the node, from the specified source.
         *
         * @param service
         * @param node
         * @param source
         * @param ports
         */
        @Override
        public void authorize(ComputeService service, NodeMetadata node, String source, int... ports) {
            String region = AWSUtils.parseHandle(node.getId())[0];
            EC2Client ec2Client = service.getContext().unwrap(EC2ApiMetadata.CONTEXT_TOKEN).getApi();
            String groupName = "jclouds#" + node.getGroup();
            for (int port : ports) {
                try {
                    ec2Client.getSecurityGroupServices()
                            .authorizeSecurityGroupIngressInRegion(region, groupName,
                                    IpProtocol.TCP, port, port, source);
                } catch (IllegalStateException e) {
                    //noop
                }
            }
        }

        /**
         * Revokes access to the specified ports of the node, from the specified source.
         *
         * @param service
         * @param node
         * @param source
         * @param ports
         */
        @Override
        public void revoke(ComputeService service, NodeMetadata node, String source, int... ports) {
            String region = AWSUtils.parseHandle(node.getId())[0];
            EC2Client ec2Client = service.getContext().unwrap(EC2ApiMetadata.CONTEXT_TOKEN).getApi();
            String groupName = "jclouds#" + node.getGroup() + "#" + region;
            for (int port : ports) {
                try {
                    ec2Client.getSecurityGroupServices()
                            .revokeSecurityGroupIngressInRegion(region, groupName,
                                    IpProtocol.TCP, port, port, source);
                } catch (IllegalStateException e) {
                    //noop
                }
            }
        }

        /**
         * Removes all rules.
         */
        @Override
        public void flush(ComputeService service, NodeMetadata node) {
            String region = AWSUtils.parseHandle(node.getId())[0];
            EC2Client ec2Client = service.getContext().unwrap(EC2ApiMetadata.CONTEXT_TOKEN).getApi();
            String groupName = "jclouds#" + node.getGroup() + "#" + region;
            Set<SecurityGroup> matchedSecurityGroups = ec2Client.getSecurityGroupServices().describeSecurityGroupsInRegion(region, groupName);
            for (SecurityGroup securityGroup : matchedSecurityGroups) {
                for (IpPermission ipPermission : securityGroup) {
                    for (String cdr : ipPermission.getIpRanges()) {
                        ec2Client.getSecurityGroupServices().revokeSecurityGroupIngressInRegion(region, groupName,
                                IpProtocol.TCP, ipPermission.getFromPort(), ipPermission.getToPort(),
                                cdr
                        );
                    }
                }
            }
            //We want at least ssh access from everywhere.
            authorize(service, node, "0.0.0.0/0", 22);
        }

        @Override
        public boolean supports(ComputeService computeService) {
            return EC2ApiMetadata.CONTEXT_TOKEN.isAssignableFrom(computeService.getContext().getBackendType());
        }
    }
}
