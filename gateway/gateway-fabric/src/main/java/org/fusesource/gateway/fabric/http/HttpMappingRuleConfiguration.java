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
package org.fusesource.gateway.fabric.http;

import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.support.ConfigInjection;
import io.fabric8.internal.Objects;
import io.fabric8.zookeeper.internal.SimplePathTemplate;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.fusesource.gateway.handlers.http.HttpGateway;
import org.fusesource.gateway.handlers.http.HttpMappingRule;
import org.fusesource.gateway.handlers.http.MappedServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A mapping rule for use with the {@link org.fusesource.gateway.fabric.http.FabricHTTPGateway}
 */
@Component(name = "io.fabric8.gateway.http.mapping", immediate = true, metatype = true, policy = ConfigurationPolicy.REQUIRE,
        label = "Fabric8 HTTP Gateway Mapping Rule",
        description = "Provides a mapping between part of the fabric cluster and a HTTP URI template")
public class HttpMappingRuleConfiguration extends AbstractComponent implements HttpMappingRule {
    private static final transient Logger LOG = LoggerFactory.getLogger(HttpMappingRuleConfiguration.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, bind = "setGateway", unbind = "unsetGateway")
    private FabricHTTPGateway gateway;

    @Property(name = "zooKeeperPath", value = "/fabric/registry/clusters/webapps",
            label = "ZooKeeper path", description = "The path in ZooKeeper which is monitored to discover the available message brokers")
    private String zooKeeperPath;

    @Property(name = "uriTemplate", value = "{contextPath}/",
            label = "URI template", description = "The URI template mapping the URI to the underlying service implementation.\nThis can use a number of URI template values such as 'contextPath', 'version', 'serviceName'")
    private String uriTemplate;

    @Property(name = "enabledVersion",
            label = "Enable version", description = "Specify the exact profile version to expose; if none is specified then the gateways current profile version is used.\nIf a {version} URI template is used then all versions are exposed.")
    private String enabledVersion;

    /**
     * The version used if no "version" expression is used in the {@link #uriTemplate} and no
     * {@link #enabledVersion} is specified
     */
    private String gatewayVersion;

    private HttpProxyMappingTree mappingTree;

    private SimplePathTemplate pathTemplate;

    private Map<String, MappedServices> mappingRules = new ConcurrentHashMap<String, MappedServices>();

    /**
     * Populates the parameters from the URL of the service so they can be reused in the URI template
     */
    public static void populateUrlParams(Map<String, String> params, String service) {
        try {
            URL url = new URL(service);
            params.put("contextPath", url.getPath());
            params.put("protocol", url.getProtocol());
            params.put("host", url.getHost());
            params.put("port", "" + url.getPort());

        } catch (MalformedURLException e) {
            LOG.warn("Invalid URL '" + service + "'. " + e);
        }
    }


    @Override
    public String toString() {
        return "HttpMappingRuleConfiguration{" +
                "zooKeeperPath='" + zooKeeperPath + '\'' +
                ", uriTemplate='" + uriTemplate + '\'' +
                ", enabledVersion='" + enabledVersion + '\'' +
                '}';
    }

    @Activate
    void activate(Map<String, ?> configuration) throws Exception {
        activateComponent();
        updateConfiguration(configuration);
    }

    @Modified
    void modified(Map<String, ?> configuration) throws Exception {
        deactivateInternal();
        updateConfiguration(configuration);
    }

    protected void updateConfiguration(Map<String, ?> configuration) throws Exception {
        LOG.info("activating http mapping rule " + configuration);
        ConfigInjection.applyConfiguration(configuration, this);
        LOG.info("activating http mapping rule " + zooKeeperPath + " on " + gateway.getPort());

        Objects.notNull(getGateway(), "gateway");
        Objects.notNull(getZooKeeperPath(), "zooKeeperPath");
        Objects.notNull(getUriTemplate(), "uriTemplate");

        setGatewayVersion(gateway.getGatewayVersion());

        CuratorFramework curator = gateway.getCurator();

        mappingTree = new HttpProxyMappingTree(curator, this);
        mappingTree.init();

        gateway.addMappingRuleConfiguration(this);
    }

    @Deactivate
    void deactivate() {
        gateway.removeMappingRuleConfiguration(this);

        deactivateInternal();
        deactivateComponent();
    }

    protected void deactivateInternal() {
        if (mappingTree != null) {
            mappingTree.destroy();
            mappingTree = null;
        }
    }

    @Override
    public void appendMappedServices(Map<String, MappedServices> rules) {
        rules.putAll(mappingRules);
    }

    // Properties
    //-------------------------------------------------------------------------

    public HttpGateway getGateway() {
        return gateway;
    }

    public void setGateway(FabricHTTPGateway gateway) {
        this.gateway = gateway;
    }

    public void unsetGateway(FabricHTTPGateway gateway) {
        this.gateway = null;
    }

    public SimplePathTemplate getPathTemplate() {
        if (pathTemplate == null) {
            if (uriTemplate != null) {
                pathTemplate = new SimplePathTemplate(uriTemplate);
            }
        }
        return pathTemplate;
    }

    public String getZooKeeperPath() {
        return zooKeeperPath;
    }

    public void setZooKeeperPath(String zooKeeperPath) {
        this.zooKeeperPath = zooKeeperPath;
    }

    public String getEnabledVersion() {
        return enabledVersion;
    }

    public void setEnabledVersion(String enabledVersion) {
        this.enabledVersion = enabledVersion;
    }

    public String getUriTemplate() {
        return uriTemplate;
    }

    public void setUriTemplate(String uriTemplate) {
        this.uriTemplate = uriTemplate;
    }

    public String getGatewayVersion() {
        return gatewayVersion;
    }

    public void setGatewayVersion(String gatewayVersion) {
        this.gatewayVersion = gatewayVersion;
    }

    /**
     * Given a path being added or removed, update the services.
     *
     * @param remove whether to remove (if true) or add (if false) this mapping
     * @param path the path that this mapping is bound
     * @param services the HTTP URLs of the services to map to
     * @param defaultParams the default parameters to use in the URI templates such as for version and container
     */
    public void updateMappingRules(boolean remove, String path, List<String> services, Map<String, String> defaultParams) {
        SimplePathTemplate pathTemplate = getPathTemplate();
        if (pathTemplate != null) {
            boolean versionSpecificUri = pathTemplate.getParameterNames().contains("version");

            String versionId = defaultParams.get("version");
            if (!remove && versionId != null && !versionSpecificUri && gatewayVersion != null) {
                // lets ignore this mapping if the version does not match
                if (!gatewayVersion.equals(versionId)) {
                    remove = true;
                }
            }

            Map<String, String> params = new HashMap<String, String>();
            if (defaultParams != null){
                params.putAll(defaultParams);
            }
            params.put("servicePath", path);

            // TODO decide whether or not to expose this based on the version number!!!

            for (String service : services) {
                populateUrlParams(params, service);
                String fullPath = pathTemplate.bindByNameNonStrict(params);
                if (remove) {
                    MappedServices rule = mappingRules.get(fullPath);
                    if (rule != null) {
                        Set<String> serviceUrls = rule.getServiceUrls();
                        serviceUrls.remove(service);
                        if (serviceUrls.isEmpty()) {
                            mappingRules.remove(fullPath);
                        }
                    }
                } else {
                    MappedServices mappedServices = new MappedServices(service);
                    MappedServices oldRule = mappingRules.put(fullPath, mappedServices);
                    if (oldRule != null) {
                        mappedServices.getServiceUrls().addAll(oldRule.getServiceUrls());
                    }
                }
            }
        }
    }
}
