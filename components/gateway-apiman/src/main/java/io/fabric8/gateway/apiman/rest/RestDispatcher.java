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
package io.fabric8.gateway.apiman.rest;

import io.apiman.gateway.engine.async.IAsyncResult;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.Application;
import io.apiman.gateway.engine.beans.Service;
import io.apiman.gateway.engine.beans.ServiceEndpoint;
import io.fabric8.gateway.apiman.ApiManEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RestDispatcher {

	private static final transient Logger LOG = LoggerFactory.getLogger(RestDispatcher.class);
	private ObjectMapper mapper;
	
	public ObjectMapper getObjectMapper() {
		if (mapper == null) {
			mapper = new ObjectMapper();
		}
		return mapper;
	}
	
	public void dispatch(final HttpServerRequest request, final ApiManEngine engine) {
		
		//check headers
		LOG.info("Request: " + request.method() + " " + request.uri() + " from " + request.remoteAddress());
		if (request.method().equals("PUT")) {
			String contentType = request.headers().get("Content-Type");
			contentType = contentType.toUpperCase();
			if (contentType==null || !contentType.startsWith("APPLICATION/JSON")) {
				request.response().setStatusCode(403);
				request.response().end("Expecting Content-Type of 'application/json'");
				LOG.error("Expecting Content-Type of 'application/json");
				return;
			} 
			if (contentType==null || !contentType.endsWith("UTF-8")) {
				request.response().setStatusCode(403);
				request.response().end("Expecting charset of 'UTF-8'");
				request.response().close();
				LOG.error("Expecting charset of 'UTF-8'");
				return;
			}
		}
		//read the body
		request.bodyHandler(new Handler<Buffer>() {
			
			@Override
			public void handle(Buffer event) {
				try {
					String body = event.getString(0, event.length());
					String uri = request.uri().substring(1);
					if (uri.contains("?")) uri = uri.substring(0, uri.indexOf("?"));
					String[] pathSegment = uri.split("/");

					// The async response handler used for all the void IRegistry calls
                    IAsyncResultHandler<Void> voidHandler = new IAsyncResultHandler<Void>() {
                        @Override
                        public void handle(IAsyncResult<Void> result) {
                            if (result.isError()) {
                                Throwable e = result.getError();
                                writeError(request, e);
                            } else {
                                request.response().setStatusCode(200);
                                request.response().end();
                            }
                        }
                    };

					if (uri.startsWith("rest/apimanager/applications")) {
						if (request.method().equals("PUT")) {
							Application application = getObjectMapper().readValue(body, Application.class);
                            engine.getRegistry().registerApplication(application, voidHandler);
						} else if (request.method().equals("DELETE"))  {
							//path {organizationId}/{applicationId}/{version}
							if (pathSegment.length < 6) {
							    throw new UserException("Query Parse Exception , expecting /rest/apimanager/applications/{organizationId}/{applicationId}/{version}");
							}
							Application application = new Application();
					        application.setOrganizationId(pathSegment[3]);
					        application.setApplicationId(pathSegment[4]);
					        application.setVersion(pathSegment[5]);
                            engine.getRegistry().unregisterApplication(application, voidHandler);
						} else {
							throw new UserException("Method not Supported");
						}
					} else if (uri.startsWith("rest/apimanager/services")) {
						if (request.method().equals("PUT")) {
							Service service = getObjectMapper().readValue(body, Service.class);
							engine.getRegistry().publishService(service, voidHandler);
						}
						else if (request.method().equals("DELETE"))  {
							//path {organizationId}/{serviceId}/{version}
							if (pathSegment.length < 6) throw new UserException("Query Parse Exception , "
									+ "expecting /rest/apimanager/applications/{organizationId}/{serviceId}/{version}");
					        Service service = new Service();
					        service.setOrganizationId(pathSegment[3]);
					        service.setServiceId(pathSegment[4]);
					        service.setVersion(pathSegment[5]);
                            engine.getRegistry().retireService(service , voidHandler);
						} else if (request.method().equals("GET")) {
							//path {organizationId}/{serviceId}/{version}
							if (pathSegment.length < 7) throw new UserException("Query Parse Exception , "
									+ "expecting /rest/apimanager/applications/{organizationId}/{serviceId}/{version}/endpoint");
							String organizationId = pathSegment[3];
							String serviceId = pathSegment[4];
					        String version = pathSegment[5];
					        
					        engine.getRegistry().getService(organizationId, serviceId, version, new IAsyncResultHandler<Service>() {
                                @Override
                                public void handle(IAsyncResult<Service> result) {
                                    if (result.isError()) {
                                        Throwable e = result.getError();
                                        writeError(request, e);
                                    } else {
                                        Service service = result.getResult();
                                        ServiceEndpoint serviceEndpoint = new ServiceEndpoint();
                                        serviceEndpoint.setEndpoint(engine.serviceMapping(service));
                                        try {
                                            String json = getObjectMapper().writeValueAsString(serviceEndpoint);
                                            request.response().headers().set("ContentType", "application/json");
                                            request.response().headers().set("Content-Length", String.valueOf(json.length()));
                                            request.response().write(json);
                                            request.response().setStatusCode(200);
                                            request.response().end();
                                        } catch (JsonProcessingException e) {
                                            writeError(request, e);
                                        }
                                    }
                                }
                            });
						} else {
							throw new UserException("Method not Supported");
						}
						
					} else if (uri.startsWith("rest/apimanager/system/status")) {
						SystemResource systemResource = new SystemResource(engine);
						String json = getObjectMapper().writeValueAsString(systemResource.getStatus());
						request.response().headers().set("ContentType", "application/json");
						request.response().headers().set("Content-Length", String.valueOf(json.length()));
						request.response().write(json);
	                    request.response().setStatusCode(200);
	                    request.response().end();
					} else {
						throw new UserException("No Such Service");
					}
				} catch (UserException e) {
					LOG.error(e.getMessage(),e);
					request.response().setStatusCode(404);
					request.response().setStatusMessage(e.getMessage());
					request.response().end();
				} catch (Throwable e) {
				    writeError(request, e);
				}
			}
			
		});
	}
	
    protected static void writeError(final HttpServerRequest request, Throwable e) {
        LOG.error(e.getMessage(), e);
        request.response().setStatusCode(500);
        request.response().setStatusMessage(e.getMessage());
        request.response().end();
    }

}
