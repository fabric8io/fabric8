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
import io.fabric8.api.scr.Configurer;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.internal.Objects;
import io.fabric8.zookeeper.internal.SimplePathTemplate;

import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.fusesource.gateway.fabric.support.http.HttpMappingRuleBase;
import org.fusesource.gateway.fabric.support.http.HttpMappingZooKeeperTreeCache;
import org.fusesource.gateway.loadbalancer.LoadBalancer;
import org.fusesource.gateway.loadbalancer.LoadBalancers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A mapping rule for use with the {@link org.fusesource.gateway.fabric.http.FabricHTTPGateway}
 */
@Component(name = "io.fabric8.gateway.http.mapping", immediate = true, metatype = true, policy = ConfigurationPolicy.REQUIRE,
        label = "Fabric8 HTTP Gateway Mapping Rule",
        description = "Provides a mapping between part of the fabric cluster and a HTTP URI template")
public final class HttpMappingRuleConfiguration extends AbstractComponent {

    private static final transient Logger LOG = LoggerFactory.getLogger(HttpMappingRuleConfiguration.class);

    @Reference
    private Configurer configurer;
    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();
    @Reference(referenceInterface = FabricHTTPGateway.class)
    private final ValidatingReference<FabricHTTPGateway> gateway = new ValidatingReference<FabricHTTPGateway>();

    @Property(name = "zooKeeperPath", value = "/fabric/registry/clusters/webapps",
            label = "ZooKeeper path", description = "The path in ZooKeeper which is monitored to discover the available web services or web applications")
    private String zooKeeperPath;

    @Property(name = "uriTemplate", value = "{contextPath}/",
            label = "URI template", description = "The URI template mapping the URI to the underlying service implementation.\nThis can use a number of URI template values such as 'contextPath', 'version', 'serviceName'")
    private String uriTemplate;

    @Property(name = "enabledVersion",
            label = "Enable version", description = "Specify the exact profile version to expose; if none is specified then the gateways current profile version is used.\nIf a {version} URI template is used then all versions are exposed.")
    private String enabledVersion;

    @Property(name = "reverseHeaders", boolValue = true,
            label = "Reverse headers", description = "If enabled then the URL in the Location, Content-Location and URI headers from the proxied HTTP responses are rewritten from the back end service URL to match the front end URL on the gateway.\nThis is equivalent to the ProxyPassReverse directive in mod_proxy.")
    private boolean reverseHeaders = true;

    @Property(name = "loadBalancerType",
            value = LoadBalancers.ROUND_ROBIN_LOAD_BALANCER,
            options = {
                    @PropertyOption(name = LoadBalancers.RANDOM_LOAD_BALANCER, value = "Random"),
                    @PropertyOption(name = LoadBalancers.ROUND_ROBIN_LOAD_BALANCER, value = "Round Robin"),
                    @PropertyOption(name = LoadBalancers.STICKY_LOAD_BALANCER, value = "Sticky")
            },
            label = "Load Balancer", description = "The kind of load balancing strategy used")
    private String loadBalancerType;

    @Property(name = "stickyLoadBalancerCacheSize", intValue = LoadBalancers.STICKY_LOAD_BALANCER_DEFAULT_CACHE_SIZE,
            label = "Sticky Load Balancer Cache Size", description = "The number of unique client keys to cache for the sticky load balancer (using an LRU caching algorithm)")
    private int stickyLoadBalancerCacheSize = LoadBalancers.STICKY_LOAD_BALANCER_DEFAULT_CACHE_SIZE;

    private HttpMappingRuleBase httpMappingRuleBase;

    private HttpMappingZooKeeperTreeCache mappingTree;

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

    @Deactivate
    void deactivate() {
        gateway.get().removeMappingRuleConfiguration(httpMappingRuleBase);
        httpMappingRuleBase = null;

        deactivateInternal();
        deactivateComponent();
    }

    String getZooKeeperPath() {
        return zooKeeperPath;
    }

    void setZooKeeperPath(String zooKeeperPath) {
        this.zooKeeperPath = zooKeeperPath;
    }

    String getEnabledVersion() {
        return enabledVersion;
    }

    void setEnabledVersion(String enabledVersion) {
        this.enabledVersion = enabledVersion;
    }

    String getUriTemplate() {
        return uriTemplate;
    }

    void setUriTemplate(String uriTemplate) {
        this.uriTemplate = uriTemplate;
    }

    String getLoadBalancerType() {
        return loadBalancerType;
    }

    void setLoadBalancerType(String loadBalancerType) {
        this.loadBalancerType = loadBalancerType;
    }

    private void updateConfiguration(Map<String, ?> configuration) throws Exception {
        LOG.info("activating http mapping rule " + configuration);
        configurer.configure(configuration, this);
        LOG.info("activating http mapping rule " + zooKeeperPath + " on " + gateway.get().getPort());

        String zkPath = getZooKeeperPath();
        Objects.notNull(zkPath, "zooKeeperPath");
        Objects.notNull(getUriTemplate(), "uriTemplate");

        LoadBalancer<String> loadBalancer = LoadBalancers.createLoadBalancer(loadBalancerType, stickyLoadBalancerCacheSize);

        LOG.info("activating http mapping ZooKeeper path: " + zkPath + " with URI template: " + uriTemplate
                + " enabledVersion: " + enabledVersion + " with load balancer: " + loadBalancer);

        if (httpMappingRuleBase != null) {
            gateway.get().removeMappingRuleConfiguration(httpMappingRuleBase);
        }
        httpMappingRuleBase = new HttpMappingRuleBase(
                new SimplePathTemplate(uriTemplate),
                gateway.get().getGatewayVersion(),
                enabledVersion, loadBalancer, reverseHeaders);

        mappingTree = new HttpMappingZooKeeperTreeCache(curator.get(), httpMappingRuleBase, zooKeeperPath);
        mappingTree.init();

        gateway.get().addMappingRuleConfiguration(httpMappingRuleBase);
    }

    private void deactivateInternal() {
        if (mappingTree != null) {
            mappingTree.destroy();
            mappingTree = null;
        }
    }

    void bindCuratorFramework(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCuratorFramework(CuratorFramework curator) {
        this.curator.unbind(curator);
    }

    void bindFabricHTTPGateway(FabricHTTPGateway gateway) {
        this.gateway.bind(gateway);
    }

    void unbindFabricHTTPGateway(FabricHTTPGateway gateway) {
        this.gateway.unbind(gateway);
    }

    @Override
    public String toString() {
        return "HttpMappingRuleConfiguration{" +
                "zooKeeperPath='" + zooKeeperPath + '\'' +
                ", uriTemplate='" + uriTemplate + '\'' +
                ", enabledVersion='" + enabledVersion + '\'' +
                '}';
    }
}
