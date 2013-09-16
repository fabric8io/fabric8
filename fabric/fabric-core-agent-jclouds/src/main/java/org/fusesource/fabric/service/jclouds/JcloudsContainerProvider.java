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

package org.fusesource.fabric.service.jclouds;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.ContainerProvider;
import org.fusesource.fabric.api.CreateContainerMetadata;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.internal.ContainerProviderUtils;
import org.fusesource.fabric.service.jclouds.firewall.FirewallManagerFactory;
import org.fusesource.fabric.service.jclouds.functions.ToRunScriptOptions;
import org.fusesource.fabric.service.jclouds.functions.ToTemplate;
import org.fusesource.fabric.service.jclouds.internal.CloudUtils;
import org.fusesource.fabric.service.support.AbstractComponent;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.options.RunScriptOptions;
import org.jclouds.karaf.core.CredentialStore;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.fusesource.fabric.internal.ContainerProviderUtils.buildStartScript;
import static org.fusesource.fabric.internal.ContainerProviderUtils.buildStopScript;

/**
 * A concrete {@link org.fusesource.fabric.api.ContainerProvider} that creates {@link org.fusesource.fabric.api.Container}s via jclouds {@link ComputeService}.
 */
@Component(name = "org.fusesource.fabric.container.provider.jclouds", description = "Fabric Jclouds Container Provider", immediate = true)
@Service(ContainerProvider.class)
public class JcloudsContainerProvider extends AbstractComponent implements ContainerProvider<CreateJCloudsContainerOptions, CreateJCloudsContainerMetadata> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JcloudsContainerProvider.class);

    private static final String NODE_CREATED_FORMAT = "Node %s has been succesfully created.";
    private static final String NODE_ERROR_FORMAT = "Error creating node %s. Status: .";
    private static final String OVERVIEW_FORMAT = "Creating %s nodes on %s. It may take a while ...";


    private static final String SCHEME = "jclouds";

    @Reference(cardinality = OPTIONAL_MULTIPLE, bind = "bindComputeService", unbind = "unbindComputeService", referenceInterface = ComputeService.class, policy = ReferencePolicy.DYNAMIC)
    private final ConcurrentMap<String, ComputeService> computeServiceMap = new ConcurrentHashMap<String, ComputeService>();

    @Reference
    private ComputeRegistry computeRegistry;
    @Reference(referenceInterface = FirewallManagerFactory.class)
    private FirewallManagerFactory firewallManagerFactory;
    @Reference(referenceInterface = CredentialStore.class)
    private CredentialStore credentialStore;
    @Reference(referenceInterface = ConfigurationAdmin.class)
    private ConfigurationAdmin configurationAdmin;
    @Reference(referenceInterface = CuratorFramework.class)
    private CuratorFramework curator;

    private ExecutorService executorService = Executors.newCachedThreadPool();

    @Activate
    synchronized void activate(ComponentContext context) {
        activateComponent(context);
    }

    @Deactivate
    synchronized void deactivate() {
        try {
            executorService.shutdown();
        } finally {
            deactivateComponent();
        }
    }

    public Set<CreateJCloudsContainerMetadata> create(CreateJCloudsContainerOptions input) throws MalformedURLException, RunNodesException, URISyntaxException, InterruptedException {
        Set<? extends NodeMetadata> metadata = null;
        CreateJCloudsContainerOptions options = input.updateComputeService(getOrCreateComputeService(input));
        int number = Math.max(options.getNumber(), 1);
        int suffix = 1;

        final Set<CreateJCloudsContainerMetadata> result = new LinkedHashSet<CreateJCloudsContainerMetadata>();

        try {
            options.getCreationStateListener().onStateChange("Looking up for compute service.");
            ComputeService computeService = getOrCreateComputeService(options);

            if (computeService == null) {
                throw new IllegalStateException("Compute service could not be found or created.");
            }

            Template template = ToTemplate.apply(options);

            options.getCreationStateListener().onStateChange(String.format(OVERVIEW_FORMAT, number, options.getContextName()));

            try {
                metadata = computeService.createNodesInGroup(options.getGroup(), number, template);
                for (NodeMetadata nodeMetadata : metadata) {
                    switch (nodeMetadata.getStatus()) {
                        case RUNNING:
                            options.getCreationStateListener().onStateChange(String.format(NODE_CREATED_FORMAT, nodeMetadata.getName()));
                            break;
                        default:
                            options.getCreationStateListener().onStateChange(String.format(NODE_ERROR_FORMAT, nodeMetadata.getStatus()));
                    }
                }
            } catch (RunNodesException ex) {
                CreateJCloudsContainerMetadata failureMetdata = new CreateJCloudsContainerMetadata();
                failureMetdata.setCreateOptions(options);
                failureMetdata.setFailure(ex);
                result.add(failureMetdata);
                return result;
            }

            String originalName = new String(options.getName());
            CountDownLatch countDownLatch = new CountDownLatch(number);

            for (NodeMetadata nodeMetadata : metadata) {
                String containerName;
                if (options.getNumber() >= 1) {
                    containerName = originalName + (suffix++);
                } else {
                    containerName = originalName;
                }
                CloudContainerInstallationTask installationTask = new CloudContainerInstallationTask(containerName,
                        nodeMetadata, options, computeService, firewallManagerFactory, template.getOptions(), result,
                        countDownLatch);
                executorService.execute(installationTask);
            }

            if (!countDownLatch.await(15, TimeUnit.MINUTES)) {
                throw new FabricException("Error waiting for container installation.");
            }

        } catch (Throwable t) {
                for (int i = result.size(); i < number; i++) {
                    CreateJCloudsContainerMetadata failureMetdata = new CreateJCloudsContainerMetadata();
                    failureMetdata.setCreateOptions(options);
                    failureMetdata.setFailure(t);
                    result.add(failureMetdata);
                }
        }
        return result;
    }

    @Override
    public void start(Container container) {
        CreateContainerMetadata metadata = container.getMetadata();
        if (!(metadata instanceof CreateJCloudsContainerMetadata)) {
            throw new IllegalStateException("Container doesn't have valid create container metadata type");
        } else {
            CreateJCloudsContainerMetadata jCloudsContainerMetadata = (CreateJCloudsContainerMetadata) metadata;
            CreateJCloudsContainerOptions options = jCloudsContainerMetadata.getCreateOptions();
            ComputeService computeService = getOrCreateComputeService(options);
            try {

                String nodeId = jCloudsContainerMetadata.getNodeId();
                Optional<RunScriptOptions> runScriptOptions = ToRunScriptOptions.withComputeService(computeService).apply(jCloudsContainerMetadata);
                String script = buildStartScript(container.getId(), options);
                ExecResponse response;

                if (runScriptOptions.isPresent()) {
                    response = computeService.runScriptOnNode(nodeId, script, runScriptOptions.get());
                } else {
                    response = computeService.runScriptOnNode(nodeId, script);
                }

                if (response == null) {
                    jCloudsContainerMetadata.setFailure(new Exception("No response received for fabric install script."));
                } else if (response.getOutput() != null && response.getOutput().contains(ContainerProviderUtils.FAILURE_PREFIX)) {
                    jCloudsContainerMetadata.setFailure(new Exception(ContainerProviderUtils.parseScriptFailure(response.getOutput())));
                }
            } catch (Throwable t) {
                jCloudsContainerMetadata.setFailure(t);
            }
        }
    }

    @Override
    public void stop(Container container) {
        CreateContainerMetadata metadata = container.getMetadata();
        if (!(metadata instanceof CreateJCloudsContainerMetadata)) {
            throw new IllegalStateException("Container doesn't have valid create container metadata type");
        } else {
            CreateJCloudsContainerMetadata jCloudsContainerMetadata = (CreateJCloudsContainerMetadata) metadata;
            CreateJCloudsContainerOptions options = jCloudsContainerMetadata.getCreateOptions();
            try {
                ComputeService computeService = getOrCreateComputeService(options);
                String nodeId = jCloudsContainerMetadata.getNodeId();
                Optional<RunScriptOptions> runScriptOptions = ToRunScriptOptions.withComputeService(computeService).apply(jCloudsContainerMetadata);
                String script = buildStopScript(container.getId(), options);
                ExecResponse response;

                if (runScriptOptions.isPresent()) {
                    response = computeService.runScriptOnNode(nodeId, script, runScriptOptions.get());
                } else {
                    response = computeService.runScriptOnNode(nodeId, script);
                }

                if (response == null) {
                    jCloudsContainerMetadata.setFailure(new Exception("No response received for fabric install script."));
                } else if (response.getOutput() != null && response.getOutput().contains(ContainerProviderUtils.FAILURE_PREFIX)) {
                    jCloudsContainerMetadata.setFailure(new Exception(ContainerProviderUtils.parseScriptFailure(response.getOutput())));
                }
            } catch (Throwable t) {
                jCloudsContainerMetadata.setFailure(t);
            }
        }
    }

    @Override
    public void destroy(Container container) {
        CreateContainerMetadata metadata = container.getMetadata();
        if (!(metadata instanceof CreateJCloudsContainerMetadata)) {
            throw new IllegalStateException("Container doesn't have valid create container metadata type");
        } else {
            CreateJCloudsContainerMetadata jCloudsContainerMetadata = (CreateJCloudsContainerMetadata) metadata;
            CreateJCloudsContainerOptions options = jCloudsContainerMetadata.getCreateOptions();
            String nodeId = jCloudsContainerMetadata.getNodeId();
            ComputeService computeService = getOrCreateComputeService(options);
            computeService.destroyNode(nodeId);
        }
    }

    /**
     * Gets an existing {@link ComputeService} that matches configuration or creates a new one.
     *
     * @param options
     * @return
     */
    private synchronized ComputeService getOrCreateComputeService(CreateJCloudsContainerOptions options) {
        ComputeService computeService = null;
        if (options != null) {
            Object object = options.getComputeService();
            if (object instanceof ComputeService) {
                computeService = (ComputeService) object;
            }
            if (computeService == null && options.getContextName() != null) {
                computeService = computeRegistry.getIfPresent(options.getContextName());
            }
            if (computeService == null) {
                options.getCreationStateListener().onStateChange("Compute Service not found. Creating ...");
                //validate options and make sure a compute service can be created.
                if (Strings.isNullOrEmpty(options.getProviderName()) || Strings.isNullOrEmpty(options.getIdentity()) || Strings.isNullOrEmpty(options.getCredential())) {
                    throw new IllegalArgumentException("Cannot create compute service. A registered cloud provider or the provider name, identity and credential options are required");
                }

                Map<String, String> serviceOptions = options.getServiceOptions();
                try {
                    if (options.getProviderName() != null) {
                        CloudUtils.registerProvider(curator, configurationAdmin, options.getContextName(), options.getProviderName(), options.getIdentity(), options.getCredential(), serviceOptions);
                    } else if (options.getApiName() != null) {
                        CloudUtils.registerApi(curator, configurationAdmin, options.getContextName(), options.getApiName(), options.getEndpoint(), options.getIdentity(), options.getCredential(), serviceOptions);
                    }
                    BundleContext bundleContext = getComponentContext().getBundleContext();
                    computeService = CloudUtils.waitForComputeService(bundleContext, options.getContextName());
                } catch (Exception e) {
                    LOGGER.warn("Did not manage to register compute cloud provider.");
                }
            }
        }
        return computeService;
    }

    @Override
    public String getScheme() {
        return SCHEME;
    }

    @Override
    public Class<CreateJCloudsContainerOptions> getOptionsType() {
        return CreateJCloudsContainerOptions.class;
    }

    @Override
    public Class<CreateJCloudsContainerMetadata> getMetadataType() {
        return CreateJCloudsContainerMetadata.class;
    }

    public FirewallManagerFactory getFirewallManagerFactory() {
        return firewallManagerFactory;
    }

    public void setFirewallManagerFactory(FirewallManagerFactory firewallManagerFactory) {
        this.firewallManagerFactory = firewallManagerFactory;
    }

    public CredentialStore getCredentialStore() {
        return credentialStore;
    }

    public void setCredentialStore(CredentialStore credentialStore) {
        this.credentialStore = credentialStore;
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public synchronized void bindComputeService(ComputeService computeService) {
        if (computeService != null) {
            String name = computeService.getContext().unwrap().getName();
            if (name != null) {
                computeServiceMap.put(name, computeService);
            }
        }
    }

    public void unbindComputeService(ComputeService computeService) {
        if (computeService != null) {
            String serviceId = computeService.getContext().unwrap().getName();
            if (serviceId != null) {
                computeServiceMap.remove(serviceId);
            }
        }
    }
}
