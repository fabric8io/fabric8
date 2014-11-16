package io.fabric8.gateway.apiman.rest;

import org.overlord.apiman.rt.api.rest.contract.IApplicationResource;
import org.overlord.apiman.rt.api.rest.contract.exceptions.NotAuthorizedException;
import org.overlord.apiman.rt.engine.IEngine;
import org.overlord.apiman.rt.engine.beans.Application;
import org.overlord.apiman.rt.engine.beans.exceptions.RegistrationException;

public class ApplicationResource implements IApplicationResource {
	
	private IEngine engine;
	
	public ApplicationResource(IEngine engine) {
		super();
		this.engine = engine;
	}

	

	@Override
	public void register(Application application) throws RegistrationException,
			NotAuthorizedException {
		engine.registerApplication(application);
	}

	@Override
	public void unregister(String organizationId, String applicationId, String version)
			throws RegistrationException, NotAuthorizedException {
		engine.unregisterApplication(organizationId, applicationId, version);
	}

}
