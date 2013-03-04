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
package org.fusesource.fabric.itests.paxexam.support;

import org.fusesource.fabric.api.*;
import org.fusesource.fabric.zookeeper.ZkDefs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator.getOsgiService;

public class SshContainerBuilder extends ContainerBuilder<SshContainerBuilder, CreateSshContainerOptions> {

    public static final String SSH_HOSTS_PROPERTY = "FABRIC_ITEST_SSH_HOSTS";
    public static final String SSH_USERS_PROPERTY = "FABRIC_ITEST_SSH_USERS";
    public static final String SSH_PASSWORD_PROPERTY = "FABRIC_ITEST_SSH_PASSWORDS";
    public static final String SSH_RESOLVER_PROPERTY = "FABRIC_ITEST_SSH_RESOLVER";


    /**
     * Creates an ssh based {@link ContainerBuilder} with the specified {@link CreateSshContainerOptions}.
     *
     * @param createOptions
     */
    protected SshContainerBuilder(CreateSshContainerOptions createOptions) {
        super(createOptions.zookeeperPassword("admin"));
    }


    public static CreateSshContainerOptions defaultOptions() {
        CreateSshContainerOptions options = new CreateSshContainerOptions();
        return options;
    }

    /**
     * Create the containers.
     *
     * @return
     */
    @Override
    public Set<Container> build() {
        if (getCreateOptions().getHost() == null || getCreateOptions().getHost().isEmpty()) {
            Set<Container> containers = new HashSet<Container>();
            FabricService fabricService = getOsgiService(FabricService.class);
            getCreateOptions().zookeeperUrl(fabricService.getZookeeperUrl()).zookeeperPassword("admin").proxyUri(fabricService.getMavenRepoURI());

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

            List<CreateContainerOptions> optionsList = new ArrayList<CreateContainerOptions>();
            for (int i = 0; i < hosts.length; i++) {
                try {
                    CreateSshContainerOptions hostOpts = getCreateOptions().clone();
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
                    throw new FabricException(e);
                }
            }
            return super.build(optionsList);
        } else {
            return super.build();
        }
    }
}