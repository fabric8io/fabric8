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
package io.fabric8.openshift;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import com.openshift.client.IGearProfile;
import com.openshift.client.OpenShiftTimeoutException;

import io.fabric8.api.FabricException;
import io.fabric8.api.scr.Configurer;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerAutoScaler;
import io.fabric8.api.ContainerAutoScalerFactory;
import io.fabric8.api.ContainerProvider;
import io.fabric8.api.CreationStateListener;
import io.fabric8.api.FabricService;
import io.fabric8.api.NameValidator;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IHttpClient;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.IUser;
import com.openshift.client.cartridge.EmbeddableCartridge;
import com.openshift.client.cartridge.IEmbeddableCartridge;
import com.openshift.client.cartridge.StandaloneCartridge;
import com.openshift.internal.client.GearProfile;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

@ThreadSafe
@Component(name = "io.fabric8.container.provider.openshift",
        configurationPid = "io.fabric8.openshift",
        label = "Fabric8 Openshift Container Provider", policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = false)
@Service(ContainerProvider.class)
public final class OpenshiftContainerProvider extends AbstractComponent implements ContainerProvider<CreateOpenshiftContainerOptions, CreateOpenshiftContainerMetadata>, ContainerAutoScalerFactory {

    public static final String PROPERTY_AUTOSCALE_SERVER_URL = "autoscale.server.url";
    public static final String PROPERTY_AUTOSCALE_LOGIN = "autoscale.login";
    public static final String PROPERTY_AUTOSCALE_PASSWORD = "autoscale.password";
    public static final String PROPERTY_AUTOSCALE_DOMAIN = "autoscale.domain";

    public static final String PREFIX_CARTRIDGE_ID = "id:";

    private static final transient Logger LOG = LoggerFactory.getLogger(OpenshiftContainerProvider.class);
    private static final String SCHEME = "openshift";

