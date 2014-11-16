package io.fabric8.gateway.api.apimanager;


public class ApiManager {

	/**
     * The Gateway will be a simple proxy, or send requests via an API Manager
     * true - gateway requests are routed via an API Manager
     * false - gateway requests are not send via an API Manager and the gateway functions
     * as a simple proxy.
     */
	private boolean isApiManagerEnabled = false;
	private ApiManagerService service;

	public boolean isApiManagerEnabled() {
		return isApiManagerEnabled;
	}
	
	public ApiManagerService getService() {
		return service;
	}
	public void setService(ApiManagerService service) {
		this.service = service;
		if (this.service != null) {
			isApiManagerEnabled = true;
		} else {
			isApiManagerEnabled = false;
		}
	}
	
	
}
