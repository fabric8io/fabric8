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
package io.fabric8.gateway.api.handlers.http;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 */
public class HttpGatewayClient {
    private static final transient Logger LOG = LoggerFactory.getLogger(HttpGatewayClient.class);

    private final Vertx vertx;
    private final HttpGateway httpGateway;
    private final ObjectMapper mapper = new ObjectMapper();

    public HttpGatewayClient(Vertx vertx, HttpGateway httpGateway) {
        this.vertx = vertx;
        this.httpGateway = httpGateway;
    }

	public void execute(final HttpServerRequest request, final Object apiManagerResponseHandler) {
    	
        String uri = request.uri();
        String uri2 = null;
        if (!uri.endsWith("/")) {
            uri2 = uri + "/";
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Proxying request: " + uri);
        }

        // lets map the request URI to map to the service URI and then the renaming URI
        // using mapping rules...
        HttpClient client = null;
        String remaining = null;
        String prefix = null;
        String proxyServiceUrl = null;
        String reverseServiceUrl = null;
        Map<String, IMappedServices> mappingRules = httpGateway.getMappedServices();
        try {
            if (isMappingIndexRequest(request)) {
                // lets return the JSON of all the results
                String json = mappingRulesToJson(mappingRules);
                HttpServerResponse httpServerResponse = request.response();
                httpServerResponse.headers().set("ContentType", "application/json");
                httpServerResponse.setStatusCode(200);
                httpServerResponse.end(json);
            } else {
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
                            remaining = uri.substring(pathPrefixLength);
                        } else {
                            remaining = null;
                        }

                        // now lets pick a service for this path
                        proxyServiceUrl = mappedServices.chooseService(request);
                        if (proxyServiceUrl != null) {
                            // lets create a client for this request...
                            try {
                                clientURL = new URL(proxyServiceUrl);
                                client = createClient(clientURL);
                                prefix = clientURL.getPath();
                                reverseServiceUrl = request.absoluteURI().resolve(pathPrefix).toString();
                                if (reverseServiceUrl.endsWith("/")) {
                                    reverseServiceUrl = reverseServiceUrl.substring(0, reverseServiceUrl.length() - 1);
                                }
                                break;
                            } catch (MalformedURLException e) {
                                LOG.warn("Failed to parse URL: " + proxyServiceUrl + ". " + e, e);
                            }
                        }
                    }
                }

                if (client != null) {
                    String servicePath = prefix != null ? prefix : "";
                    // we should usually end the prefix path with a slash for web apps at least
                    if (servicePath.length() > 0 && !servicePath.endsWith("/")) {
                        servicePath += "/";
                    }
                    if (remaining != null) {
                        servicePath += remaining;
                    }

                    LOG.info("Proxying request " + uri + " to service path: " + servicePath + " on service: " + proxyServiceUrl + " reverseServiceUrl: " + reverseServiceUrl);
                    final HttpClient finalClient = client;
                    
                    Handler<HttpClientResponse> serviceResponseHandler = httpGateway.getApiManagerService().createServiceResponseHandler(finalClient, request, apiManagerResponseHandler);
                    
                    if (mappedServices != null) {
                        ProxyMappingDetails proxyMappingDetails = new ProxyMappingDetails(proxyServiceUrl, reverseServiceUrl, servicePath);
                        serviceResponseHandler = mappedServices.wrapResponseHandlerInPolicies(request, serviceResponseHandler, proxyMappingDetails);
                    }
                    
                    final HttpClientRequest clientRequest = client.request(request.method(), servicePath, serviceResponseHandler);
                    clientRequest.headers().set(request.headers());
                    clientRequest.setChunked(true);
                    request.dataHandler(new Handler<Buffer>() {
                        public void handle(Buffer data) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Proxying request body:" + data);
                            }
                            clientRequest.write(data);
                        }
                    });
                    request.endHandler(new VoidHandler() {
                        public void handle() {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("end of the request");
                            }
                            clientRequest.end();
                        }
                    });

                } else {
                    //  lets return a 404
                    LOG.info("Could not find matching proxy path for " + uri + " from paths: " + mappingRules.keySet());
                    HttpServerResponse httpServerResponse = request.response();
                    httpServerResponse.setStatusCode(404);
                    httpServerResponse.setStatusMessage("Could not find matching proxy path for " + uri + " from paths: " + mappingRules.keySet());
                    httpServerResponse.end();
                }
            }
        } catch (Throwable e) {
            LOG.error("Caught: " + e, e);
            request.response().setStatusCode(404);
            StringWriter buffer = new StringWriter();
            e.printStackTrace(new PrintWriter(buffer));
            request.response().setStatusMessage("Error: " + e + "\nStack Trace: " + buffer);
            request.response().close();
        }
    }

    protected String mappingRulesToJson(Map<String, IMappedServices> rules) throws IOException {
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

    protected boolean isMappingIndexRequest(HttpServerRequest request) {
        if (httpGateway == null || !httpGateway.isEnableIndex()) {
            return false;
        }
        String uri = request.uri();
        return uri == null || uri.length() == 0 || request.path().equals("/");
    }

    protected HttpClient createClient(URL url) throws MalformedURLException {
        // lets create a client
        HttpClient client = vertx.createHttpClient();
        client.setHost(url.getHost());
        client.setPort(url.getPort());
        return client;

    }

}
