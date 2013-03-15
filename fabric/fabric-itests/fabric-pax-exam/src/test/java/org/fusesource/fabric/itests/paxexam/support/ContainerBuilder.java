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

import com.google.common.collect.Lists;
import org.fusesource.fabric.api.*;

import java.util.*;
import java.util.concurrent.*;

import static org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator.getOsgiService;

public abstract class ContainerBuilder<T extends ContainerBuilder, O extends CreateContainerOptions> {

    public static final Long CREATE_TIMEOUT = 10 * 60000L;
    public static final Long PROVISION_TIMEOUT = 5 * 60000L;
    public static final String CONTAINER_TYPE_PROPERTY = "FABRIC_ITEST_CONTAINER_TYPE";
    public static final String CONTAINER_NUMBER_PROPERTY = "FABRIC_ITEST_CONTAINER_NUMBER";
    public static final Set<Container> CONTAINERS = new HashSet<Container>();

    private final O createOptions;
    private final List<String> profileNames = Lists.newArrayList("default");
    private boolean waitForProvisioning;
    private boolean assertProvisioningResult;
    private long provisionTimeOut = PROVISION_TIMEOUT;
    private ExecutorService executorService = Executors.newCachedThreadPool();

    protected ContainerBuilder(O createOptions) {
        this.createOptions = createOptions;
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

    public ContainerBuilder<T, O> withName(String name) {
        getCreateOptions().setName(name);
        return this;
    }

    public ContainerBuilder<T, O> withJvmOpts(String jvmOpts) {
        getCreateOptions().setJvmOpts(jvmOpts);
        return this;
    }

    public ContainerBuilder<T, O> withResolver(String resolver) {
        getCreateOptions().setResolver(resolver);
        return this;
    }

    public ContainerBuilder<T, O> withProfiles(String profile) {
        profileNames.add(profile);
        return this;
    }

    public ContainerBuilder<T, O> waitForProvisioning() {
        this.waitForProvisioning = true;
        return this;
    }

    public ContainerBuilder<T, O> assertProvisioningResult() {
        this.assertProvisioningResult = true;
        return this;
    }

    public O getCreateOptions() {
        return createOptions;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public Future<Set<Container>> prepareAsync(CreateSshContainerOptions options) {
        FabricService fabricService = getOsgiService(FabricService.class);
        CompletionService<Set<Container>> completionService = new ExecutorCompletionService<Set<Container>>(executorService);
        return completionService.submit(new CreateContainerTask(fabricService, options));
    }


    /**
     * Create the containers.
     *
     * @param optionList
     * @return
     */
    public Set<Container> build(Collection<CreateContainerOptions> optionList) {
        Set<Container> containers = new HashSet<Container>();
        FabricService fabricService = getOsgiService(FabricService.class);
        CompletionService<Set<Container>> completionService = new ExecutorCompletionService<Set<Container>>(executorService);

        int tasks = 0;
        for (CreateContainerOptions options : optionList) {
            if (!options.isEnsembleServer()) {
                options.setZookeeperUrl(fabricService.getZookeeperUrl());
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
                    Profile profile = fabricService.getProfile(container.getVersion().getName(), profileName);
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
        return build(Arrays.<CreateContainerOptions>asList(getCreateOptions()));
    }

    /**
     * Destroy all containers
     */
    public static void destroy() {
        for (Container c : CONTAINERS) {
            try {
                c.destroy();
            } catch (Exception ex) {
                //noop
            }
        }
    }
}
