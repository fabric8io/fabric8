/*
 * Copyright (C) 2011 FuseSource, Corp. All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the CDDL license
 * a copy of which has been included with this distribution in the license.txt file.
 */

package org.fusesource.fabric.service.jclouds;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import org.fusesource.fabric.api.AgentProvider;
import org.fusesource.fabric.api.CreateAgentArguments;
import org.fusesource.fabric.api.CreateJCloudsAgentArguments;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.JCloudsInstanceType;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.ComputeServiceContextFactory;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.RunScriptOptions;
import org.jclouds.domain.Credentials;
import org.jclouds.rest.RestContextFactory;


import static org.fusesource.fabric.internal.AgentProviderUtils.DEFAULT_SSH_PORT;
import static org.fusesource.fabric.internal.AgentProviderUtils.buildStartupScript;

/**
 * A concrete {@link AgentProvider} that creates {@link org.fusesource.fabric.api.Agent}s via jclouds {@link ComputeService}.
 */
public class JcloudsAgentProvider implements AgentProvider {

    private static final String IMAGE_ID = "imageId";
    private static final String LOCATION_ID = "locationId";
    private static final String HARDWARE_ID = "hardwareId";
    private static final String USER = "user";
    private static final String GROUP = "group";

    private static final String INSTANCE_TYPE = "instanceType";

    private final ConcurrentMap<String, ComputeService> computeServiceMap = new ConcurrentHashMap<String, ComputeService>();

    public void bind(ComputeService computeService) {
        if(computeService != null) {
            String providerName = computeService.getContext().getProviderSpecificContext().getId();
            if(providerName != null) {
              computeServiceMap.put(providerName,computeService);
            }
        }
    }

    public void unbind(ComputeService computeService) {
        if(computeService != null) {
            String providerName = computeService.getContext().getProviderSpecificContext().getId();
            if(providerName != null) {
               computeServiceMap.remove(providerName);
            }
        }
    }

    public ConcurrentMap<String, ComputeService> getComputeServiceMap() {
        return computeServiceMap;
    }

    /**
     * Creates an {@link org.fusesource.fabric.api.Agent} with the given name pointing to the specified zooKeeperUrl.
     * @param proxyUri     The uri of the maven proxy to use.
     * @param agentUri     The uri that contains required information to build the Agent.
     * @param name         The name of the Agent.
     * @param zooKeeperUrl The url of Zoo Keeper.
     */
    public void create(URI proxyUri, URI agentUri, String name, String zooKeeperUrl, boolean debugAgent) {
           create(proxyUri, agentUri,name,zooKeeperUrl,debugAgent,1);
    }

