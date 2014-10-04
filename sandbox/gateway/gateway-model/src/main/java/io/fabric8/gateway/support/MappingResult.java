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
package io.fabric8.gateway.support;

import io.fabric8.gateway.loadbalancer.ClientRequestFacade;
import io.fabric8.gateway.loadbalancer.LoadBalancer;
import io.fabric8.gateway.model.HttpProxyRule;
import io.fabric8.gateway.model.UriTemplateDefinition;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a mapping result
 */
public class MappingResult {
    private final Map<String, String> parameterNameValues;
    private final String[] requestUriPaths;
    private final HttpProxyRule proxyRule;

    public MappingResult(Map<String, String> parameterNameValues, String[] requestUriPaths, HttpProxyRule proxyRule) {
        this.requestUriPaths = requestUriPaths;
        this.proxyRule = proxyRule;
        this.parameterNameValues = Collections.unmodifiableMap(new HashMap<String, String>(parameterNameValues));
    }

    /**
     * Returns the resulting proxy URL from the mapping rule
     */
    public String getDestinationUrl(ClientRequestFacade requestFacade) {
        UriTemplateDefinition uriTemplateDefinition = proxyRule.chooseBackEndService(requestFacade);
        if (uriTemplateDefinition != null) {
            UriTemplate uriTemplate = uriTemplateDefinition.getUriTemplateObject();
            if (uriTemplate != null) {
                return uriTemplate.bindByName(parameterNameValues);
            }
        }
        return null;
    }

    /**
     * Returns a mapping of the URI templates to the mapped values
     */
    public Map<String, String> getParameterNameValues() {
        return parameterNameValues;
    }

    /**
     * Returns the paths from the request URI
     */
    public String[] getRequestUriPaths() {
        return requestUriPaths;
    }

    /**
     * Returns the proxy mapping rule that matched
     */
    public HttpProxyRule getProxyRule() {
        return proxyRule;
    }
}