    @Reference
    private Configurer configurer;
    @Reference(referenceInterface = IOpenShiftConnection.class, cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    private final ValidatingReference<IOpenShiftConnection> openShiftConnection = new ValidatingReference<IOpenShiftConnection>();
    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    @Reference(referenceInterface = MBeanServer.class)
    private MBeanServer mbeanServer;


    @Property(name="default.cartridge.url", label = "Default Cartridge URL", value = "${default.cartridge.url}")
    private String defaultCartridgeUrl;

    private ObjectName objectName;
    private OpenShiftFacade mbean;

    private final ValidatingReference<Map<String, ?>> configuration = new ValidatingReference<Map<String, ?>>();

    @Activate
    void activate(Map<String, ?> configuration) throws Exception {
        updateConfiguration(configuration);
        configurer.configure(configuration, this);
        activateComponent();
        if (mbeanServer != null) {
            objectName = new ObjectName("io.fabric8:type=OpenShift");
            mbean = new OpenShiftFacade(this);
            if (!mbeanServer.isRegistered(objectName)) {
                mbeanServer.registerMBean(mbean, objectName);
            }
        }
    }

    @Modified
    void modified(Map<String, ?> configuration) throws Exception {
        updateConfiguration(configuration);
        configurer.configure(configuration, this);
    }

    @Deactivate
    void deactivate() throws MBeanRegistrationException, InstanceNotFoundException {
        if (mbeanServer != null) {
            if (mbeanServer.isRegistered(objectName)) {
                mbeanServer.unregisterMBean(objectName);
            }
        }
        deactivateComponent();
    }

    private void updateConfiguration(Map<String, ?> configuration) {
        this.configuration.bind(Collections.unmodifiableMap(new HashMap<String, Object>(configuration)));
    }

    FabricService getFabricService() {
        return fabricService.get();
    }

    Map<String, ?> getConfiguration() {
        return configuration.get();
    }

    @Override
    public CreateOpenshiftContainerOptions.Builder newBuilder() {
        return CreateOpenshiftContainerOptions.builder();
    }

    @Override
    public CreateOpenshiftContainerMetadata create(CreateOpenshiftContainerOptions options, CreationStateListener listener) throws Exception {
        assertValid();
        IUser user = getOrCreateConnection(options).getUser();
        IDomain domain =  getOrCreateDomain(user, options);
        String cartridgeUrl = null;
        Set<String> profiles = options.getProfiles();
        String versionId = options.getVersion();
        Map<String, String> openshiftConfigOverlay = new HashMap<String, String>();
        if (profiles != null && versionId != null) {
            Version version = fabricService.get().getVersion(versionId);
            if (version != null) {
                for (String profileId : profiles) {
                    Profile profile = version.getProfile(profileId);
                    if (profile != null) {
                        Profile overlay = profile.getOverlay();
                        Map<String, String> openshiftConfig = overlay.getConfiguration(OpenShiftConstants.OPENSHIFT_PID);
                        if (openshiftConfig != null)  {
                            openshiftConfigOverlay.putAll(openshiftConfig);
                        }
                    }
                }
            }
            cartridgeUrl = openshiftConfigOverlay.get("cartridge");
        }
        if (cartridgeUrl == null) {
            cartridgeUrl = defaultCartridgeUrl;
        }
        String[] cartridgeUrls = cartridgeUrl.split(" ");
        LOG.info("Creating cartridges: " + cartridgeUrl);
        String standAloneCartridgeUrl = cartridgeUrls[0];
        StandaloneCartridge cartridge;
        if (standAloneCartridgeUrl.startsWith(PREFIX_CARTRIDGE_ID)) {
            cartridge = new StandaloneCartridge(standAloneCartridgeUrl.substring(PREFIX_CARTRIDGE_ID.length()));
        } else {
            cartridge = new StandaloneCartridge(new URL(standAloneCartridgeUrl));
        }

        String zookeeperUrl = fabricService.get().getZookeeperUrl();
        String zookeeperPassword = fabricService.get().getZookeeperPassword();

        Map<String,String> userEnvVars = null;
        if (!options.isEnsembleServer()) {
            userEnvVars = new HashMap<String, String>();
            userEnvVars.put("OPENSHIFT_FUSE_ZOOKEEPER_URL", zookeeperUrl);
            userEnvVars.put("OPENSHIFT_FUSE_ZOOKEEPER_PASSWORD", zookeeperPassword);
            String zkPasswordEncode = System.getProperty("zookeeper.password.encode", "true");
            userEnvVars.put("OPENSHIFT_FUSE_ZOOKEEPER_PASSWORD_ENCODE", zkPasswordEncode);
        }

        String initGitUrl = null;
        int timeout = IHttpClient.NO_TIMEOUT;
        ApplicationScale scale = null;

        String containerName = options.getName();

        long t0 = System.currentTimeMillis();
        IApplication application;
        try {
            application = domain.createApplication(containerName, cartridge, scale, new GearProfile(options.getGearProfile()), initGitUrl, timeout, userEnvVars);
        } catch (OpenShiftTimeoutException e) {
            long t1;
            do {
                Thread.sleep(5000);
                application = domain.getApplicationByName(containerName);
                if (application != null) {
                    break;
                }
                t1 = System.currentTimeMillis();
            } while (t1  - t0 < TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES));
        }
        LOG.info("Created application " + containerName);

        // now lets add all the embedded cartridges
        List<IEmbeddableCartridge> list = new ArrayList<IEmbeddableCartridge>();
        for (int idx = 1,  size = cartridgeUrls.length; idx < size; idx++) {
            String embeddedUrl = cartridgeUrls[idx];
            LOG.info("Adding embedded cartridge: " + embeddedUrl);
            if (embeddedUrl.startsWith(PREFIX_CARTRIDGE_ID)) {
                list.add(new EmbeddableCartridge(embeddedUrl.substring(PREFIX_CARTRIDGE_ID.length())));
            } else {
                list.add(new EmbeddableCartridge(new URL(embeddedUrl)));
            }
        }
        if (!list.isEmpty()) {
            application.addEmbeddableCartridges(list);
        }

        String gitUrl = application.getGitUrl();
        CreateOpenshiftContainerMetadata metadata = new CreateOpenshiftContainerMetadata(domain.getId(), application.getUUID(), application.getCreationLog(), gitUrl);
        metadata.setContainerName(containerName);
        metadata.setCreateOptions(options);
        return metadata;
    }

    @Override
    public void start(Container container) {
        assertValid();
        getContainerApplication(container, true).start();
    }

    @Override
    public void stop(Container container) {
        assertValid();
        getContainerApplication(container, true).stop();
    }