    /**
     * Creates an {@link org.fusesource.fabric.api.Agent} with the given name pointing to the specified zooKeeperUrl.
     * @param proxyUri      The uri of the maven proxy to use.
     * @param agentUri      The uri that contains required information to build the Agent.
     * @param name          The name of the Agent.
     * @param zooKeeperUrl  The url of Zoo Keeper.
     * @param debugAgent    Flag used to enable debugging on the new Agent.
     * @param number        The number of Agents to create.
     */
    @Override
    public void create(URI proxyUri, URI agentUri, String name, String zooKeeperUrl, boolean debugAgent, int number) {
        String imageId = null;
        String hardwareId = null;
        String locationId = null;
        String group = null;
        String user = null;
        JCloudsInstanceType instanceType = JCloudsInstanceType.Smallest;
        String identity = null;
        String credential = null;
        String owner = null;

        try {
            String providerName = agentUri.getHost();

            if (agentUri.getQuery() != null) {
                Map<String, String> parameters = parseQuery(agentUri.getQuery());
                if (parameters != null) {
                    imageId = parameters.get(IMAGE_ID);
                    group = parameters.get(GROUP);
                    locationId = parameters.get(LOCATION_ID);
                    hardwareId = parameters.get(HARDWARE_ID);
                    user = parameters.get(USER);
                    if (parameters.get(INSTANCE_TYPE) != null) {
                        instanceType = JCloudsInstanceType.get(parameters.get(INSTANCE_TYPE), instanceType);
                    }
                }
            }

            doCreateAgent(proxyUri, name, number, zooKeeperUrl, debugAgent, imageId, hardwareId, locationId, group, user, instanceType, providerName, identity, credential, owner);
        } catch (FabricException e) {
            throw e;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    /**
     * Creates an {@link org.fusesource.fabric.api.Agent} with the given name pointing to the specified zooKeeperUrl.
     * @param proxyUri     The uri of the maven proxy to use.
     * @param agentUri     The uri that contains required information to build the Agent.
     * @param name         The name of the Agent.
     * @param zooKeeperUrl The url of Zoo Keeper.
     */
    public void create(URI proxyUri, URI agentUri, String name, String zooKeeperUrl) {
        create(proxyUri,agentUri, name, zooKeeperUrl);
    }

    @Override
    public boolean create(CreateAgentArguments createArgs, String name, String zooKeeperUrl) throws Exception {
        if (createArgs instanceof CreateJCloudsAgentArguments) {
            CreateJCloudsAgentArguments args = (CreateJCloudsAgentArguments) createArgs;

            boolean debugAgent = args.isDebugAgent();
            int number = args.getNumber();
            String imageId = args.getImageId();
            String hardwareId = args.getHardwareId();
            String locationId = args.getLocationId();
            String group = args.getGroup();
            String user = args.getUser();
            JCloudsInstanceType instanceType = args.getInstanceType();
            String providerName = args.getProviderName();
            String identity = args.getIdentity();
            String credential = args.getCredential();
            String owner = args.getOwner();
            URI proxyURI = args.getProxyUri();

            doCreateAgent(proxyURI, name, number, zooKeeperUrl, debugAgent, imageId, hardwareId, locationId, group, user, instanceType, providerName, identity, credential, owner);
            return true;
        }
        return false;
    }

    protected void doCreateAgent(URI proxyUri, String name, int number, String zooKeeperUrl, boolean debugAgent, String imageId, String hardwareId, String locationId, String group, String user, JCloudsInstanceType instanceType, String providerName, String identity, String credential, String owner) throws MalformedURLException, RunNodesException, URISyntaxException {
        ComputeService computeService = computeServiceMap.get(providerName);
        if (computeService == null) {
            //Iterable<? extends Module> modules = ImmutableSet.of(new Log4JLoggingModule(), new JschSshClientModule());
            Iterable<? extends Module> modules = ImmutableSet.of();

            Properties props = new Properties();
            props.put("provider", providerName);
            props.put("identity", identity);
            props.put("credential", credential);
            if (!Strings.isNullOrEmpty(owner)) {
                props.put("jclouds.ec2.ami-owners", owner);
            }

            RestContextFactory restFactory = new RestContextFactory();
            ComputeServiceContext context = new ComputeServiceContextFactory(restFactory).createContext(providerName, identity, credential, modules, props);
            computeService = context.getComputeService();
        }

        TemplateBuilder builder = computeService.templateBuilder();
        builder.any();
        switch (instanceType) {
            case Smallest:
                builder.smallest();
                break;
            case Biggest:
                builder.biggest();
                break;
            case Fastest:
                builder.fastest();
        }

        if (locationId != null) {
            builder.locationId(locationId);
        }
        if (imageId != null) {
            builder.imageId(imageId);
        }
        if (hardwareId != null) {
            builder.hardwareId(hardwareId);
        }

        Set<? extends NodeMetadata> metadatas = null;
        Credentials credentials = null;
        if (user != null && credentials == null) {
            credentials = new Credentials(user, null);
        }

        metadatas = computeService.createNodesInGroup(group, number, builder.build());

        int suffix = 1;
        if (metadatas != null) {
            for (NodeMetadata nodeMetadata : metadatas) {
                String id = nodeMetadata.getId();
                String agentName = name;
                if(number > 1) {
                    agentName+=suffix++;
                }
                String script = buildStartupScript(proxyUri, agentName, "~/", zooKeeperUrl, DEFAULT_SSH_PORT, debugAgent);
                if (credentials != null) {
                    computeService.runScriptOnNode(id, script, RunScriptOptions.Builder.overrideCredentialsWith(credentials).runAsRoot(false));
                } else {
                    computeService.runScriptOnNode(id, script);
                }
            }
        }
    }

    /*
    protected URI getMavenRepoURI(FabricService fabricService) throws URISyntaxException {
        URI localRepoURI = null;
        if (mavenProxy != null) {
            localRepoURI = mavenProxy.getAddress();
        }
        return FabricServices.getMavenRepoURI(fabricService, localRepoURI);
    }
    */

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
}