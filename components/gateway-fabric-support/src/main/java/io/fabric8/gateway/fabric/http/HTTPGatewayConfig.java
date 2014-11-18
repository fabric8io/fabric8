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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HTTPGatewayConfig extends HashMap<String, String> {

    private static final long serialVersionUID = -7563354951392959816L;
    /** The host name used when listening for HTTP traffic" */
    public final static String HOST = "HOST";
    /** Port number to listen on for HTTP requests") */
    public final static String HTTP_PORT = "HTTP_PORT";
    /** If enabled then performing a HTTP GET on the path '/' will return a JSON representation of the gateway mappings */
    public final static String ENABLE_INDEX = "ENABLE_INDEX";
    /** Url to the Kubernetes Master */
    public final static String KUBERNETES_MASTER = "KUBERNETES_MASTER";
    /** The selector in Kubernetes which is monitored to discover the available web services or web applications */
    public final static String SELECTOR = "GATEWAY_SERVICES_SELECTOR";
    /** The url template to use, for example: '/api/{contextPath}' */
    public final static String URI_TEMPLATE = "URI_TEMPLATE";
    /** Specify the exact profile version to expose; if none is specified then the 
     * gateway's current profile version is used. If a {version} URI template 
     * is used then all versions are exposed. */
    public final static String ENABLED_VERSION = "ENABLED_VERSION";
    /** If enabled then the URL in the Location, Content-Location and URI headers from 
     * the proxied HTTP responses are rewritten from the back end service URL to match the 
     * front end URL on the gateway.This is equivalent to the ProxyPassReverse directive
     * in mod_proxy.")
     */
    public final static String REVERSE_HEADERS = "REVERSE_HEADERS";
    /** The loadbalancer to use in the gateway */
    public final static String LOAD_BALANCER = "LOAD_BALANCER";
    
    public int getPort() {
        return Integer.parseInt(get(HTTP_PORT));
    }
    
    public String getHost() {
        return get(HOST);
    }
    
    public boolean isIndexEnabled() {
        return Boolean.parseBoolean(get(ENABLE_INDEX));
    }
    
    public String getKubernetesMaster() {
    	return get(KUBERNETES_MASTER);
    }
    /** Returns the selector which will be used to select services that
     * will be proxied by the gqteway. It expects an input string formatted
     * as a comma separated list of key-value pairs.
     * example: "[{container=java, group=quickstarts}, {container=camel, group=quickstarts}]"
     * @return 
     * @throws IOException 
     */
    public List<Map<String,String>> getServiceSelectors() throws IOException {
        return parseSelectorConfig(get(SELECTOR));
    }
    
    public String getLoadBalancerType() {
        return get(LOAD_BALANCER);
    }
    
    public String getUriTemplate() {
        return get(URI_TEMPLATE);
    }
    
    public String getEnabledVersiond() {
        return get(ENABLED_VERSION);
    }
    public boolean isReverseHeaders() {
        return Boolean.parseBoolean(get(REVERSE_HEADERS));
    }
    public static List<Map<String,String>> parseSelectorConfig(String selectorConfig) throws IOException {
    	ObjectMapper mapper = new ObjectMapper();
    	TypeReference<List<Map<String,String>>> typeRef = new TypeReference<List<Map<String,String>>>() {};
        return mapper.readValue(selectorConfig, typeRef);
    }
}
