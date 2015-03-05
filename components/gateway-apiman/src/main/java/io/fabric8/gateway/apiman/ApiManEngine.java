package io.fabric8.gateway.apiman;

import io.apiman.gateway.api.rest.contract.exceptions.NotAuthorizedException;
import io.apiman.gateway.engine.IEngine;
import io.apiman.gateway.engine.beans.Service;

public interface ApiManEngine extends IEngine {

	public String serviceMapping(Service service) throws NotAuthorizedException;
	public String[] getServiceInfo(String serviceBindPath);
}
