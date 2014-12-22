/*
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.gateway.api.handlers.http;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

public class HttpMapping {
	
	private static final transient Logger LOG = LoggerFactory.getLogger(HttpMapping.class);
	private final static ObjectMapper mapper = new ObjectMapper();

	public static void respond(HttpServerRequest request, HttpGateway httpGateway) {
		try {
			String json = mappingRulesToJson(httpGateway.getMappedServices());
	        HttpServerResponse httpServerResponse = request.response();
	        httpServerResponse.headers().set("ContentType", "application/json");
	        httpServerResponse.setStatusCode(200);
	        httpServerResponse.end(json);
	        httpServerResponse.close();
		} catch (Throwable e) {
            LOG.error("Caught: " + e, e);
            request.response().setStatusCode(404);
            StringWriter buffer = new StringWriter();
            e.printStackTrace(new PrintWriter(buffer));
            request.response().setStatusMessage("Error: " + e + "\nStack Trace: " + buffer);
            request.response().close();
        }
	}
	
    public static boolean isMappingIndexRequest(HttpServerRequest request, HttpGateway httpGateway) {
        if (httpGateway == null || !httpGateway.isEnableIndex()) {
            return false;
        }
        String uri = request.uri();
        return uri == null || uri.length() == 0 || request.path().equals("/");
    }
    
    protected static String mappingRulesToJson(Map<String, IMappedServices> rules) throws IOException {
        Map<String, Collection<String>> data = new HashMap<String, Collection<String>>();

        Set<Map.Entry<String, IMappedServices>> entries = rules.entrySet();
        for (Map.Entry<String, IMappedServices> entry : entries) {
            String key = entry.getKey();
            IMappedServices value = entry.getValue();
            Collection<String> serviceUrls = value.getServiceUrls();
            data.put(key, serviceUrls);
        }
        return mapper.writeValueAsString(data);
    }
    
    public static IMappedServices getMapping(final HttpServerRequest request, Map<String, IMappedServices> mappingRules) {
    	 
    	String uri = request.uri();
        String uri2 = null;
        if (!uri.endsWith("/")) {
            uri2 = uri + "/";
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Proxying request: " + uri);
        }
        
    	String remaining = null;
        String prefix = null;
        String proxyServiceUrl = null;
        String reverseServiceUrl = null;
        
    	IMappedServices mappedServices = null;
         URL clientURL = null;
         Set<Map.Entry<String, IMappedServices>> entries = mappingRules.entrySet();
         for (Map.Entry<String, IMappedServices> entry : entries) {
             String path = entry.getKey();
             mappedServices = entry.getValue();

             String pathPrefix = path;
             if (uri.startsWith(pathPrefix) || (uri2 != null && uri2.startsWith(pathPrefix))) {
                 int pathPrefixLength = pathPrefix.length();
                 if (pathPrefixLength < uri.length()) {
                     remaining = uri.substring(pathPrefixLength+1);
                 } else {
                     remaining = null;
                 }

                 // now lets pick a service for this path
                 proxyServiceUrl = mappedServices.chooseService(request);
                 if (proxyServiceUrl != null) {
                     // lets create a client for this request...
                     try {
                         clientURL = new URL(proxyServiceUrl);
                         prefix = clientURL.getPath();
                         reverseServiceUrl = request.absoluteURI().resolve(pathPrefix).toString();
                         if (reverseServiceUrl.endsWith("/")) {
                             reverseServiceUrl = reverseServiceUrl.substring(0, reverseServiceUrl.length() - 1);
                         }
                         
                         String servicePath = prefix != null ? prefix : "";
                         // we should usually end the prefix path with a slash for web apps at least
                         if (servicePath.length() > 0 && !servicePath.endsWith("/")) {
                             servicePath += "/";
                         }
                         if (remaining != null) {
                             servicePath += remaining;
                         }
                         mappedServices.setProxyMappingDetails(new ProxyMappingDetails(proxyServiceUrl, reverseServiceUrl, servicePath));
                         break;
                     } catch (MalformedURLException e) {
                         LOG.warn("Failed to parse URL: " + proxyServiceUrl + ". " + e, e);
                     }
                 }
             }
         }
         return mappedServices;
    }
}
