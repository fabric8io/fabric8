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

import io.apiman.gateway.api.rest.contract.IServiceResource;
import io.apiman.gateway.api.rest.contract.exceptions.NotAuthorizedException;
import io.apiman.gateway.engine.beans.Service;
import io.apiman.gateway.engine.beans.ServiceEndpoint;
import io.apiman.gateway.engine.beans.exceptions.PublishingException;
import io.fabric8.gateway.apiman.ApiManEngine;
/**
 * Implements the IServiceResource interface to set up a REST
 * interface to configure Services in APIMan Engine.
 */
public class ServiceResource implements IServiceResource {

	private ApiManEngine engine;
	
	public ServiceResource(ApiManEngine engine) {
		super();
		this.engine = engine;
	}

	@Override
	public void publish(Service service) throws PublishingException,
			NotAuthorizedException {
		engine.publishService(service);
	}

	@Override
	public void retire(String organizationId, String serviceId, String version) throws PublishingException,
			NotAuthorizedException {
		engine.retireService(organizationId, serviceId, version);
	}

	@Override
	public ServiceEndpoint getServiceEndpoint(String orgId, String serviceId, String version) 
			throws NotAuthorizedException {
		ServiceEndpoint serviceEndpoint = new ServiceEndpoint();
		serviceEndpoint.setEndpoint(engine.serviceMapping(orgId, serviceId, version));
		return serviceEndpoint;
	}

}
