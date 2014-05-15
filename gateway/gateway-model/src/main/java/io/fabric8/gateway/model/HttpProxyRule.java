/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.gateway.model;

import io.fabric8.gateway.model.loadbalancer.LoadBalancerDefinition;
import io.fabric8.gateway.support.MappingResult;
import io.fabric8.gateway.support.UriTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a HTTP mapping rule from an input URI pattern to some back end service
 */
public class HttpProxyRule {
    private static final transient Logger LOG = LoggerFactory.getLogger(HttpProxyRule.class);

    private UriTemplateDefinition uriTemplate;
    private boolean reverseHeaders = true;
    private LoadBalancerDefinition loadBalancer;
    private Set<UriTemplateDefinition> destinationUriTemplates = new HashSet<UriTemplateDefinition>();

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
        return template.matches(paths);
    }


    // DSL
    //-------------------------------------------------------------------------

    /**
     * Adds a destination URI template mapping; such as a physical endpoint we can proxy to if there are multiple possible physical endpoints and we are not using a load balancer service to hide the mapping of a logical URI to physical URI.
     */
    public void to(String destinationUriTemplate) {
        to(new UriTemplateDefinition(destinationUriTemplate));
    }

    public void to(UriTemplateDefinition templateDefinition) {
        destinationUriTemplates.add(templateDefinition);
    }


    // Properties
    //-------------------------------------------------------------------------

    public UriTemplate getUriTemplateObject() {
        return getUriTemplate().getUriTemplateObject();
    }

    public UriTemplateDefinition getUriTemplate() {
        return uriTemplate;
    }

    public void setUriTemplate(UriTemplateDefinition uriTemplate) {
        this.uriTemplate = uriTemplate;
    }

    public Set<UriTemplateDefinition> getDestinationUriTemplates() {
        return destinationUriTemplates;
    }

    public void setDestinationUriTemplates(Set<UriTemplateDefinition> destinationUriTemplates) {
        this.destinationUriTemplates = destinationUriTemplates;
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

    public void setReverseHeaders(boolean reverseHeaders) {
        this.reverseHeaders = reverseHeaders;
    }

    /**
     * Returns the kind of load balancing strategy used to bridge from
     */
    public LoadBalancerDefinition getLoadBalancer() {
        return loadBalancer;
    }

    public void setLoadBalancer(LoadBalancerDefinition loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

}
