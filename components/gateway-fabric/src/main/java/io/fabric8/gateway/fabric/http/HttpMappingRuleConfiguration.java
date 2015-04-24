/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.gateway.fabric.http;

import io.fabric8.gateway.fabric.support.SimplePathTemplate;
import io.fabric8.gateway.fabric.support.http.HttpMappingKubeCache;
import io.fabric8.gateway.fabric.support.http.HttpMappingRuleBase;
import io.fabric8.gateway.loadbalancer.LoadBalancer;
import io.fabric8.gateway.loadbalancer.LoadBalancers;

import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A mapping rule for use with the {@link FabricHTTPGateway}
 * Provides a mapping between part of the fabric cluster and a HTTP URI template"
 */
@ApplicationScoped
public class HttpMappingRuleConfiguration {

    private static final transient Logger LOG = LoggerFactory.getLogger(HttpMappingRuleConfiguration.class);

    @Inject
    private FabricHTTPGateway gateway;

    private List<Map<String,String>> serviceSelectors;
    
    /** The URI template mapping the URI to the underlying service implementation.
     * This can use a number of URI template values such as 'contextPath', 'version', 'serviceName'
     * example api/{contextPath}/
     */
    private String uriTemplate;

    /** Specify the exact profile version to expose; if none is specified then the 
     * gateway's current profile version is used. If a {version} URI template 
     * is used then all versions are exposed.
     */
    private String enabledVersion;

    /** If enabled then the URL in the Location, Content-Location and URI headers from 
     * the proxied HTTP responses are rewritten from the back end service URL to match the 
     * front end URL on the gateway.This is equivalent to the ProxyPassReverse directive
     * in mod_proxy.")
     */
    private boolean reverseHeaders = true;

    /** The kind of load balancing strategy used
     * <ul>
     * <li>LoadBalancers.RANDOM_LOAD_BALANCER, value = "Random"),
     * <li>LoadBalancers.ROUND_ROBIN_LOAD_BALANCER, value = "Round Robin"),
     * <li>LoadBalancers.STICKY_LOAD_BALANCER, value = "Sticky")
     * </ul>
     */
    private String loadBalancerType;

    /** The number of unique client keys to cache for the sticky load balancer (using an 
     * LRU caching algorithm)")
     */
    private int stickyLoadBalancerCacheSize = LoadBalancers.STICKY_LOAD_BALANCER_DEFAULT_CACHE_SIZE;

    private HttpMappingRuleBase httpMappingRuleBase;

    private HttpMappingKubeCache mappingTree;

    @PreDestroy
    void deactivate() {
        gateway.removeMappingRuleConfiguration(httpMappingRuleBase);
        httpMappingRuleBase = null;
        deactivateInternal();
    }

    List<Map<String,String>> getServiceSelectors() {
        return serviceSelectors;
    }

    void setServiceSelectors(List<Map<String,String>> serviceSelectors) {
        this.serviceSelectors = serviceSelectors;
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

    public void configure(HTTPGatewayConfig httpGatewayConfig) throws Exception {
        
        serviceSelectors = httpGatewayConfig.getServiceSelectors();
        LOG.info("activating http mapping rule " + httpGatewayConfig.get(HTTPGatewayConfig.SELECTORS) + " on " + httpGatewayConfig.get(HTTPGatewayConfig.HTTP_PORT));
        loadBalancerType = httpGatewayConfig.getLoadBalancerType();
        uriTemplate = httpGatewayConfig.getUriTemplate();
        enabledVersion = httpGatewayConfig.getEnabledVersion();
        reverseHeaders = httpGatewayConfig.isReverseHeaders();
        
        LoadBalancer loadBalancer = LoadBalancers.createLoadBalancer(loadBalancerType, stickyLoadBalancerCacheSize);

        LOG.info("activating http mapping selector: " + serviceSelectors + " with URI template: " + uriTemplate
                + " enabledVersion: " + enabledVersion + " with load balancer: " + loadBalancer);

        if (httpMappingRuleBase != null) {
            gateway.removeMappingRuleConfiguration(httpMappingRuleBase);
        }
        httpMappingRuleBase = new HttpMappingRuleBase(
                new SimplePathTemplate(uriTemplate),
                gateway.getGatewayVersion(),
                enabledVersion, loadBalancer, reverseHeaders);

        gateway.configure(httpGatewayConfig);
        mappingTree = new HttpMappingKubeCache(httpMappingRuleBase, serviceSelectors, gateway.getApiManager());
        //mappingTree = new HttpMappingZooKeeperTreeCache(curator.get(), httpMappingRuleBase, zooKeeperPath);
        mappingTree.init(httpGatewayConfig);

        gateway.addMappingRuleConfiguration(httpMappingRuleBase);
        
    }

    private void deactivateInternal() {
        if (mappingTree != null) {
            mappingTree.destroy();
            mappingTree = null;
        }
    }

    @Override
    public String toString() {
        return "HttpMappingRuleConfiguration{" +
                "serviceSelectors='" + serviceSelectors + '\'' +
                ", uriTemplate='" + uriTemplate + '\'' +
                ", enabledVersion='" + enabledVersion + '\'' +
                '}';
    }
}
