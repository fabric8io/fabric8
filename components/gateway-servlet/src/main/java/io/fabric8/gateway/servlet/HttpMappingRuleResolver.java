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
package io.fabric8.gateway.servlet;

import io.fabric8.gateway.model.HttpProxyRuleBase;
import io.fabric8.gateway.support.MappingResult;
import io.fabric8.gateway.support.MappingRuleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Takes a collection of mapping rules and builds a resolver so that we can find the mapping rules
 * for a given request and response.
 */
public class HttpMappingRuleResolver {
    private MappingRuleResolver resolver = new MappingRuleResolver();

    public HttpMappingResult findMappingRule(HttpServletRequest request, HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        MappingResult answer = null;
        if (contextPath != null && contextPath.length() > 0 && !contextPath.equals("/")) {
            String requestWithoutContextPath = requestURI.substring(contextPath.length());
            answer = resolver.findMappingRule(requestWithoutContextPath);
        }
        if (answer == null) {
            // lets try the full request URI with the context path to see if that maps
            answer = resolver.findMappingRule(requestURI);
        }
        return answer != null ? new HttpMappingResult(answer) : null;
    }

    public void setMappingRules(HttpProxyRuleBase mappingRules) {
        resolver.setMappingRules(mappingRules);
    }

    public HttpProxyRuleBase getMappingRules() {
        return resolver.getMappingRules();
    }
}
