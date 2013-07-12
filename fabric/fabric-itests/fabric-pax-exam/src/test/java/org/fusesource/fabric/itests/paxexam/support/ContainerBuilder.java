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

import java.util.*;
import java.util.concurrent.*;

import static org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator.getOsgiService;

public abstract class ContainerBuilder<T extends ContainerBuilder, B extends CreateContainerBasicOptions.Builder> {

    public static final Long CREATE_TIMEOUT = 10 * 60000L;
    public static final Long PROVISION_TIMEOUT = 5 * 60000L;
    public static final String CONTAINER_TYPE_PROPERTY = "FABRIC_ITEST_CONTAINER_TYPE";
    public static final String CONTAINER_NUMBER_PROPERTY = "FABRIC_ITEST_CONTAINER_NUMBER";
    public static final Set<Container> CONTAINERS = new HashSet<Container>();

    private final B optionsBuilder;
    private final List<String> profileNames = new LinkedList<String>();
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
        return new ChildContainerBuilder(CreateContainerOptionsBuilder.child().number(numberOfContainers));
    }


    /**
     * Creates a {@link JcloudsContainerBuilder}.
     *
     * @return
     */
    public static JcloudsContainerBuilder jclouds() {
        return new JcloudsContainerBuilder(CreateContainerOptionsBuilder.jclouds());
    }

    /**
     * Creates a {@link JcloudsContainerBuilder}
     *
     * @param numberOfContainers The number of {@link Container}s the builder will create.
     * @return
     */
    public static JcloudsContainerBuilder jclouds(int numberOfContainers) {
        return new JcloudsContainerBuilder(CreateContainerOptionsBuilder.jclouds().number(numberOfContainers));
    }

    /**
     * Creates a {@link SshContainerBuilder}.
     *
     * @return
     */
    public static SshContainerBuilder ssh() {
        return new SshContainerBuilder(CreateContainerOptionsBuilder.ssh());
    }

    /**
     * Creates an {@link SshContainerBuilder}.
     *
     * @param numberOfContainers The number of contaienrs the builder will create.
     * @return
     */
    public static SshContainerBuilder ssh(int numberOfContainers) {
        return new SshContainerBuilder(CreateContainerOptionsBuilder.ssh().number(1));
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
        FabricService fabricService = getOsgiService(FabricService.class);
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
        FabricService fabricService = getOsgiService(FabricService.class);
        CompletionService<Set<Container>> completionService = new ExecutorCompletionService<Set<Container>>(executorService);

        int tasks = 0;
        for (B options : buildersList) {
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

            for (Container container : containers) {
                Version version = fabricService.getDefaultVersion();
                container.setVersion(version);
                Set<Profile> profiles = new HashSet(Arrays.asList(container.getProfiles()));
                for (String profileName : profileNames) {
                    Profile profile = container.getVersion().getProfile(profileName);
                    profiles.add(profile);
                }
                container.setProfiles(profiles.toArray(new Profile[profiles.size()]));
            }
            try {
                if (waitForProvisioning) {
                    Provision.waitForContainerStatus(containers, provisionTimeOut);
                }
                if (assertProvisioningResult) {
                    Provision.assertSuccess(containers, provisionTimeOut);
                }
            } catch (Exception e) {
                throw new FabricException(e);
            }
        } catch (Exception e) {
            throw new FabricException(e);
        }
        return containers;
    }

    /**
     * Create the containers.
     *
     * @return
     */
    public Set<Container> build() {
        return build(Arrays.<B>asList(getOptionsBuilder()));
    }

    /**
     * Destroy all containers
     */
    public static void destroy() {
        FabricService fabricService = getOsgiService(FabricService.class);
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
        FabricService fabricService = getOsgiService(FabricService.class);
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
