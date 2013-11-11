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

import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.ContainerRegistration;
import org.fusesource.fabric.api.CreateChildContainerOptions;
import org.fusesource.fabric.api.CreateContainerBasicOptions;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.service.jclouds.CreateJCloudsContainerOptions;
import org.fusesource.fabric.service.ssh.CreateSshContainerOptions;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator.getOsgiService;
import static org.fusesource.fabric.itests.paxexam.support.ServiceProxy.getOsgiServiceProxy;

public abstract class ContainerBuilder<T extends ContainerBuilder, B extends CreateContainerBasicOptions.Builder> {

    public static final Long CREATE_TIMEOUT = 10 * 60000L;
    public static final Long PROVISION_TIMEOUT = 5 * 60000L;
    public static final String CONTAINER_TYPE_PROPERTY = "FABRIC_ITEST_CONTAINER_TYPE";
    public static final String CONTAINER_NUMBER_PROPERTY = "FABRIC_ITEST_CONTAINER_NUMBER";
    public static final Set<Container> CONTAINERS = new HashSet<Container>();

    private final B optionsBuilder;
    private final Set<String> profileNames = new HashSet<String>();
    private boolean waitForProvisioning;
    private boolean assertProvisioningResult;
    private long provisionTimeOut = PROVISION_TIMEOUT;
    private ExecutorService executorService = Executors.newCachedThreadPool();

    protected ContainerBuilder(B optionsBuilder) {
        this.optionsBuilder = optionsBuilder;
    }

    public static ContainerBuilder create() {
        return create(1);
    }

    public static ContainerBuilder create(int minimumNumber) {
        return create(minimumNumber, 0);
    }

    public static ContainerBuilder create(int minimumNumber, int maximumNumber) {
        String containerType = System.getProperty(CONTAINER_TYPE_PROPERTY, "child");
        int numberOfContainers = Math.max(minimumNumber, Integer.parseInt(System.getProperty(CONTAINER_NUMBER_PROPERTY, "1")));

        if (maximumNumber < numberOfContainers && maximumNumber != 0) {
            numberOfContainers = minimumNumber;
        }

        if ("child".equals(containerType)) {
            return child(numberOfContainers);
        } else if ("jclouds".equals(containerType)) {
            return jclouds(numberOfContainers);
        } else if ("ssh".equals(containerType)) {
            return ssh(numberOfContainers);
        } else {
            return child(1);
        }
    }

    /**
     * Creates a {@link ChildContainerBuilder}.
     *
     * @return
     */
    public static ChildContainerBuilder child() {
        return child(1);
    }

    /**
     * Creates a {@link ChildContainerBuilder}.
     *
     * @param numberOfContainers The number of {@link Container}s that the builder will create.
     * @return
     */
    public static ChildContainerBuilder child(int numberOfContainers) {
        return new ChildContainerBuilder(CreateChildContainerOptions.builder().number(numberOfContainers));
    }


    /**
     * Creates a {@link JcloudsContainerBuilder}.
     *
     * @return
     */
    public static JcloudsContainerBuilder jclouds() {
        return new JcloudsContainerBuilder(CreateJCloudsContainerOptions.builder());
    }

    /**
     * Creates a {@link JcloudsContainerBuilder}
     *
     * @param numberOfContainers The number of {@link Container}s the builder will create.
     * @return
     */
    public static JcloudsContainerBuilder jclouds(int numberOfContainers) {
        return new JcloudsContainerBuilder(CreateJCloudsContainerOptions.builder().number(numberOfContainers));
    }

    /**
     * Creates a {@link SshContainerBuilder}.
     *
     * @return
     */
    public static SshContainerBuilder ssh() {
        return new SshContainerBuilder(CreateSshContainerOptions.builder());
    }

    /**
     * Creates an {@link SshContainerBuilder}.
     *
     * @param numberOfContainers The number of contaienrs the builder will create.
     * @return
     */
    public static SshContainerBuilder ssh(int numberOfContainers) {
        return new SshContainerBuilder(CreateSshContainerOptions.builder().number(1));
    }

    public ContainerBuilder<T, B> withName(String name) {
        getOptionsBuilder().name(name);
        return this;
    }

    public ContainerBuilder<T, B> withJvmOpts(String jvmOpts) {
        getOptionsBuilder().jvmOpts(jvmOpts);
        return this;
    }

    public ContainerBuilder<T, B> withResolver(String resolver) {
        getOptionsBuilder().resolver(resolver);
        return this;
    }

    public ContainerBuilder<T, B> withProfiles(String profile) {
        profileNames.add(profile);
        return this;
    }

    public ContainerBuilder<T, B> waitForProvisioning() {
        this.waitForProvisioning = true;
        return this;
    }

    public ContainerBuilder<T, B> assertProvisioningResult() {
        this.assertProvisioningResult = true;
        return this;
    }

    public B getOptionsBuilder() {
        return optionsBuilder;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public Future<Set<Container>> prepareAsync(B builder) {
        FabricService fabricService = getOsgiServiceProxy(FabricService.class);
        CompletionService<Set<Container>> completionService = new ExecutorCompletionService<Set<Container>>(executorService);
        return completionService.submit(new CreateContainerTask(fabricService, builder));
    }


    /**
     * Create the containers.
     *
     * @param buildersList
     * @return
     */
    public Set<Container> build(Collection<B> buildersList) {
        Set<Container> containers = new HashSet<Container>();
        FabricService fabricService = getOsgiServiceProxy(FabricService.class);
        CompletionService<Set<Container>> completionService = new ExecutorCompletionService<Set<Container>>(executorService);

        int tasks = 0;
        for (B options : buildersList) {
            options.profiles(profileNames);
            if (!options.isEnsembleServer()) {
                options.zookeeperUrl(fabricService.getZookeeperUrl());
                completionService.submit(new CreateContainerTask(fabricService, options));
                tasks++;
            }
        }

        try {
            for (int i = 0; i < tasks; i++) {
                Future<Set<Container>> futureContainerSet = completionService.poll(CREATE_TIMEOUT, TimeUnit.MILLISECONDS);
                Set<Container> containerSet = futureContainerSet.get();
                CONTAINERS.addAll(containerSet);
                containers.addAll(containerSet);
            }

            try {
                if (waitForProvisioning) {
                    Provision.containerStatus(containers, provisionTimeOut);
                }
                if (assertProvisioningResult) {
                    Provision.provisioningSuccess(containers, provisionTimeOut);
                }
            } catch (Exception e) {
                throw FabricException.launderThrowable(e);
            }
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
        return containers;
    }

    /**
     * Create the containers.
     *
     * @return
     */
    public Set<Container> build() {
        getOsgiService(ContainerRegistration.class);
        return build(Arrays.<B>asList(getOptionsBuilder()));
    }

    /**
     * Destroy all containers
     */
    public static void destroy() {
        FabricService fabricService = getOsgiServiceProxy(FabricService.class);
        for (Container c : CONTAINERS) {
            try {
                //We want to use the latest metadata
                Container updated = fabricService.getContainer(c.getId());
                updated.destroy();
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
                //noop
            }
        }
    }

    /**
     * Stop all containers.
     * The container directory will not get deleted.
     */
    public static void stop() {
        FabricService fabricService = getOsgiServiceProxy(FabricService.class);
        for (Container c : CONTAINERS) {
            try {
                //We want to use the latest metadata
                Container updated = fabricService.getContainer(c.getId());
                updated.stop();
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
                //noop
            }
        }
    }
}
