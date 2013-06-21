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

import java.util.Map;
import java.util.Set;
import org.fusesource.fabric.service.jclouds.firewall.ProviderFirewallSupport;
import org.jclouds.aws.util.AWSUtils;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.ec2.EC2Client;
import org.jclouds.ec2.domain.IpPermission;
import org.jclouds.ec2.domain.IpProtocol;
import org.jclouds.ec2.domain.SecurityGroup;

public class Ec2FirewallSupport implements ProviderFirewallSupport {

    ProviderFirewallSupport delegate;

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
            ex.printStackTrace(System.out);
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
            ex.printStackTrace(System.out);
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
    public String[] getProviders() {
        return new String[]{"aws-ec2"};
    }

    private synchronized ProviderFirewallSupport getDelegate() {
        if (this.delegate == null) {
            this.delegate = new Ec2SupportDelegate();
        }
        return delegate;
    }


    private static final class Ec2SupportDelegate implements ProviderFirewallSupport {

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
            EC2Client ec2Client = EC2Client.class.cast(service.getContext().unwrap().getProviderMetadata().getApiMetadata());
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
            EC2Client ec2Client = EC2Client.class.cast(service.getContext().unwrap().getProviderMetadata().getApiMetadata());
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
            EC2Client ec2Client = EC2Client.class.cast(service.getContext().unwrap().getProviderMetadata().getApiMetadata());
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
        public String[] getProviders() {
            return new String[]{"aws-ec2"};
        }
    }
}
