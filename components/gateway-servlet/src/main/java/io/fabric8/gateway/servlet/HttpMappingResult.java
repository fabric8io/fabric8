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

import io.fabric8.gateway.loadbalancer.ClientRequestFacade;
import io.fabric8.gateway.model.HttpProxyRule;
import io.fabric8.gateway.support.MappingResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * Represents the result of a HTTP mapping rule.
 */
public class HttpMappingResult {
    private final MappingResult result;

    public HttpMappingResult(MappingResult result) {
        this.result = result;
    }

    public Map<String, String> getParameterNameValues() {
        return result.getParameterNameValues();
    }

    public String getDestinationUrl(ClientRequestFacade requestFacade) {
        return result.getDestinationUrl(requestFacade);
    }

    public String[] getRequestUriPaths() {
        return result.getRequestUriPaths();
    }

    public HttpProxyRule getProxyRule() {
        return result.getProxyRule();
    }

    /**
     * Performs the HTTP request on the back end service
     */
    public void request(HttpServletRequest request, HttpServletResponse response) throws IOException {
    }
}
