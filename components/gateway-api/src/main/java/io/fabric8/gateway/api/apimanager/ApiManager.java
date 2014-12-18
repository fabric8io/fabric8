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
