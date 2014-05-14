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
package io.fabric8.gateway.servlet;

import io.fabric8.gateway.model.HttpProxyRuleBase;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A servlet filter which takes a collection of mapping rules and implements a proxy to it
 */
public abstract class GatewayFilter implements Filter {
    private HttpMappingRuleResolver resolver = new HttpMappingRuleResolver();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        HttpProxyRuleBase ruleBase = new HttpProxyRuleBase();
        loadRuleBase(filterConfig, ruleBase);
        resolver.setMappingRules(ruleBase);
    }

    /**
     * load the mapping rules from the servlet context; could use a Java DSL, the XML DSL or load from a database
     */
    protected abstract void loadRuleBase(FilterConfig filterConfig, HttpProxyRuleBase ruleBase) throws ServletException;

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            doHttpFilter(httpRequest, httpResponse, chain);
        } else {
            chain.doFilter(request, response);
        }
    }

    protected void doHttpFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpMappingResult result = resolver.findMappingRule(request, response);
        if (result != null) {
            result.request(request, response);
        } else {
            chain.doFilter(request, response);
        }
    }
}
