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

package io.fabric8.service.jclouds.firewall.internal;

import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.service.jclouds.firewall.ApiFirewallSupport;
import org.jclouds.aws.util.AWSUtils;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.ec2.EC2Api;
import org.jclouds.ec2.EC2ApiMetadata;
import org.jclouds.ec2.compute.EC2ComputeService;
import org.jclouds.ec2.domain.SecurityGroup;
import org.jclouds.ec2.features.SecurityGroupApi;
import org.jclouds.net.domain.IpPermission;
import org.jclouds.net.domain.IpProtocol;
@ThreadSafe
@Component(name = "io.fabric8.jclouds.firewall.ec2", label = "Fabric8 Firewall Support for EC2", immediate = true, metatype = false)
@Service(ApiFirewallSupport.class)
public final class Ec2FirewallSupport extends AbstractComponent implements ApiFirewallSupport {

    private final ApiFirewallSupport delegate = new Ec2SupportDelegate();

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    /**
     * Authorizes access to the specified ports of the node, from the specified source.
     */
    @Override
    public void authorize(ComputeService service, NodeMetadata node, String source, int... ports) {
        assertValid();
        try {
            delegate.authorize(service, node, source, ports);
        } catch (NoClassDefFoundError ex) {
            //ignore
        }
    }

    /**
     * Revokes access to the specified ports of the node, from the specified source.
     */
    @Override
    public void revoke(ComputeService service, NodeMetadata node, String source, int... ports) {
        assertValid();
        try {
            delegate.revoke(service, node, source, ports);
        } catch (NoClassDefFoundError ex) {
            //ignore
        }
    }

    /**
     * Removes all rules.
     */
    @Override
    public void flush(ComputeService service, NodeMetadata node) {
        assertValid();
        delegate.flush(service, node);
    }

    @Override
    public boolean supports(ComputeService computeService) {
        assertValid();
        try {
            return delegate.supports(computeService);
        } catch (NoClassDefFoundError ex) {
            return false;
        }
    }

    private static final class Ec2SupportDelegate implements ApiFirewallSupport {

        /**
         * Authorizes access to the specified ports of the node, from the specified source.
         */
        @Override
        public void authorize(ComputeService service, NodeMetadata node, String source, int... ports) {
            String region = AWSUtils.parseHandle(node.getId())[0];
            EC2Api ec2Api = service.getContext().unwrapApi(EC2Api.class);
            String groupName = "jclouds#" + node.getGroup();
            for (int port : ports) {
                try {
                    ec2Api.getSecurityGroupApi().get()
                        .authorizeSecurityGroupIngressInRegion(region, groupName,
                        IpProtocol.TCP, port, port, source);
                } catch (IllegalStateException e) {
                    //noop
                }
            }
        }

        /**
         * Revokes access to the specified ports of the node, from the specified source.
         */
        @Override
        public void revoke(ComputeService service, NodeMetadata node, String source, int... ports) {
            String region = AWSUtils.parseHandle(node.getId())[0];
            EC2Api ec2Api = service.getContext().unwrapApi(EC2Api.class);
            String groupName = "jclouds#" + node.getGroup() + "#" + region;
            for (int port : ports) {
                try {
                    ec2Api.getSecurityGroupApi().get()
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
            EC2Api ec2Api = service.getContext().unwrapApi(EC2Api.class);
            String groupName = "jclouds#" + node.getGroup() + "#" + region;
            Set<SecurityGroup> matchedSecurityGroups = ec2Api.getSecurityGroupApi().get().describeSecurityGroupsInRegion(region, groupName);
            for (SecurityGroup securityGroup : matchedSecurityGroups) {
                for (IpPermission ipPermission : securityGroup) {
                    for (String cdr : ipPermission.getCidrBlocks()) {
                        ec2Api.getSecurityGroupApi().get().revokeSecurityGroupIngressInRegion(region, groupName,
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
            return EC2ComputeService.class.isInstance(computeService);
        }
    }
}
