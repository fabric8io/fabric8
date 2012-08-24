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

package org.fusesource.bai.model.policy;



/**
 * A Policy defines what should happen ({@link Action} when some set of conditions matches ({@link Scope}.
 * Conditions are defined by a set of Scope filters, that limit the application of the Action.
 * A condition could be: the event comes from a specific context, bundle, etc. or is of a specific type, etc.
 * @author Raul Kripalani
 */
public class Policy {

	/**
	 * One-to-many scope items that restrict the visibility of this {@link Policy}.
	 */
	public ScopeSet scope = new ScopeSet();
	
	/**
	 * One-to-one relationship with an action.
	 */
	public Action action = new Action();
	
	public boolean hasScopes() {
		return !(scope == null || scope.isEmpty());
	}

	/**
	 * Removes redundant {@link Scope} elements if they refer to catch-all wildcards.
	 * There's no value in restricting the Scope of a Policy to "all contexts", for example.
	 */
	public void pruneRedundantScopes() {
		ScopeSet toRemove = new ScopeSet();
		for (Scope s : scope) {
			switch (s.filterElement) {
			// the BUNDLE and CONTEXT expressions are simple wildcard-aware Strings
			case BUNDLE:
			case CONTEXT:
				if (s.expression.matches("\\**")) toRemove.add(s); //regex: \**
				break;
			case EVENT:
			case EXCHANGE:
				// nothing, placeholder for additional logic
				break;
			}

		}
	}
}

