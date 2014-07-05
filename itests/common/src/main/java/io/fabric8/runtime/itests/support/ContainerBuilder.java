/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.runtime.itests.support;

import io.fabric8.api.Container;
import io.fabric8.api.ContainerRegistration;
import io.fabric8.api.CreateChildContainerOptions;
import io.fabric8.api.CreateContainerBasicOptions;
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricService;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceLocator;

public abstract class ContainerBuilder<T extends ContainerBuilder, B extends CreateContainerBasicOptions.Builder> {

    public static final Long CREATE_TIMEOUT = 10 * 60000L;
    public static final Long PROVISION_TIMEOUT = 5 * 60000L;
    public static final String CONTAINER_TYPE_PROPERTY = "FABRIC_ITEST_CONTAINER_TYPE";
    public static final String CONTAINER_NUMBER_PROPERTY = "FABRIC_ITEST_CONTAINER_NUMBER";

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
        } else {
            return child(1);
        }
    }

    /**
     * Creates a {@link ChildContainerBuilder}.
     */
    public static ChildContainerBuilder child() {
        return child(1);
    }

    /**
     * Creates a {@link ChildContainerBuilder}.
     */
    public static ChildContainerBuilder child(int numberOfContainers) {
        return new ChildContainerBuilder(CreateChildContainerOptions.builder().number(numberOfContainers));
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
        ModuleContext moduleContext = RuntimeLocator.getRequiredRuntime().getModuleContext();
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            CompletionService<Set<Container>> completionService = new ExecutorCompletionService<Set<Container>>(executorService);
            return completionService.submit(new CreateContainerTask(fabricService, builder));
        } finally {
            fabricProxy.close();
        }
    }

    /**
     * Create the containers.
     */
    public Set<Container> build(Collection<B> buildersList) {
        Set<Container> containers = new HashSet<Container>();
        ModuleContext moduleContext = RuntimeLocator.getRequiredRuntime().getModuleContext();
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
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
        } finally {
            fabricProxy.close();
        }
    }

    /**
     * Create the containers.
     */
    public Set<Container> build() {
        ModuleContext moduleContext = RuntimeLocator.getRequiredRuntime().getModuleContext();
        ServiceLocator.awaitService(moduleContext, ContainerRegistration.class);
        return build(Arrays.<B> asList(getOptionsBuilder()));
    }

    /**
     * Destroy the given containers
     */
    public static void destroy(Set<Container> containers) {
        ModuleContext moduleContext = RuntimeLocator.getRequiredRuntime().getModuleContext();
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            for (Container aux : containers) {
                try {
                    //We want to use the latest metadata
                    Container container = fabricService.getContainer(aux.getId());
                    container.destroy(true);
                } catch (Exception ex) {
                    ex.printStackTrace(System.err);
                    //noop
                }
            }
        } finally {
            fabricProxy.close();
        }
    }

    /**
     * Stop the given containers.
     * The container directory will not get deleted.
     */
    public static void stop(Set<Container> containers) {
        ModuleContext moduleContext = RuntimeLocator.getRequiredRuntime().getModuleContext();
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            for (Container aux : containers) {
                try {
                    //We want to use the latest metadata
                    Container updated = fabricService.getContainer(aux.getId());
                    updated.stop(true);
                } catch (Exception ex) {
                    ex.printStackTrace(System.err);
                    //noop
                }
            }
        } finally {
            fabricProxy.close();
        }
    }
}
