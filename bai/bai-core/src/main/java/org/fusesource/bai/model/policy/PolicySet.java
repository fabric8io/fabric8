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
import java.util.Set;

import org.fusesource.bai.model.policy.Constants.ActionType;
import org.fusesource.bai.model.policy.Constants.FilterElement;

/**
 * This class takes a Policy set and returns commonly requested information.
 * @author raul
 */
public class PolicySet extends HashSet<Policy> {

	private static final long serialVersionUID = 531594759418489911L;

	public PolicySet() { }
	
	public PolicySet(Set<Policy> setOfPolicies) {
		this.addAll(setOfPolicies);
	}
	
	public PolicySet queryWithScope(FilterElement e) {
		PolicySet answer = new PolicySet();
		for (Policy policy : this) {
            if (!policy.hasScopes()) continue;
            Scope scope = policy.scope.queryWithFilterElement(e);
            if (scope != null) {
            	answer.add(policy);
            }
		}
		return answer;
	}
	
	public PolicySet queryWithSingleScope(FilterElement e) {
		PolicySet answer = new PolicySet();
		for (Policy policy : this) {
            if (!policy.hasScopes() || policy.scope.size() > 1) continue;
            Scope scope = policy.scope.queryWithFilterElement(e);
            if (scope != null) {
            	answer.add(policy);
            }
		}
		return answer;
	}
	
	public PolicySet queryAllExclusions() {
		PolicySet answer = new PolicySet();
		for (Policy policy : this) {
			if (policy.action.type != ActionType.EXCLUDE) continue;
			answer.add(policy);
		}
		return answer;
	}
	
	public PolicySet queryAllInclusions() {
		PolicySet answer = new PolicySet();
		for (Policy policy : this) {
			if (policy.action.type != ActionType.INCLUDE) continue;
			answer.add(policy);
		}
		return answer;
	}
	
	public Policy getOne() {
		return this.iterator().next();
	}

}
