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
package org.fusesource.fabric.openshift;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IHttpClient;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.IUser;
import com.openshift.client.OpenShiftConnectionFactory;
import com.openshift.internal.client.GearProfile;
import com.openshift.internal.client.StandaloneCartridge;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.ContainerProvider;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


@Component(name = "org.fusesource.fabric.container.provider.openshift",
        description = "Fabric Openshift Container Provider",
        immediate = true)
@Service(ContainerProvider.class)
public class OpenshiftContainerProvider implements ContainerProvider<CreateOpenshiftContainerOptions, CreateOpenshiftContainerMetadata> {

    private static final String SCHEME = "openshift";

    private static final String REGISTRY_CART = "https://raw.github.com/jboss-fuse/fuse-registry-openshift-cartridge/master/metadata/manifest.yml";
    private static final String PLAIN_CART = "https://raw.github.com/jboss-fuse/fuse-openshift-cartridge/master/metadata/manifest.yml";

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    private IOpenShiftConnection connection;

    @Override
    public Set<CreateOpenshiftContainerMetadata> create(CreateOpenshiftContainerOptions options) throws Exception {
        Set<CreateOpenshiftContainerMetadata> metadata = new HashSet<CreateOpenshiftContainerMetadata>();
        IUser user = getOrCreateConnection(options).getUser();
        IDomain domain =  getOrCreateDomain(user, options);
        int number = Math.max(options.getNumber(), 1);
        StandaloneCartridge cartridge = options.isEnsembleServer() ? new StandaloneCartridge(REGISTRY_CART) : new StandaloneCartridge(PLAIN_CART);

        // TODO fill these in with the correct ZK stuff & credentials
        Map<String,String> userEnvVars = new HashMap<String, String>();
        userEnvVars.put("MY_FOO", "bar!");

        String initGitUrl = null;
        int timeout = IHttpClient.NO_TIMEOUT;
        ApplicationScale scale = null;

        for (int i = 1; i <= number; i++) {
            IApplication application = domain.createApplication(options.getName(),cartridge, scale, new GearProfile(options.getGearProfile()), initGitUrl, timeout, userEnvVars);
            String containerName = application.getName() + "-" + application.getUUID();
            if (!options.isEnsembleServer()) {
                application.restart();
            }
            CreateOpenshiftContainerMetadata meta = new CreateOpenshiftContainerMetadata(domain.getId(), application.getUUID(), application.getCreationLog());
            meta.setContainerName(containerName);
            meta.setCreateOptions(options);
            metadata.add(meta);
        }
        return metadata;
    }

    @Override
    public void start(Container container) {
        getContainerApplication(container).start();
    }

    @Override
    public void stop(Container container) {
        getContainerApplication(container).stop();
    }

    @Override
    public void destroy(Container container) {
        getContainerApplication(container).destroy();
    }

    @Override
    public String getScheme() {
        return SCHEME;
    }

    @Override
    public Class<CreateOpenshiftContainerOptions> getOptionsType() {
        return CreateOpenshiftContainerOptions.class;
    }

    @Override
    public Class<CreateOpenshiftContainerMetadata> getMetadataType() {
        return CreateOpenshiftContainerMetadata.class;
    }

    private IApplication getContainerApplication(Container container) {
        CreateOpenshiftContainerMetadata metadata = (CreateOpenshiftContainerMetadata) container.getMetadata();
        String containerName = container.getId();
        String applicationName = containerName.substring(0, containerName.lastIndexOf("-"));
        IOpenShiftConnection connection = getOrCreateConnection(metadata.getCreateOptions());
        IDomain domain = connection.getUser().getDomain(metadata.getDomainId());
        return domain.getApplicationByName(applicationName);
    }
    /**
     * Gets a {@link IDomain} that matches the specified {@link CreateOpenshiftContainerOptions}.
     * If no domain has been provided in the options the default domain is used. Else one is returned or created.
     * @param user          The openshift user.
     * @param options       The create options.
     * @return
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
     * @param user          The {@link IUser} to use.
     * @param domainId      The domainId.
     * @return              True if a matching domain is found.
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
        if (connection != null) {
            return connection;
        } else {
            return new OpenShiftConnectionFactory().getConnection("fabric", options.getLogin(), options.getPassword(), options.getServerUrl());
        }
    }
}
