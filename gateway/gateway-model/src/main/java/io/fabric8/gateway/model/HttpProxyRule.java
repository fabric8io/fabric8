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

import io.fabric8.gateway.model.loadbalancer.LoadBalancerConfig;
import io.fabric8.gateway.support.MappingResult;
import io.fabric8.gateway.support.UriTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a HTTP mapping rule from an input URI pattern to some back end service
 */
public class HttpProxyRule {
    private static final transient Logger LOG = LoggerFactory.getLogger(HttpProxyRule.class);

    private String uriTemplate;
    private boolean reverseHeaders = true;
    private LoadBalancerConfig loadBalancer;
    private AtomicReference<UriTemplate> uriTemplateReference = new AtomicReference<UriTemplate>();

    public MappingResult matches(String[] paths) {
        UriTemplate template = getUriTemplateObject();
        if (template == null) {
            LOG.warn("getUriTemplateObject() returned null!");
            return null;
        }
        return template.matches(paths);
    }


    // Properties
    //-------------------------------------------------------------------------

    /**
     * Returns the URI template mapping the URI to the underlying back end service.
     */
    public String getUriTemplate() {
        return uriTemplate;
    }

    public void setUriTemplate(String uriTemplate) {
        this.uriTemplate = uriTemplate;
        uriTemplateReference.set(null);
    }

    /**
     * Returns the {@link io.fabric8.gateway.support.UriTemplate} instance which is lazily constructed
     * from the {@link #getUriTemplate()} value.
     */
    public UriTemplate getUriTemplateObject() {
        uriTemplateReference.compareAndSet(null, new UriTemplate(getUriTemplate()));
        return uriTemplateReference.get();
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
    public LoadBalancerConfig getLoadBalancer() {
        return loadBalancer;
    }

    public void setLoadBalancer(LoadBalancerConfig loadBalancer) {
        this.loadBalancer = loadBalancer;
    }
}
