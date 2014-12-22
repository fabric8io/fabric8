package io.fabric8.gateway.apiman;

import io.apiman.gateway.api.rest.contract.exceptions.NotAuthorizedException;
import io.apiman.gateway.engine.IEngine;

public interface ApiManEngine extends IEngine {

	public String serviceMapping(String orgId, String serviceId, String version) throws NotAuthorizedException;
}
