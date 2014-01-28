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
import org.fusesource.gateway.handlers.http.HttpMappingRule;
import org.fusesource.gateway.handlers.http.MappedServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
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
    private Runnable changeListener = new Runnable() {
        @Override
        public void run() {
            try {
                rewriteHaproxyConfigFile();
            } catch (Exception e) {
                LOG.warn("Failed to write haproxy config file: " + e, e);
            }
        }
    };

    public void rewriteHaproxyConfigFile() throws IOException {
        LOG.info("Writing HAProxy file: " + haproxyConfigFile);
        File outFile = new File(haproxyConfigFile);
        outFile.getParentFile().mkdirs();
        PrintWriter writer = new PrintWriter(new FileWriter(outFile));
        try {
            Map<String, String> servers = new HashMap<String, String>();
            Map<String, String> backends = new HashMap<String, String>();
            Map<String, MappedServices> mappedServices = getMappedServices();
            Set<Map.Entry<String, MappedServices>> entries = mappedServices.entrySet();
            for (Map.Entry<String, MappedServices> entry : entries) {
                String uri = entry.getKey();
                MappedServices services = entry.getValue();
                Set<String> serviceUrls = services.getServiceUrls();
                for (String serviceUrl : serviceUrls) {
                    URL url = null;
                    try {
                        url = new URL(serviceUrl);
                    } catch (MalformedURLException e) {
                        LOG.warn("Ignore bad URL: " + e);
                    }
                    if (url != null) {
                        writeHaproxyConfig(writer, uri,  url, services, servers, backends);
                    }
                }
            }

            for (Map.Entry<String, String> entry : backends.entrySet()) {
                writer.println(entry.getValue());
            }
            for (Map.Entry<String, String> entry : servers.entrySet()) {
                writer.println(entry.getValue());
            }
        } finally {
            try {
                writer.close();
            } catch (Exception e) {
                LOG.debug("Caught while closing: " + e, e);
            }
        }
    }

    protected void writeHaproxyConfig(PrintWriter writer, String uri, URL serviceUrl,
                                      MappedServices services, Map<String, String> servers, Map<String, String> backends) {


        int backendPort = getPortValue(serviceUrl);
        String backend = "b" + uri.replace('/', '_').replace('-', '_');
        while (backend.endsWith("_")) {
            backend = backend.substring(0, backend.length() - 1);
        }
        if (!backends.containsKey(backend)) {
            backends.put(backend, "use backend " + backend + "\nbackend " + backend + " :" + backendPort);
        }

        int serverPort = getPortValue(serviceUrl);
        //String server = serviceUrl.getHost();
        String server = services.getContainer();
        if (!servers.containsKey(server)) {
            //servers.put(server, "server " + server + ":" + serverPort + " check");
            servers.put(server, "server " + server + " check");
        }
/*
        acl cxf_about_service path_beg /about
        use backend b_cxf_about_service if cxf_about_service
        backend b_cxf_about_service :80
        server X:1234 check
        server Y:4321 check
*/
    }

    protected static int getPortValue(URL url) {
        int answer = url.getPort();
        if (answer == 0) {
            answer = 80;
        }
        return answer;
    }

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
        mappingRuleConfiguration.addChangeListener(changeListener);
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