    @Override
    public void destroy(Container container) {
        assertValid();
        IApplication app = getContainerApplication(container, false);
        if (app != null) {
            app.destroy();
        }
    }

    @Override
    public String getScheme() {
        assertValid();
        return SCHEME;
    }

    @Override
    public Class<CreateOpenshiftContainerOptions> getOptionsType() {
        assertValid();
        return CreateOpenshiftContainerOptions.class;
    }

    @Override
    public Class<CreateOpenshiftContainerMetadata> getMetadataType() {
        assertValid();
        return CreateOpenshiftContainerMetadata.class;
    }


    public List<String> getDomains(String serverUrl, String login, String password) {
        List<String> answer = new ArrayList<String>();
        IOpenShiftConnection connection = OpenShiftUtils.createConnection(serverUrl, login, password);
        if (connection != null) {
            List<IDomain> domains = connection.getDomains();
            if (domains != null) {
                for (IDomain domain : domains) {
                    answer.add(domain.getId());
                }
            }
        }
        return answer;
    }

    public List<String> getGearProfiles(String serverUrl, String login, String password) {
        List<String> answer = new ArrayList<String>();
        IOpenShiftConnection connection = OpenShiftUtils.createConnection(serverUrl, login, password);
        if (connection != null) {
            List<IDomain> domains = connection.getDomains();
            if (domains != null) {
                for (IDomain domain : domains) {
                    List<IGearProfile> gearProfiles = domain.getAvailableGearProfiles();
                    for (IGearProfile gearProfile : gearProfiles) {
                        answer.add(gearProfile.getName());
                    }
                    // assume gears are the same on each domain
                    if (!answer.isEmpty()) {
                        break;
                    }
                }
            }
        }
        return answer;
    }

    private IApplication getContainerApplication(Container container, boolean required) {
        IApplication app = null;
        CreateOpenshiftContainerMetadata metadata = OpenShiftUtils.getContainerMetadata(container);
        if (metadata != null) {
            IOpenShiftConnection connection = getOrCreateConnection(metadata.getCreateOptions());
            app = OpenShiftUtils.getApplication(container, metadata, connection);
        }
        if (app == null && required) {
            throw new FabricException("Unable to find OpenShift application for " + container.getId());
        }
        return app;
    }

    /**
     * Gets a {@link IDomain} that matches the specified {@link CreateOpenshiftContainerOptions}.
     * If no domain has been provided in the options the default domain is used. Else one is returned or created.
     */
    private static IDomain getOrCreateDomain(IUser user, CreateOpenshiftContainerOptions options)  {
        if (options.getDomain() == null || options.getDomain().isEmpty()) {
            return user.getDefaultDomain();
        } else {
            return domainExists(user, options.getDomain()) ? user.getDomain(options.getDomain()) : user.createDomain(options.getDomain());
        }
    }

    /**
     * Checks if there is a {@link IDomain} matching the specified domainId.
     */
    private static boolean domainExists(IUser user, String domainId) {
        for (IDomain domain : user.getDomains()) {
            if (domainId.equals(domain.getId())) {
                return true;
            }
        }
        return false;
    }

    private IOpenShiftConnection getOrCreateConnection(CreateOpenshiftContainerOptions options) {
        IOpenShiftConnection connection = openShiftConnection.getOptional();
        if (connection != null) {
            return connection;
        } else {
            return OpenShiftUtils.createConnection(options);
        }
    }

    @Override
    public ContainerAutoScaler createAutoScaler() {
        return new OpenShiftAutoScaler(this);
    }

    void bindOpenShiftConnection(IOpenShiftConnection service) {
        this.openShiftConnection.bind(service);
    }

    void unbindOpenShiftConnection(IOpenShiftConnection service) {
        this.openShiftConnection.unbind(service);
    }

    /**
     * Creates a name validator that checks there isn't an application of the given name already
     */
    NameValidator createNameValidator(CreateOpenshiftContainerOptions options) {
        IUser user = getOrCreateConnection(options).getUser();
        final IDomain domain =  getOrCreateDomain(user, options);
        return new NameValidator() {
            @Override
            public boolean isValid(String name) {
                IApplication application = domain.getApplicationByName(name);
                return application == null;
            }
        };
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }
}
