/*
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Set;

/**
 * The HttpGatewayServiceClient used Vert.x to create a Vert.x client to relay the client
 * request to the actual back-end service. The request and the response handling are
 * non-blocking. Note that the handling of the response is 
 * different in case APIManagement is used as the APIManager may need need to execute
 * some policies that depend on result of the service call. APIMan runs policies *before* and
 * *after* the service executes.
 */
public class HttpGatewayServiceClient {
    private static final transient Logger LOG = LoggerFactory.getLogger(HttpGatewayServiceClient.class);

    private final Vertx vertx;
    private final HttpGateway httpGateway;
   
    public HttpGatewayServiceClient(Vertx vertx, HttpGateway httpGateway) {
        this.vertx = vertx;
        this.httpGateway = httpGateway;
    }

	public HttpClientRequest execute(final HttpServerRequest request, final Object apiManagerResponseHandler) {
    	
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
                
                Handler<HttpClientResponse> serviceResponseHandler = null;
                
                if (httpGateway.getApiManager().isApiManagerEnabled()) {
                	serviceResponseHandler = httpGateway.getApiManager().getService().createServiceResponseHandler(finalClient, apiManagerResponseHandler);
        		} else {
        			serviceResponseHandler = new HttpServiceResponseHandler(finalClient, request);
        		}
                
                if (mappedServices != null) {
                    ProxyMappingDetails proxyMappingDetails = new ProxyMappingDetails(proxyServiceUrl, reverseServiceUrl, servicePath);
                    serviceResponseHandler = mappedServices.wrapResponseHandlerInPolicies(request, serviceResponseHandler, proxyMappingDetails);
                }
                
                final HttpClientRequest serviceRequest = client.request(request.method(), servicePath, serviceResponseHandler);
                serviceRequest.headers().set(request.headers());
                serviceRequest.setChunked(true);
                
                return serviceRequest;
//                    request.dataHandler(new Handler<Buffer>() {
//                        public void handle(Buffer data) {
//                            if (LOG.isDebugEnabled()) {
//                                LOG.debug("Proxying request body:" + data);
//                            }
//                            serviceRequest.write(data);
//                        }
//                    });
//                    request.endHandler(new VoidHandler() {
//                        public void handle() {
//                            if (LOG.isDebugEnabled()) {
//                                LOG.debug("end of the request");
//                            }
//                            serviceRequest.end();
//                        }
//                    });

            } else {
                //  lets return a 404
                LOG.info("Could not find matching proxy path for " + uri + " from paths: " + mappingRules.keySet());
                HttpServerResponse httpServerResponse = request.response();
                httpServerResponse.setStatusCode(404);
                httpServerResponse.setStatusMessage("Could not find matching proxy path for " + uri + " from paths: " + mappingRules.keySet());
                httpServerResponse.end();
            }
        } catch (Throwable e) {
            LOG.error("Caught: " + e, e);
            request.response().setStatusCode(404);
            StringWriter buffer = new StringWriter();
            e.printStackTrace(new PrintWriter(buffer));
            request.response().setStatusMessage("Error: " + e + "\nStack Trace: " + buffer);
            request.response().close();
        }
        return null;
    }

    


    
    protected boolean isApimanagerRestRequest(HttpServerRequest request) {
        if (httpGateway == null || !httpGateway.isEnableIndex()) {
            return false;
        }
        String uri = request.uri();
        return uri == null || uri.length() == 0 || request.path().startsWith("/rest/apimanager/");
    }

    protected HttpClient createClient(URL url) throws MalformedURLException {
        // lets create a client
        HttpClient client = vertx.createHttpClient();
        client.setHost(url.getHost());
        client.setPort(url.getPort());
        return client;

    }

}
