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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.google.common.base.Strings;
import org.apache.commons.io.IOUtils;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.ContainerProvider;
import org.fusesource.fabric.api.CreateContainerMetadata;
import org.fusesource.fabric.api.CreateJCloudsContainerMetadata;
import org.fusesource.fabric.api.CreateJCloudsContainerOptions;
import org.fusesource.fabric.internal.ContainerProviderUtils;
import org.fusesource.fabric.service.jclouds.firewall.FirewallManager;
import org.fusesource.fabric.service.jclouds.firewall.FirewallManagerFactory;
import org.fusesource.fabric.service.jclouds.firewall.FirewallNotSupportedOnProviderException;
import org.fusesource.fabric.service.jclouds.firewall.Rule;
import org.fusesource.fabric.service.jclouds.internal.CloudUtils;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.OsFamily;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.RunScriptOptions;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.domain.Credentials;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.karaf.core.CredentialStore;
import org.jclouds.scriptbuilder.statements.login.AdminAccess;
import org.linkedin.zookeeper.client.IZKClient;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static org.fusesource.fabric.internal.ContainerProviderUtils.buildInstallAndStartScript;
import static org.fusesource.fabric.internal.ContainerProviderUtils.buildStartScript;
import static org.fusesource.fabric.internal.ContainerProviderUtils.buildStopScript;

/**
 * A concrete {@link org.fusesource.fabric.api.ContainerProvider} that creates {@link org.fusesource.fabric.api.Container}s via jclouds {@link ComputeService}.
 */
