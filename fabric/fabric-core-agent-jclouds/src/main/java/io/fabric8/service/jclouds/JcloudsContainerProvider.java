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

package io.fabric8.service.jclouds;

import static io.fabric8.internal.ContainerProviderUtils.buildStartScript;
import static io.fabric8.internal.ContainerProviderUtils.buildStopScript;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerProvider;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.CreationStateListener;
import io.fabric8.api.jcip.GuardedBy;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.api.visibility.VisibleForExternal;
import io.fabric8.internal.ContainerProviderUtils;
import io.fabric8.service.jclouds.firewall.FirewallManagerFactory;
import io.fabric8.service.jclouds.functions.ToRunScriptOptions;
import io.fabric8.service.jclouds.functions.ToTemplate;
import io.fabric8.service.jclouds.internal.CloudUtils;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.options.RunScriptOptions;
import org.jclouds.karaf.core.CredentialStore;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

/**
 * A concrete {@link io.fabric8.api.ContainerProvider} that creates {@link io.fabric8.api.Container}s via jclouds {@link ComputeService}.
 */
@ThreadSafe
@Component(name = "io.fabric8.container.provider.jclouds", label = "Fabric8 Jclouds Container Provider", immediate = true, metatype = false)
@Service(ContainerProvider.class)
public class JcloudsContainerProvider extends AbstractComponent implements ContainerProvider<CreateJCloudsContainerOptions, CreateJCloudsContainerMetadata> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JcloudsContainerProvider.class);

    private static final String NODE_CREATED_FORMAT = "Node %s has been succesfully created.";
    private static final String NODE_ERROR_FORMAT = "Error creating node %s. Status: .";
    private static final String OVERVIEW_FORMAT = "Creating %s nodes on %s. It may take a while ...";


    private static final String SCHEME = "jclouds";

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, bind = "bindComputeService", unbind = "unbindComputeService", referenceInterface = ComputeService.class, policy = ReferencePolicy.DYNAMIC)
    private final ConcurrentMap<String, ComputeService> computeServiceMap = new ConcurrentHashMap<String, ComputeService>();

    @Reference(referenceInterface = ComputeRegistry.class)
    private final ValidatingReference<ComputeRegistry> computeRegistry = new ValidatingReference<ComputeRegistry>();
    @Reference(referenceInterface = FirewallManagerFactory.class)
    private final ValidatingReference<FirewallManagerFactory> firewallManagerFactory = new ValidatingReference<FirewallManagerFactory>();
    @Reference(referenceInterface = CredentialStore.class)
    private final ValidatingReference<CredentialStore> credentialStore = new ValidatingReference<CredentialStore>();
    @Reference(referenceInterface = ConfigurationAdmin.class)
    private final ValidatingReference<ConfigurationAdmin> configAdmin = new ValidatingReference<ConfigurationAdmin>();
    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();

    @GuardedBy("volatile & assertValid()") private volatile BundleContext bundleContext;

    @Activate
    void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public CreateJCloudsContainerOptions.Builder newBuilder() {
        return CreateJCloudsContainerOptions.builder();
    }

    @Override
    public CreateJCloudsContainerMetadata create(CreateJCloudsContainerOptions input, CreationStateListener listener) throws MalformedURLException, RunNodesException, URISyntaxException, InterruptedException {
        assertValid();
        CreateJCloudsContainerOptions options = input.updateComputeService(getOrCreateComputeService(input));

        listener.onStateChange("Looking up for compute service.");
        ComputeService computeService = getOrCreateComputeService(options);
        if (computeService == null) {
            throw new IllegalStateException("Compute service could not be found or created.");
        }

        Template template = ToTemplate.apply(options);

        listener.onStateChange(String.format(OVERVIEW_FORMAT, 1, options.getContextName()));

        try {
            Set<? extends NodeMetadata> metadata = computeService.createNodesInGroup(options.getGroup(), 1, template);
            if (metadata == null || metadata.size() != 1) {
                throw new IllegalStateException("JClouds created " + metadata.size() + " containers instead of 1");
            }
            NodeMetadata nodeMetadata = metadata.iterator().next();
            switch (nodeMetadata.getStatus()) {
                case RUNNING:
                    listener.onStateChange(String.format(NODE_CREATED_FORMAT, nodeMetadata.getName()));
                    break;
                default:
                    listener.onStateChange(String.format(NODE_ERROR_FORMAT, nodeMetadata.getStatus()));
            }

            CloudContainerInstallationTask installationTask = new CloudContainerInstallationTask(
                    options.getName(),
                    nodeMetadata,
                    options,
                    computeService,
                    firewallManagerFactory.get(),
                    template.getOptions(),
                    listener);
            return installationTask.install();
        } catch (Throwable ex) {
            CreateJCloudsContainerMetadata failureMetadata = new CreateJCloudsContainerMetadata();
            failureMetadata.setCreateOptions(options);
            failureMetadata.setFailure(ex);
            return failureMetadata;
        }
    }

    @Override
    public void start(Container container) {
        assertValid();
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
        assertValid();
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
        assertValid();
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
     */
    private synchronized ComputeService getOrCreateComputeService(CreateJCloudsContainerOptions options) {
        ComputeService computeService = null;
        if (options != null) {
            computeService = options.getComputeService();
            if (computeService == null && options.getContextName() != null) {
                computeService = computeRegistry.get().getIfPresent(options.getContextName());
            }
            if (computeService == null) {
//                listener.onStateChange("Compute Service not found. Creating ...");
                //validate options and make sure a compute service can be created.
                if (Strings.isNullOrEmpty(options.getProviderName()) || Strings.isNullOrEmpty(options.getIdentity()) || Strings.isNullOrEmpty(options.getCredential())) {
                    throw new IllegalArgumentException("Cannot create compute service. A registered cloud provider or the provider name, identity and credential options are required");
                }

                Map<String, String> serviceOptions = options.getServiceOptions();
                try {
                    if (options.getProviderName() != null) {
                        CloudUtils.registerProvider(curator.get(), configAdmin.get(), options.getContextName(), options.getProviderName(), options.getIdentity(), options.getCredential(), serviceOptions);
                    } else if (options.getApiName() != null) {
                        CloudUtils.registerApi(curator.get(), configAdmin.get(), options.getContextName(), options.getApiName(), options.getEndpoint(), options.getIdentity(), options.getCredential(), serviceOptions);
                    }
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

    @VisibleForExternal
    public void bindFirewallManagerFactory(FirewallManagerFactory factory) {
        this.firewallManagerFactory.bind(factory);
    }

    @VisibleForExternal
    public void unbindFirewallManagerFactory(FirewallManagerFactory factory) {
        this.firewallManagerFactory.unbind(factory);
    }

    void bindCredentialStore(CredentialStore credentialStore) {
        this.credentialStore.bind(credentialStore);
    }

    void unbindCredentialStore(CredentialStore credentialStore) {
        this.credentialStore.unbind(credentialStore);
    }

    void bindComputeRegistry(ComputeRegistry service) {
        this.computeRegistry.bind(service);
    }

    void unbindComputeRegistry(ComputeRegistry service) {
        this.computeRegistry.unbind(service);
    }

    void bindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.bind(service);
    }

    void unbindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.unbind(service);
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }

    void bindComputeService(ComputeService computeService) {
        String name = computeService.getContext().unwrap().getName();
        if (name != null) {
            computeServiceMap.put(name, computeService);
        }
    }

    void unbindComputeService(ComputeService computeService) {
        String serviceId = computeService.getContext().unwrap().getName();
        if (serviceId != null) {
            computeServiceMap.remove(serviceId);
        }
    }
}
