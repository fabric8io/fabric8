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
package io.fabric8.gateway.model;

import io.fabric8.gateway.loadbalancer.ClientRequestFacade;
import io.fabric8.gateway.loadbalancer.LoadBalancer;
import io.fabric8.gateway.model.loadbalancer.LoadBalancerDefinition;
import io.fabric8.gateway.model.loadbalancer.RoundRobinLoadBalanceDefinition;
import io.fabric8.gateway.support.MappingResult;
import io.fabric8.gateway.support.UriTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a HTTP mapping rule from an input URI pattern to some back end service
 */
public class HttpProxyRule {
    private static final transient Logger LOG = LoggerFactory.getLogger(HttpProxyRule.class);

    private UriTemplateDefinition uriTemplate;
    private boolean reverseHeaders = true;
    private LoadBalancerDefinition loadBalancer = new RoundRobinLoadBalanceDefinition();
    private Set<UriTemplateDefinition> destinationUriTemplates = new HashSet<UriTemplateDefinition>();
    private String cookiePath;
    private String cookieDomain;

    public HttpProxyRule() {
    }

    public HttpProxyRule(String uriTemplate) {
        this.uriTemplate = new UriTemplateDefinition(uriTemplate);
    }

    public MappingResult matches(String[] paths) {
        UriTemplate template = getUriTemplateObject();
        if (template == null) {
            LOG.warn("getUriTemplateObject() returned null!");
            return null;
        }
        return template.matches(paths, this);
    }

    /**
     * Chooses a back end service from the set of destination templates
     */
    public UriTemplateDefinition chooseBackEndService(ClientRequestFacade requestFacade) {
        LoadBalancer loadBalancer = getLoadBalancer().getLoadBalancer();
        List<UriTemplateDefinition> uriDefList = new ArrayList<UriTemplateDefinition>(destinationUriTemplates);
        return loadBalancer.choose(uriDefList, requestFacade);
    }

    // DSL
    //-------------------------------------------------------------------------

    /**
     * Adds a destination URI template mapping; such as a physical endpoint we can proxy to if there are multiple possible physical endpoints and we are not using a load balancer service to hide the mapping of a logical URI to physical URI.
     */
    public HttpProxyRule to(String destinationUriTemplate) {
        to(new UriTemplateDefinition(destinationUriTemplate));
        return this;
    }

    public HttpProxyRule to(UriTemplateDefinition templateDefinition) {
        destinationUriTemplates.add(templateDefinition);
        return this;
    }


    // Properties
    //-------------------------------------------------------------------------

    public UriTemplate getUriTemplateObject() {
        return getUriTemplate().getUriTemplateObject();
    }

    public UriTemplateDefinition getUriTemplate() {
        return uriTemplate;
    }

    public HttpProxyRule setUriTemplate(UriTemplateDefinition uriTemplate) {
        this.uriTemplate = uriTemplate;
        return this;
    }

    public Set<UriTemplateDefinition> getDestinationUriTemplates() {
        return destinationUriTemplates;
    }

    public HttpProxyRule setDestinationUriTemplates(Set<UriTemplateDefinition> destinationUriTemplates) {
        this.destinationUriTemplates = destinationUriTemplates;
        return this;
    }

    /**
     * Returns whether or not reverseHeaders is enabled.
     * <p/>
     * If enabled then the URL in the Location, Content-Location and URI headers from the proxied HTTP responses are rewritten from the back end service URL to match the front end URL on the gateway.
     * This is equivalent to the ProxyPassReverse directive in mod_proxy.
     */
    public boolean isReverseHeaders() {
        return reverseHeaders;
    }

    public HttpProxyRule setReverseHeaders(boolean reverseHeaders) {
        this.reverseHeaders = reverseHeaders;
        return this;
    }

    /**
     * Returns the kind of load balancing strategy used to bridge from
     */
    public LoadBalancerDefinition getLoadBalancer() {
        return loadBalancer;
    }

    public HttpProxyRule setLoadBalancer(LoadBalancerDefinition loadBalancer) {
        this.loadBalancer = loadBalancer;
        return this;
    }

    /**
     * Returns the value that should be used when rewriting the {@code path} attribute
     * of the {@code Set-Cookie} header.
     * <p>
     * If the path was passed unmodified by the proxy it would case the browser to not send the cookie for
     * subsequent requests as the browser will think that the cookie is not for that path. By being able
     * to specify the path the browser will include the cookie when calling the proxy.
     *
     * @return {@code String} the path to be used in replacement of {@code path} attribute from the backend service.
     */
    public String getCookiePath() {
        return cookiePath;
    }

    /**
     * Sets the value that should be used when rewriting the {@code path} attribute
     * of the {@code Set-Cookie} header.
     * <p>
     * If the path was passed unmodified by the proxy it would cause the browser to not send the cookie for
     * subsequent requests as the browser will think that the cookie is not for that path. By being able
     * to specify the path the browser will include the cookie when calling the proxy.
     *
     * @param cookiePath the path to be used in replacement of {@code path} attribute from the backend service.
     */
    public HttpProxyRule setCookiePath(String cookiePath) {
        this.cookiePath = cookiePath;
        return this;
    }

    /**
     * Returns the value that should be used when rewriting the {@code domain} attribute
     * of the {@code Set-Cookie} header.
     * <p>
     * If the domain was passed unmodified by the proxy it would cause the browser to not send the cookie for
     * subsequent requests as the browser will think that the cookie is not for that domain. By being able
     * to specify the path the browser will include the cookie when calling the proxy.
     *
     * @return {@code String} the path to be used in replacement of {@code domain} attribute from the backend service.
     */
    public String getCookieDomain() {
        return cookieDomain;
    }

    /**
     * Sets the value that should be used when rewriting the {@code domain} attribute
     * of the {@code Set-Cookie} header.
     * <p>
     * If the domain was passed unmodified by the proxy it would cause the browser to not send the cookie for
     * subsequent requests as the browser will think that the cookie is not for that domain. By being able
     * to specify the path the browser will include the cookie when calling the proxy.
     *
     * @param cookieDomain the path to be used in replacement of {@code domain} attribute from the backend service.
     *
     */
    public HttpProxyRule setCookieDomain(String cookieDomain) {
        this.cookieDomain = cookieDomain;
        return this;
    }
}