public class JcloudsContainerProvider implements ContainerProvider<CreateJCloudsContainerOptions, CreateJCloudsContainerMetadata> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JcloudsContainerProvider.class);
    private final ConcurrentMap<String, ComputeService> computeServiceMap = new ConcurrentHashMap<String, ComputeService>();

    private FirewallManagerFactory firewallManagerFactory;
    private CredentialStore credentialStore;
    private ConfigurationAdmin configurationAdmin;
    private IZKClient zooKeeper;
    private BundleContext bundleContext;

    private ServiceReference computeReference = null;

    public synchronized void bind(ComputeService computeService) {
        if (computeService != null) {
            String providerName = computeService.getContext().getProviderSpecificContext().getId();
            if (providerName != null) {
                computeServiceMap.put(providerName, computeService);
            }
        }
    }

    public void unbind(ComputeService computeService) {
        if (computeService != null) {
            String providerName = computeService.getContext().getProviderSpecificContext().getId();
            if (providerName != null) {
                computeServiceMap.remove(providerName);
            }
        }
    }

    public void destroy() {
        if (computeReference != null) {
            bundleContext.ungetService(computeReference);
        }
    }

    public ConcurrentMap<String, ComputeService> getComputeServiceMap() {
        return computeServiceMap;
    }

    public Set<CreateJCloudsContainerMetadata> create(CreateJCloudsContainerOptions options) throws MalformedURLException, RunNodesException, URISyntaxException, InterruptedException {
        final Set<CreateJCloudsContainerMetadata> result = new LinkedHashSet<CreateJCloudsContainerMetadata>();
        try {
            options.getCreationStateListener().onStateChange("Looking up for compute service.");
            ComputeService computeService = getOrCreateComputeService(options);

            if (computeService == null) {
                throw new IllegalStateException("Compute service could not be found or created.");
            }

            TemplateBuilder builder = computeService.templateBuilder();
            builder.any();
            switch (options.getInstanceType()) {
                case Smallest:
                    builder.smallest();
                    break;
                case Biggest:
                    builder.biggest();
                    break;
                case Fastest:
                    builder.fastest();
                    break;
                default:
                    builder.fastest();
            }

            StringBuilder overviewBuilder = new StringBuilder();

            overviewBuilder.append(String.format("Creating %s nodes in the cloud. Using",options.getNumber()));


            //Define ImageId
            if (!Strings.isNullOrEmpty(options.getImageId())) {
                overviewBuilder.append(" image id: ").append(options.getImageId());
                builder.imageId(options.getImageId());
            }
            //or define Image by OS & Version or By ImageId
            else if (!Strings.isNullOrEmpty(options.getOsFamily())) {
                overviewBuilder.append(" operating system: ").append(options.getOsFamily());
                builder.osFamily(OsFamily.fromValue(options.getOsFamily()));
                if (!Strings.isNullOrEmpty(options.getOsVersion())) {
                    overviewBuilder.append(" and version: ");
                    builder.osVersionMatches(options.getOsVersion());
                }
            } else {
                throw new IllegalArgumentException("Required Image id or Operation System and version predicates.");
            }

            overviewBuilder.append(options.getOsVersion()).append(".");

            //Define Location & Hardware
            if (!Strings.isNullOrEmpty(options.getLocationId())) {
                overviewBuilder.append(" On location: ").append(options.getLocationId()).append(".");
                builder.locationId(options.getLocationId());
            }

            if (!Strings.isNullOrEmpty(options.getHardwareId())) {
                builder.hardwareId(options.getHardwareId());
            }

            AdminAccess.Builder adminAccess = AdminAccess.builder();
            TemplateOptions templateOptions = computeService.templateOptions();




            if (!Strings.isNullOrEmpty(options.getPublicKeyFile())) {
                File publicKey = new File(options.getPublicKeyFile());
                if (publicKey.exists()) {
                    adminAccess.adminPublicKey(publicKey);
                } else {
                    templateOptions.runScript(AdminAccess.standard());
                    LOGGER.warn("Public key has been specified file: {} files cannot be found. Ignoring.", publicKey.getAbsolutePath());
                }
            }

            if (!Strings.isNullOrEmpty(options.getUser())) {
                adminAccess.adminUsername(options.getUser());
            }

            templateOptions.runScript(adminAccess.build());
            builder.options(templateOptions);

            Set<? extends NodeMetadata> metadatas = null;
            overviewBuilder.append("It may take a while ...");
            options.getCreationStateListener().onStateChange(overviewBuilder.toString());
            metadatas = computeService.createNodesInGroup(options.getGroup(), options.getNumber(), builder.build());

            if (metadatas != null) {
            for (NodeMetadata metadata : metadatas) {
                options.getCreationStateListener().onStateChange(String.format("Node %s has been created.", metadata.getName()));
            }
            }

            Thread.sleep(5000);

            int suffix = 1;
            if (metadatas != null) {
                String originalName = new String(options.getName());
                for (NodeMetadata nodeMetadata : metadatas) {

                    //Setup firwall for node
                    try {
                        FirewallManager firewallManager = firewallManagerFactory.getFirewallManager(computeService);
                        if (firewallManager.isSupported()) {
                            options.getCreationStateListener().onStateChange("Configuring firewall.");
                            String source = getOriginatingIp();
                            Rule jmxRule = Rule.create().source(source).destination(nodeMetadata).ports(44444, 1099);
                            Rule sshRule = Rule.create().source(source).destination(nodeMetadata).port(8101);
                            Rule httpRule = Rule.create().source("0.0.0.0/0").destination(nodeMetadata).port(8181);
                            Rule zookeeperRule = Rule.create().source(source).destination(nodeMetadata).port(2181);
                            firewallManager.addRules(jmxRule, sshRule, httpRule, zookeeperRule);
                        } else {
                            options.getCreationStateListener().onStateChange(String.format("Skipping firewall configuration. Not supported for provider %s", options.getProviderName()));
                        }
                    } catch (FirewallNotSupportedOnProviderException e) {
                        LOGGER.warn("Firewall manager not supported. Firewall will have to be manually configured.");
                    } catch (IOException e) {
                        LOGGER.warn("Could not lookup originating ip. Firewall will have to be manually configured.", e);
                    }

                    LoginCredentials credentials = nodeMetadata.getCredentials();
                    //For some cloud providers return do not allow shell access to root, so the user needs to be overrided.
                    if (!Strings.isNullOrEmpty(options.getUser()) && credentials != null) {
                        credentials = credentials.toBuilder().user(options.getUser()).build();
                    } else {
                        credentials = nodeMetadata.getCredentials();
                    }
                    String id = nodeMetadata.getId();
                    Set<String> publicAddresses = nodeMetadata.getPublicAddresses();

                    String containerName;
                    if (options.getNumber() > 1) {
                        containerName = originalName + (suffix++);
                    } else {
                        containerName = originalName;
                    }

                    //Make a copy of the addresses, because we don't want to return back a guice implementation of Set.
                    Set<String> copyOfPublicAddresses = new HashSet<String>();
                    for (String publicAddress : publicAddresses) {
                        copyOfPublicAddresses.add(publicAddress);
                    }

                    CreateJCloudsContainerMetadata jCloudsContainerMetadata = new CreateJCloudsContainerMetadata();
                    jCloudsContainerMetadata.setCreateOptions(options);
                    jCloudsContainerMetadata.setNodeId(nodeMetadata.getId());
                    jCloudsContainerMetadata.setContainerName(containerName);
                    jCloudsContainerMetadata.setPublicAddresses(copyOfPublicAddresses);
                    jCloudsContainerMetadata.setHostname(nodeMetadata.getHostname());
                    if (credentials != null) {
                        jCloudsContainerMetadata.setIdentity(credentials.identity);
                        jCloudsContainerMetadata.setCredential(credentials.credential);
                    }

                    Properties addresses = new Properties();
                    if (publicAddresses != null && !publicAddresses.isEmpty()) {
                        String publicAddress = publicAddresses.toArray(new String[publicAddresses.size()])[0];
                        addresses.put("publicip", publicAddress);
                    }

                    options.getSystemProperties().put(ContainerProviderUtils.ADDRESSES_PROPERTY_KEY, addresses);
                    options.getMetadataMap().put(containerName, jCloudsContainerMetadata);


                    try {
                        String script = buildInstallAndStartScript(options.name(containerName));
                        options.getCreationStateListener().onStateChange(String.format("Installing fabric agent on container %s. It might take a while...",containerName));
                        ExecResponse response = null;
                        if (credentials != null) {
                            response = computeService.runScriptOnNode(id, script, templateOptions.overrideLoginCredentials(credentials).runAsRoot(false));
                        } else {
                            response = computeService.runScriptOnNode(id, script, templateOptions);
                        }
                        if (response == null) {
                            jCloudsContainerMetadata.setFailure(new Exception("No response received for fabric install script."));
                        } else if (response.getOutput() != null && response.getOutput().contains(ContainerProviderUtils.FAILURE_PREFIX)) {
                            jCloudsContainerMetadata.setFailure(new Exception(ContainerProviderUtils.parseScriptFailure(response.getOutput())));
                        }
                    } catch (Throwable t) {
                        jCloudsContainerMetadata.setFailure(t);
                    }
                    //Cleanup addresses.
                    options.getSystemProperties().clear();
                    result.add(jCloudsContainerMetadata);
                }
            }
        } catch (Throwable t) {
            if (options != null && options.getNumber() > 0) {
                for (int i = result.size(); i < options.getNumber(); i++) {
                    CreateJCloudsContainerMetadata failureMetdata = new CreateJCloudsContainerMetadata();
                    failureMetdata.setFailure(t);
                    result.add(failureMetdata);
                }
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
            try {
                Credentials credentials = new Credentials(jCloudsContainerMetadata.getIdentity(), jCloudsContainerMetadata.getCredential());
                String nodeId = jCloudsContainerMetadata.getNodeId();
                ComputeService computeService = getOrCreateComputeService(options);
                String script = buildStartScript(options.name(container.getId()));
                ExecResponse response = null;
                if (credentials != null) {
                    response = computeService.runScriptOnNode(nodeId, script, RunScriptOptions.Builder.overrideCredentialsWith(credentials).runAsRoot(false));
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
                Credentials credentials = new Credentials(jCloudsContainerMetadata.getIdentity(), jCloudsContainerMetadata.getCredential());
                String nodeId = jCloudsContainerMetadata.getNodeId();
                ComputeService computeService = getOrCreateComputeService(options);
                String script = buildStopScript(options.name(container.getId()));
                ExecResponse response = null;
                if (credentials != null) {
                    response = computeService.runScriptOnNode(nodeId, script, RunScriptOptions.Builder.overrideCredentialsWith(credentials).runAsRoot(false));
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

    public Map<String, String> parseQuery(String uri) throws URISyntaxException {
        //TODO: This is copied form URISupport. We should move URISupport to core so that we don't have to copy stuff arround.
        try {
            Map<String, String> rc = new HashMap<String, String>();
            if (uri != null) {
                String[] parameters = uri.split("&");
                for (int i = 0; i < parameters.length; i++) {
                    int p = parameters[i].indexOf("=");
                    if (p >= 0) {
                        String name = URLDecoder.decode(parameters[i].substring(0, p), "UTF-8");
                        String value = URLDecoder.decode(parameters[i].substring(p + 1), "UTF-8");
                        rc.put(name, value);
                    } else {
                        rc.put(parameters[i], null);
                    }
                }
            }
            return rc;
        } catch (UnsupportedEncodingException e) {
            throw (URISyntaxException) new URISyntaxException(e.toString(), "Invalid encoding").initCause(e);
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
            if (computeService == null) {
                computeService = computeServiceMap.get(options.getProviderName());
            }
            if (computeService == null) {
                options.getCreationStateListener().onStateChange("Compute Service not found. Creating ...");
                //validate options and make sure a compute service can be created.
                if (Strings.isNullOrEmpty(options.getProviderName()) || Strings.isNullOrEmpty(options.getIdentity()) || Strings.isNullOrEmpty(options.getCredential())){
                    throw new IllegalArgumentException("Cannot create compute service. A registered cloud provider or the provider name, identity and credential options are required");
                }

                Map<String, String> serviceOptions = options.getServiceOptions();
                try {
                    CloudUtils.registerProvider(zooKeeper, configurationAdmin, options.getProviderName(), options.getIdentity(), options.getCredential(), serviceOptions);
                    computeService = waitForComputeService(options.getProviderName());
                } catch (Exception e) {
                    LOGGER.warn("Did not manage to register compute cloud provider.");
                    return computeService;
                }
            }
        }
        return computeService;
    }

    /**
     * @return the IP address of the client on which this code is running.
     * @throws java.io.IOException
     */
    private String getOriginatingIp() throws IOException {
        URL url = new URL("http://checkip.amazonaws.com/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();
        return IOUtils.toString(connection.getInputStream()).trim() + "/32";
    }

    private String readFile(String path) {
        byte[] bytes = null;
        FileInputStream fin = null;

        File file = new File(path);
        if (path != null && file.exists()) {
            try {
                fin = new FileInputStream(file);
                bytes = new byte[(int) file.length()];
                fin.read(bytes);
            } catch (IOException e) {
                LOGGER.warn("Error reading file {}.", path);
            } finally {
                if (fin != null) {
                    try {
                        fin.close();
                    } catch (Exception ex) {
                    }
                }
            }
        }
        return new String(bytes);
    }

    public synchronized ComputeService waitForComputeService(String provider) {
        ComputeService computeService = null;
        try {
            for (int r = 0; r < 6; r++) {
                ServiceReference[] references = bundleContext.getAllServiceReferences(ComputeService.class.getName(), "(provider=" + provider + ")");
                if (references != null && references.length > 0) {
                    computeService = (ComputeService) bundleContext.getService(references[0]);
                    return computeService;
                }
                Thread.sleep(10000L);
            }
        } catch (Exception e) {
            LOGGER.error("Error while waiting for service.", e);
        }
        return computeService;
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

    public IZKClient getZooKeeper() {
        return zooKeeper;
    }

    public void setZooKeeper(IZKClient zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
