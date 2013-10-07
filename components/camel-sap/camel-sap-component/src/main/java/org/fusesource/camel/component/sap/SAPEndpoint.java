/**
 * Copyright 2013 Red Hat, Inc.
 * 
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 */
package org.fusesource.camel.component.sap;

import org.apache.camel.ExchangePattern;
import org.apache.camel.impl.DefaultEndpoint;
import org.fusesource.camel.component.sap.model.rfc.Structure;

/**
 * Represents an SAP endpoint.
 * 
 * @author William Collins <punkhornsw@gmail.com>
 *
 */
public abstract class SAPEndpoint extends DefaultEndpoint {

	protected String rfcName;
	protected ExchangePattern mep = ExchangePattern.InOut;

	public SAPEndpoint() {
	}

	public SAPEndpoint(String uri, SAPComponent component) {
		super(uri, component);
	}

	public boolean isSingleton() {
		return false;
	}

	public abstract boolean isServer();

	public String getRfcName() {
		return rfcName;
	}

	public void setRfcName(String rfcName) {
		this.rfcName = rfcName;
	}

	public ExchangePattern getMep() {
		return mep;
	}

	public void setMep(ExchangePattern mep) {
		this.mep = mep;
	}
	
	public abstract Structure getRequest() throws Exception;
	
	public abstract Structure getResponse() throws Exception;
	
}
