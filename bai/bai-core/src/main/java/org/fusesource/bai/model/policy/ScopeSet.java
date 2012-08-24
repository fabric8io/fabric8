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

import java.util.HashSet;

import org.fusesource.bai.model.policy.Constants.FilterElement;
import org.fusesource.bai.model.policy.Constants.FilterMethod;

public class ScopeSet extends HashSet<Scope> {

	private static final long serialVersionUID = -1137830006055808385L;

	public Scope queryWithFilterElement(FilterElement e) {
		for (Scope scope : this) {
			if (scope.filterElement != e) continue;
			return scope;
		}
		return null;
	}
	
	public ScopeSet queryWithFilterMethod(FilterMethod m) {
		ScopeSet answer = new ScopeSet();
		for (Scope scope : this) {
			if (scope.filterMethod == m) {
				answer.add(scope);
			}
		}
		return answer;
	}
	
	public ScopeSet queryWithExpressionLanguage(String expressionLanguage) {
		ScopeSet answer = new ScopeSet();
		for (Scope scope : this) {
			if (scope.expressionLanguage.equalsIgnoreCase(expressionLanguage)) {
				answer.add(scope);
			}
		}
		return answer;
	}
	
	public ScopeSet queryContainingEnumValue(String enumValue) {
		ScopeSet answer = new ScopeSet();
		for (Scope scope : this) {
			if (scope.enumValues.contains(enumValue)) {
				answer.add(scope);
			}
		}
		return answer;
	}
	
	/**
	 * Constrain new additions to another Scope not existing with the same filter element.
	 */
	@Override
	public boolean add(Scope e) {
		if (this.queryWithFilterElement(e.filterElement) != null) {
			return false;
		}
		return super.add(e);		
	}
	
	public Scope getOne() {
		return this.iterator().next();
	}
	
}
