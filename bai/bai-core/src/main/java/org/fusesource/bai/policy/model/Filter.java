/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.bai.policy.model;

import org.fusesource.bai.policy.model.Constants.ScopeElement;

/**
 * Represents a BAI Policy Filter.
 * @author Raul Kripalani
 *
 */
public abstract class Filter {
	
	protected ScopeElement element;
	protected Policy policy;
	
	public Filter(ScopeElement element, Policy policy) {
		this.element = element;
		this.policy = policy;
	}

	public ScopeElement getElement() {
		return element;
	}

	public void setElement(ScopeElement element) {
		this.element = element;
	}

	public Policy getPolicy() {
		return policy;
	}

	public void setPolicy(Policy policy) {
		this.policy = policy;
	}

	public abstract boolean matches(Object o);

}
