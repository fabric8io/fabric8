/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.gateway.handlers.http;

import org.fusesource.common.util.Strings;
import org.fusesource.gateway.ServiceDetails;
import org.fusesource.gateway.ServiceMap;
import org.fusesource.gateway.chooser.DefaultHttpChooser;
import org.fusesource.gateway.chooser.HttpChooser;
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 */
public class HttpGatewayHandler implements Handler<HttpServerRequest> {
    private static final transient Logger LOG = LoggerFactory.getLogger(HttpGatewayHandler.class);

    private final Vertx vertx;
    private final ServiceMap serviceMap;
    private HttpChooser chooser = new DefaultHttpChooser();

    public HttpGatewayHandler(Vertx vertx, ServiceMap serviceMap) {
        this.vertx = vertx;
        this.serviceMap = serviceMap;
    }

    @Override
    public void handle(final HttpServerRequest request) {
        String uri = request.uri();
        System.out.println("Proxying request: " + uri);

        // lets map the request URI to map to the service URI and then the renaming URI
        // using mapping rules...
        HttpClient client = null;

        String remaining = null;
        String prefix = null;

        List<String> paths = serviceMap.getPaths();
        for (String path : paths) {
            if (uri.startsWith(path)) {
                remaining = uri.substring(path.length());
                // now lets pick a service for this path

                List<ServiceDetails> services = serviceMap.getServices(path);
                if (!services.isEmpty()) {
                    ServiceDetails serviceDetails = chooser.chooseService(request, services);
                    if (serviceDetails != null) {
                        List<String> urlStrings = serviceDetails.getServices();
                        if (urlStrings.size() > 0) {
                            String urlText = urlStrings.get(0);
                            if (Strings.notEmpty(urlText)) {
                                // lets create a client for this request...
                                try {
                                    URL url = new URL(urlText);
                                    client = createClient(url);
                                    prefix = url.getPath();
                                    break;
                                } catch (MalformedURLException e) {
                                    LOG.warn("Failed to parse URL: " + urlText + ". " + e, e);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (client != null) {
            String actualUrl = prefix != null ? prefix : "";
            if (remaining != null) {
                if (actualUrl.length() > 0 && !actualUrl.endsWith("/")) {
                    actualUrl += "/";
                }
                actualUrl += remaining;
            }
            final HttpClientRequest clientRequest = client.request(request.method(), actualUrl, new Handler<HttpClientResponse>() {
                public void handle(HttpClientResponse clientResponse) {
                    System.out.println("Proxying response: " + clientResponse.statusCode());
                    request.response().setStatusCode(clientResponse.statusCode());
                    request.response().headers().set(clientResponse.headers());
                    request.response().setChunked(true);
                    clientResponse.dataHandler(new Handler<Buffer>() {
                        public void handle(Buffer data) {
                            System.out.println("Proxying response body:" + data);
                            request.response().write(data);
                        }
                    });
                    clientResponse.endHandler(new VoidHandler() {
                        public void handle() {
                            request.response().end();
                        }
                    });
                }
            });
            clientRequest.headers().set(request.headers());
            clientRequest.setChunked(true);
            request.dataHandler(new Handler<Buffer>() {
                public void handle(Buffer data) {
                    System.out.println("Proxying request body:" + data);
                    clientRequest.write(data);
                }
            });
            request.endHandler(new VoidHandler() {
                public void handle() {
                    System.out.println("end of the request");
                    clientRequest.end();
                }
            });

        } else {
            //  lets return a 404
            request.response().setStatusCode(404);
            request.response().close();
        }
    }

    protected HttpClient createClient(URL url) throws MalformedURLException {
        // lets create a client
        HttpClient client = vertx.createHttpClient();
        client.setHost(url.getHost());
        client.setPort(url.getPort());
        return client;

    }

}
