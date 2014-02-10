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
package io.fabric8.camel.autotest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.MBeanServer;

import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.scr.AbstractFieldInjectionComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.utils.Strings;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.NodeIdFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "io.fabric8.camel.autotest", label = "Fabric8 Camel Auto Test Service",
        description = "Enabling this service will automatically send any sample test messages stored in the wiki for the CamelContext ID and route ID to the routes whenever the route is restarted (such as if you edit the route or change its source, configuration or code).",
        policy = ConfigurationPolicy.REQUIRE, immediate = true, metatype = true)
public final class CamelAutoTestService extends AbstractFieldInjectionComponent {

    private static final transient Logger LOG = LoggerFactory.getLogger(CamelAutoTestService.class);

    @Reference(referenceInterface = FabricService.class, bind = "bindFabricService", unbind = "unbindFabricService")
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    @Reference(referenceInterface = MBeanServer.class, bind = "bindMBeanServer", unbind = "unbindMBeanServer")
    private MBeanServer mbeanServer;

    @Reference(referenceInterface = CamelContext.class, cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, bind = "bindCamelContexts", unbind = "unbindCamelContexts")
    private Map<String, CamelContext> camelContexts = new HashMap<String, CamelContext>();

    @Property(name = "mockOutputs", boolValue = true,
    label = "Mock output endpoints", description = "If enabled then the output endpoints are replaced with mock endpoints for easier testing/viewing and the underlying middleware isn't used.")
    private boolean mockOutputs = true;

    @Property(name = "mockInputs", boolValue = true,
    label = "Mock input endpoints", description = "If enabled then the input endpoints on tested camel routes are stubbed out so the underlying middleware isn't used.")
    private boolean mockInputs = true;

    @Property(name = "messageFolder", value = "testMessages",
            label = "Test message folder", description = "The folder path in the wiki to store sample input messages")
    private String messageFolder = "testMessages";

    private final CamelAutoInterceptSendToEndpointStrategy strategy = new CamelAutoInterceptSendToEndpointStrategy();

    /**
     * Keeps track of which camel contexts we've configured
     */
    private Set<String> camelContextsConfigured = new HashSet<String>();

    @Override
    protected void onDeactivate() throws Exception {
        LOG.debug("onDeactivate");
        camelContextsConfigured.clear();
        super.onDeactivate();
    }

    @Override
    protected void onConfigured() throws Exception {
        LOG.debug("onConfigured. mockOutputs: " + mockOutputs + " mockInputs: " + mockInputs + " messageFolder: " + messageFolder);

        FabricService fabricService = this.fabricService.getOptional();

        // lets find the camel contexts to test in this container
        MBeanServer mbeanServerValue = mbeanServer;
        if (mbeanServerValue != null && fabricService != null) {
            Profile overlayProfile = fabricService.getCurrentContainer().getOverlayProfile();
            List<String> configurationFileNames = overlayProfile.getConfigurationFileNames();
            for (CamelContext camelContext : camelContexts.values()) {
                String camelContextID = camelContext.getName();
                // check we only add testing stuff to each context once
                if (camelContext instanceof ModelCamelContext) {
                    final ModelCamelContext modelCamelContext = (ModelCamelContext) camelContext;
                    List<RouteDefinition> routeDefinitions = modelCamelContext.getRouteDefinitions();
                    if (camelContextsConfigured.add(camelContextID)) {
                        NodeIdFactory nodeIdFactory = camelContext.getNodeIdFactory();

                        if (mockInputs || mockOutputs) {
                            for (RouteDefinition routeDefinition : routeDefinitions) {
                                String routeId = routeDefinition.idOrCreate(nodeIdFactory);
                                modelCamelContext.stopRoute(routeId);

                                final String routeKey = camelContextID + "." + routeId;
                                LOG.info("Mocking Camel route: " + routeKey);
                                routeDefinition.adviceWith(modelCamelContext, new AdviceWithRouteBuilder() {
                                    @Override
                                    public void configure() throws Exception {
                                        if (mockOutputs) {
                                            modelCamelContext.addRegisterEndpointCallback(strategy);
                                        }
                                    }
                                });
                                // the advised route is automatic restarted
                            }
                        }

                        String path = messageFolder;
                        if (Strings.isNotBlank(path)) {
                            path += "/";
                        }
                        path += camelContextID;

                        ProducerTemplate producerTemplate = camelContext.createProducerTemplate();
                        try {
                            for (RouteDefinition routeDefinition : routeDefinitions) {
                                String routeId = routeDefinition.idOrCreate(nodeIdFactory);
                                String routePath = path + "/" + routeId + "/";
                                List<FromDefinition> inputs = routeDefinition.getInputs();
                                for (FromDefinition input : inputs) {
                                    Endpoint endpoint = input.getEndpoint();
                                    if (endpoint == null) {
                                        String uri = input.getUri();
                                        if (Strings.isNullOrBlank(uri)) {
                                            String ref = input.getRef();
                                            if (Strings.isNotBlank(ref)) {
                                                uri = "ref:" + ref;
                                            }
                                        }
                                        if (Strings.isNotBlank(uri)) {
                                            endpoint = camelContext.getEndpoint(uri);
                                        }
                                    }
                                    if (endpoint == null) {
                                        LOG.warn("Cannot find endpoint, uri or ref of input " + input + " on route " + routeId + " camelContext: " + camelContextID);
                                    } else {
                                        for (String configFile : configurationFileNames) {
                                            if (configFile.startsWith(routePath)) {
                                                LOG.info("Sending file: " + configFile + " to " + endpoint);
                                                byte[] data = overlayProfile.getFileConfiguration(configFile);
                                                if (data != null) {
                                                    // lest send this message to this endpoint
                                                    producerTemplate.sendBody(endpoint, data);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } finally {
                            producerTemplate.stop();
                        }
                    }
                }
            }
        }
    }

    FabricService getFabricService() {
        return fabricService.get();
    }

    void bindCamelContexts(CamelContext camelContext) {
        if (camelContext != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Bind camelContext " + camelContext + " status: " + camelContext.getStatus());
            }
            String id = camelContext.getName();
            this.camelContexts.put(id, camelContext);
            clearCamelContextConfiguration(id);
        }
    }

    protected void clearCamelContextConfiguration(String camelContextId) {
        this.camelContextsConfigured.remove(camelContextId);
    }

    void unbindCamelContexts(CamelContext camelContext) {
        if (camelContext != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unbind camelContext " + camelContext + " status: " + camelContext.getStatus());
            }
            String id = camelContext.getName();
            this.camelContexts.remove(id);
            clearCamelContextConfiguration(id);
        }
    }

    void bindMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    void unbindMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = null;
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }
}
