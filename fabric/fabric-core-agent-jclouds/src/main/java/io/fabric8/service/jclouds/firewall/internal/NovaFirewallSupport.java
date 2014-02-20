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

import java.util.ArrayList;
import java.util.List;

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
import org.jclouds.javax.annotation.Nullable;
import org.jclouds.net.domain.IpProtocol;
import org.jclouds.openstack.nova.v2_0.NovaApiMetadata;
import org.jclouds.openstack.nova.v2_0.domain.Ingress;
import org.jclouds.openstack.nova.v2_0.domain.SecurityGroup;
import org.jclouds.openstack.nova.v2_0.domain.SecurityGroupRule;
import org.jclouds.openstack.nova.v2_0.extensions.SecurityGroupApi;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

/**
 * An {@link ApiFirewallSupport} implementation for OpenStack Nova.
 * It uses delegation to static inner class to prevent Class loading issues when optional dependencies are
 * not satisfied.
 */
@ThreadSafe
@Component(name = "io.fabric8.jclouds.firewall.nova", label = "Fabric8 Firewall Support for Openstack Nova", immediate = true, metatype = false)
@Service(ApiFirewallSupport.class)
public final class NovaFirewallSupport extends AbstractComponent implements ApiFirewallSupport {

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
            Optional<? extends SecurityGroupApi> securityGroupApi = getSecurityGroup(service, region);

            if (securityGroupApi.isPresent()) {
                String groupName = "jclouds-" + node.getGroup();
                Optional<? extends SecurityGroup> securityGroup = getSecurityGroupForGroup(securityGroupApi.get(), groupName);
                if (securityGroup.isPresent()) {
                    for (int port : ports) {
                        try {
                            securityGroupApi.get().createRuleAllowingCidrBlock(securityGroup.get().getId(),
                                    Ingress.builder()
                                            .ipProtocol(IpProtocol.TCP)
                                            .fromPort(port).toPort(port).build(),
                                    source);
                        } catch (IllegalStateException e) {
                            //noop
                        }
                    }
                }
            }
        }

        /**
         * Revokes access to the specified ports of the node, from the specified source.
         */
        @Override
        public void revoke(ComputeService service, NodeMetadata node, String source, int... ports) {
            String region = AWSUtils.parseHandle(node.getId())[0];
            Optional<? extends SecurityGroupApi> securityGroupApi = getSecurityGroup(service, region);

            if (securityGroupApi.isPresent()) {
                String groupName = "jclouds-" + node.getGroup();
                Optional<? extends SecurityGroup> securityGroup = getSecurityGroupForGroup(securityGroupApi.get(), groupName);
                if (securityGroup.isPresent()) {
                    try {
                        for (SecurityGroupRule rule : getAllRuleMatching(securityGroup.get(), source, ports)) {
                            securityGroupApi.get().deleteRule(rule.getId());
                        }
                    } catch (IllegalStateException e) {
                        //noop
                    }
                }
            }
        }

        /**
         * Removes all rules.
         */
        @Override
        public void flush(ComputeService service, NodeMetadata node) {
            String region = AWSUtils.parseHandle(node.getId())[0];
            Optional<? extends SecurityGroupApi> securityGroupApi = getSecurityGroup(service, region);

            if (securityGroupApi.isPresent()) {
                String groupName = "jclouds-" + node.getGroup();
                Optional<? extends SecurityGroup> securityGroup = getSecurityGroupForGroup(securityGroupApi.get(), groupName);
                if (securityGroup.isPresent()) {
                    try {
                        for (SecurityGroupRule rule : securityGroup.get().getRules()) {
                            securityGroupApi.get().deleteRule(rule.getId());
                        }
                    } catch (IllegalStateException e) {
                        //noop
                    }
                }
            }
            authorize(service, node, "0.0.0.0", 22);
        }

        @Override
        public boolean supports(ComputeService computeService) {
            return NovaApiMetadata.CONTEXT_TOKEN.isAssignableFrom(computeService.getContext().getBackendType());
        }

        /**
         * Returns the @{link SecurityGroupApi} for the target location.
         */
        private static Optional<? extends SecurityGroupApi> getSecurityGroup(ComputeService computeService, String location) {
           return computeService.getContext().unwrap(NovaApiMetadata.CONTEXT_TOKEN)
                    .getApi()
                    .getSecurityGroupExtensionForZone(location);
        }

        /**
         * Returns the {@link SecurityGroup} instance for the target group.
         */
        private static Optional<? extends SecurityGroup> getSecurityGroupForGroup(final SecurityGroupApi securityGroupApi, final String group) {
            return securityGroupApi.list().firstMatch(new Predicate<org.jclouds.openstack.nova.v2_0.domain.SecurityGroup>() {
                @Override
                public boolean apply(org.jclouds.openstack.nova.v2_0.domain.SecurityGroup secGrp) {
                    return secGrp.getName().equals(group);
                }
            });
        }

        /**
         * Returns all the {@link SecurityGroup} rule that match the specified source and ports
         */
        private static Iterable<SecurityGroupRule> getAllRuleMatching(SecurityGroup securityGroup, final String source, final int... ports) {
            return Iterables.filter(securityGroup.getRules(), new Predicate<SecurityGroupRule>() {
                @Override
                public boolean apply(@Nullable SecurityGroupRule input) {
                    if(!rangeFulfilled(input.getFromPort(), input.getToPort(), ports)) {
                        return false;
                    } else if (!input.getIpRange().equals(source)) {
                        return false;
                    }
                    return true;
                }
            });
        }


        /**
         * Checks if a range is fulfilled by a given number of ports.
         */
        private static boolean rangeFulfilled(int from, int to, int... ports) {
            boolean matches = true;
            List<Integer> targetPorts = new ArrayList<Integer>();

            for (int port : ports) {
                targetPorts.add(port);
            }
            for (int port = from; port <= to; port++) {
                matches &= targetPorts.contains(port);
            }
            return matches;
        }
    }
}
