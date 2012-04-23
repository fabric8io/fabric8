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

import org.fusesource.fabric.service.jclouds.firewall.ProviderFirewallSupport;
import org.jclouds.aws.util.AWSUtils;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.ec2.EC2Client;
import org.jclouds.ec2.domain.IpProtocol;

public class Ec2FirewallSupport implements ProviderFirewallSupport {
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
            new Ec2SupportDelegate().authorize(service, node, source, ports);
        } catch (NoClassDefFoundError ex) {
            ex.printStackTrace(System.out);
            //ignore
        }
    }

    @Override
    public String[] getProviders() {
        return new String[]{"aws-ec2"};
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
            EC2Client ec2Client = EC2Client.class.cast(service.getContext().getProviderSpecificContext().getApi());
            String groupName = "jclouds#" + node.getGroup() + "#" + region;
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

        @Override
        public String[] getProviders() {
            return new String[]{"aws-ec2"};
        }
    }
}
