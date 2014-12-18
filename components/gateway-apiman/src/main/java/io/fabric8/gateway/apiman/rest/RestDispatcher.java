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

import io.apiman.gateway.engine.IEngine;
import io.apiman.gateway.engine.beans.Application;
import io.apiman.gateway.engine.beans.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

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
	
	public void dispatch(final HttpServerRequest request, final IEngine engine) {
		
		//check headers
		if (request.method().equals("PUT")) {
			String contentType = request.headers().get("Content-Type");
			if (contentType==null || !contentType.startsWith("application/json")) {
				request.response().setStatusCode(403);
				request.response().end("Expecting Content-Type of 'application/json'");
				request.response().close();
				return;
			} 
			if (contentType==null || !contentType.endsWith("UTF-8")) {
				request.response().setStatusCode(403);
				request.response().end("Expecting charset of 'UTF-8'");
				request.response().close();
				return;
			}
		}
		//read the body
		request.bodyHandler(new Handler<Buffer>() {

			@Override
			public void handle(Buffer event) {
				
				String body = event.getString(0, event.length());
				String uri = request.uri().substring(0,request.uri().lastIndexOf("/")+1);
				switch (uri) {
				case "/rest/apimanager/api/applications/":
					ApplicationResource applicationResource = new ApplicationResource(engine);
					try {
						if (request.method().equals("PUT")) {
							Application application = getObjectMapper().readValue(body, Application.class);
							applicationResource.register(application);
							request.response().setStatusCode(200);
						} 
						else if (request.method().equals("DELETE"))  {
							String applicationId = request.params().get("applicationId");
							String organizationId = request.params().get("organizationId");
					        String version = request.params().get("version");
							applicationResource.unregister(organizationId, applicationId, version);
						} else {
							request.response().setStatusCode(404);
							request.response().setStatusMessage("Method not Supported");
						}
						request.response().end();
						request.response().close();
					} catch (Throwable e) {
						LOG.error(e.getMessage(),e);
						request.response().setStatusCode(500);
						request.response().setStatusMessage(e.getMessage());
						request.response().end();
						request.response().close();
					}
					break;
					
				case "/rest/apimanager/api/services/":
					ServiceResource serviceResource = new ServiceResource(engine);
					try {
						if (request.method().equals("PUT")) {
							Service service = getObjectMapper().readValue(body, Service.class);
							serviceResource.publish(service);
							request.response().setStatusCode(200);
						} 
						else if (request.method().equals("DELETE"))  {
							String serviceId = request.params().get("serviceId");
					        String organizationId = request.params().get("organizationId");
					        String version = request.params().get("version");
							serviceResource.retire(organizationId, serviceId, version);
							request.response().setStatusCode(200);
						} else {
							request.response().setStatusCode(404);
							request.response().setStatusMessage("Method not Supported");
						}
						request.response().end();
						request.response().close();
					} catch (Throwable e) {
						LOG.error(e.getMessage(),e);
						request.response().setStatusCode(500);
						request.response().setStatusMessage(e.getMessage());
						request.response().close();
					}
					break;

				default:
					request.response().setStatusCode(404);
					request.response().setStatusMessage("No Such Service");
					request.response().end();
					request.response().close();
					break;
				}
			};
		});
				
	}
	
}
