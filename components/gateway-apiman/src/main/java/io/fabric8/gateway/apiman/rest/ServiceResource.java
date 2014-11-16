package io.fabric8.gateway.apiman.rest;

import org.overlord.apiman.rt.api.rest.contract.IServiceResource;
import org.overlord.apiman.rt.api.rest.contract.exceptions.NotAuthorizedException;
import org.overlord.apiman.rt.engine.IEngine;
import org.overlord.apiman.rt.engine.beans.Service;
import org.overlord.apiman.rt.engine.beans.exceptions.PublishingException;

public class ServiceResource implements IServiceResource {

	private IEngine engine;
	
	public ServiceResource(IEngine engine) {
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

}
