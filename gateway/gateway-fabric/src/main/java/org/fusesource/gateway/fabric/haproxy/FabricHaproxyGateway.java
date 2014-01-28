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
package org.fusesource.gateway.fabric.haproxy;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Version;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.support.ConfigInjection;
import io.fabric8.internal.Objects;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.gateway.fabric.FabricGateway;
import org.fusesource.gateway.handlers.http.HttpGateway;
import org.fusesource.gateway.handlers.http.HttpGatewayHandler;
import org.fusesource.gateway.handlers.http.HttpGatewayServer;
import org.fusesource.gateway.handlers.http.HttpMappingRule;
import org.fusesource.gateway.handlers.http.MappedServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * An HTTP gateway which listens on a port and applies a number of {@link org.fusesource.gateway.fabric.http.HttpMappingRuleConfiguration} instances to bind
 * HTTP requests to different HTTP based services running within the fabric.
 */
@Service(FabricHaproxyGateway.class)
@Component(name = "io.fabric8.gateway.haproxy", immediate = true, metatype = true, policy = ConfigurationPolicy.REQUIRE,
        label = "Fabric8 HAProxy Gateway",
        description = "Provides a discovery and load balancing gateway between clients using various messaging protocols and the available message brokers in the fabric")
public class FabricHaproxyGateway extends AbstractComponent {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabricHaproxyGateway.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, bind = "setGateway", unbind = "unsetGateway")
    private FabricGateway gateway;

    @Property(name = "haproxyConfigFile", value = "${karaf.data}/data/haproxy.conf",
            label = "Haproxy config file", description = "The output configuration file created for haproxy to reuse")
    private String haproxyConfigFile;

    private Set<HttpMappingRule> mappingRuleConfigurations = new CopyOnWriteArraySet<HttpMappingRule>();

    @Activate
    void activate(Map<String, ?> configuration) throws Exception {
        updateConfiguration(configuration);
        activateComponent();
    }


    @Modified
    void modified(Map<String, ?> configuration) throws Exception {
        deactivateInternal();
        updateConfiguration(configuration);
    }


    @Deactivate
    void deactivate() {
        deactivateInternal();
        deactivateComponent();
    }

    protected void updateConfiguration(Map<String, ?> configuration) throws Exception {
        ConfigInjection.applyConfiguration(configuration, this);
        Objects.notNull(getGateway(), "gateway");
    }

    protected void deactivateInternal() {
    }


    public void addMappingRuleConfiguration(HttpMappingRule mappingRuleConfiguration) {
        mappingRuleConfigurations.add(mappingRuleConfiguration);
    }


    public void removeMappingRuleConfiguration(HttpMappingRule mappingRuleConfiguration) {
        mappingRuleConfigurations.remove(mappingRuleConfiguration);
    }

    public Map<String, MappedServices> getMappedServices() {
        Map<String, MappedServices> answer = new HashMap<String, MappedServices>();
        for (HttpMappingRule mappingRuleConfiguration : mappingRuleConfigurations) {
            mappingRuleConfiguration.appendMappedServices(answer);
        }
        return answer;
    }

    // Properties
    //-------------------------------------------------------------------------

    public FabricGateway getGateway() {
        return gateway;
    }

    public void setGateway(FabricGateway gateway) {
        this.gateway = gateway;
    }

    public void unsetGateway(FabricGateway gateway) {
        this.gateway = null;
    }

    public CuratorFramework getCurator() {
        Objects.notNull(getGateway(), "gateway");
        return gateway.getCurator();
    }

    /**
     * Returns the default profile version used to filter out the current versions of services
     * if no version expression is used the URI template
     */
    public String getGatewayVersion() {
        FabricService fabricService = gateway.getFabricService();
        if (fabricService != null) {
            Container currentContainer = fabricService.getCurrentContainer();
            if (currentContainer != null) {
                Version version = currentContainer.getVersion();
                if (version != null) {
                    return version.getId();
                }
            }
        }
        return null;
    }
}
