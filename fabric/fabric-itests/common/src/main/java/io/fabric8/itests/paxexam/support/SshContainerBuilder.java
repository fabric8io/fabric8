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
package io.fabric8.itests.paxexam.support;

import io.fabric8.api.Container;
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceLocator;
import io.fabric8.api.ServiceProxy;
import io.fabric8.service.ssh.CreateSshContainerOptions;
import io.fabric8.zookeeper.ZkDefs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.BundleContext;

public class SshContainerBuilder extends ContainerBuilder<SshContainerBuilder, CreateSshContainerOptions.Builder> {

    public static final String SSH_HOSTS_PROPERTY = "FABRIC_ITEST_SSH_HOSTS";
    public static final String SSH_USERS_PROPERTY = "FABRIC_ITEST_SSH_USERS";
    public static final String SSH_PASSWORD_PROPERTY = "FABRIC_ITEST_SSH_PASSWORDS";
    public static final String SSH_RESOLVER_PROPERTY = "FABRIC_ITEST_SSH_RESOLVER";


    /**
     * Creates an ssh based {@link ContainerBuilder} with the specified {@link CreateSshContainerOptions}.
     *
     * @param proxy
     * @param builder
     */
    protected SshContainerBuilder(ServiceProxy<FabricService> proxy, CreateSshContainerOptions.Builder builder) {
        super(proxy, builder.zookeeperPassword("admin"));
    }


    public static CreateSshContainerOptions defaultOptions() {
        CreateSshContainerOptions options = CreateSshContainerOptions.builder().build();
        return options;
    }

    /**
     * Create the containers.
     */
    @Override
    public Set<ContainerProxy> build() {
        BundleContext bundleContext = ContainerBuilder.getBundleContext();
        if (getOptionsBuilder().getHost() == null || getOptionsBuilder().getHost().isEmpty()) {
            FabricService fabricService = ServiceLocator.awaitService(bundleContext, FabricService.class);
            getOptionsBuilder().zookeeperUrl(fabricService.getZookeeperUrl()).zookeeperPassword("admin").proxyUri(fabricService.getMavenRepoURI());

            String hostProperty = System.getProperty(SSH_HOSTS_PROPERTY);
            String userProperty = System.getProperty(SSH_USERS_PROPERTY);
            String passwordProperty = System.getProperty(SSH_PASSWORD_PROPERTY);
            String resolverProperty = System.getProperty(SSH_RESOLVER_PROPERTY, ZkDefs.DEFAULT_RESOLVER);
            if (resolverProperty.isEmpty()) {
                resolverProperty = ZkDefs.DEFAULT_RESOLVER;
            }
            String numberOfContainersProperty = System.getProperty(CONTAINER_NUMBER_PROPERTY, "1");
            int numberOfContainers = Integer.parseInt(numberOfContainersProperty);

            String[] hosts = null;
            String[] usernames = null;
            String[] passwords = null;

            if (hostProperty != null && !hostProperty.isEmpty()) {
                hosts = hostProperty.replaceAll(" ", "").split(",");
            }

            if (userProperty != null && !userProperty.isEmpty()) {
                usernames = userProperty.replaceAll(" ", "").split(",");
            }

            if (passwordProperty != null && !passwordProperty.isEmpty()) {
                passwords = passwordProperty.replaceAll(" ", "").split(",");
            }

            int numberOfHosts = hosts.length;
            int containersPerHost = numberOfContainers > 1 ? numberOfContainers / numberOfHosts : 1;

            List<CreateSshContainerOptions.Builder> optionsList = new ArrayList<CreateSshContainerOptions.Builder>();

            for (int i = 0; i < hosts.length; i++) {
                try {
                    CreateSshContainerOptions.Builder hostOpts = getOptionsBuilder().clone();

                    hostOpts.number(containersPerHost).host(hosts[i]);
                    if (hostOpts.getNumber() > 1) {
                        hostOpts.name(hostOpts.getName() + "-" + i + "-");
                    } else {
                        hostOpts.name(hostOpts.getName() + i);
                    }
                    hostOpts.resolver(resolverProperty);
                    if (usernames.length > i) {
                        hostOpts.setUsername(usernames[i]);
                    }
                    if (passwords.length > i) {
                        hostOpts.setPassword(passwords[i]);
                    }
                    optionsList.add(hostOpts);
                } catch (CloneNotSupportedException e) {
                    throw FabricException.launderThrowable(e);
                }
            }
            return super.build(optionsList);
        } else {
            return super.build();
        }
    }
}