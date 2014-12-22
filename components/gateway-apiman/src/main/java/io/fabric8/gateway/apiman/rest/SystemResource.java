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

import io.apiman.gateway.api.rest.contract.ISystemResource;
import io.apiman.gateway.engine.IEngine;
import io.apiman.gateway.engine.beans.SystemStatus;
/**
 * Implements the IServiceResource interface to set up a REST
 * interface to configure Services in APIMan Engine.
 */
public class SystemResource implements ISystemResource {

	private IEngine engine;
	
	public SystemResource(IEngine engine) {
		super();
		this.engine = engine;
	}

	@Override
	public SystemStatus getStatus() {
		SystemStatus status = new SystemStatus();
		if (engine != null) {
			status.setUp(true);
			status.setVersion(engine.getVersion());
		} else {
			status.setUp(false);
		}
		return status;
	}



}
