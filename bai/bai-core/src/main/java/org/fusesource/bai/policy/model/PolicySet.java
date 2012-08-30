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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.fusesource.bai.policy.model.Constants.ActionType;
import org.fusesource.bai.policy.model.Constants.ScopeElement;

/**
 * Implements convenient operations on sets of Policies. Core element of the BAI Policy API.
 * @author Raul Kripalani
 */
public class PolicySet extends HashSet<Policy> {

	private static final long serialVersionUID = 531594759418489911L;

	public PolicySet() { }
	
	public PolicySet(Set<Policy> setOfPolicies) {
		this.addAll(setOfPolicies);
	}
	
	public PolicySet policiesContainingAnyScopeElements(ScopeElement... e) {
		Set<ScopeElement> criteria = new HashSet<ScopeElement>(Arrays.asList(e));
		PolicySet answer = new PolicySet();
		for (Policy policy : this) {
			// asymmetric difference of the sets
			// if this operation returns true, it means that elements have been removed, i.e. some match was found
			HashSet<ScopeElement> tempMyScopeElements = new HashSet<ScopeElement>(policy.getScope().keySet());
            if (tempMyScopeElements.removeAll(criteria)) {
            	answer.add(policy);
            }
		}
		return answer;
	}
	
	public PolicySet policiesContainingAllScopeElements(ScopeElement... e) {
		Set<ScopeElement> criteria = new HashSet<ScopeElement>(Arrays.asList(e));
		PolicySet answer = new PolicySet();
		for (Policy policy : this) {
            if (policy.getScope().size() < criteria.size()) {
            	continue;
            }
            
            if (policy.getScope().keySet().containsAll(criteria)) {
            	answer.add(policy);
            }
		}
		return answer;
	}
	
	public PolicySet policiesWithExactScopeElements(ScopeElement... e) {
		Set<ScopeElement> criteria = new HashSet<ScopeElement>(Arrays.asList(e));
		PolicySet answer = new PolicySet();
		for (Policy policy : this) {
            if (policy.getScope().size() != e.length) {
            	continue;
            }
            if (policy.getScope().keySet().equals(criteria)) {
            	answer.add(policy);
            }
		}
		return answer;
	}
	
	public PolicySet queryAllExclusions() {
		PolicySet answer = new PolicySet();
		for (Policy policy : this) {
			if (policy.getAction().getType() != ActionType.EXCLUDE) continue;
			answer.add(policy);
		}
		return answer;
	}
	
	public PolicySet queryAllInclusions() {
		PolicySet answer = new PolicySet();
		for (Policy policy : this) {
			if (policy.getAction().getType() != ActionType.INCLUDE) continue;
			answer.add(policy);
		}
		return answer;
	}
	
	public FilterSet<Filter> filtersForScopeElement(ScopeElement e) {
		FilterSet<Filter> answer = new FilterSet<Filter>();
		for (Policy policy : this) {
			Filter filter = policy.getScope().get(e);
			if (filter != null) {
				answer.add(filter);
			}
		}
		return answer;
	}
	
	public void pruneRedundantFilters() {
		
	}
	
	public Policy getOne() {
		return this.iterator().next();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("PolicySet [");
		for (Policy policy : this) {
			sb.append(policy.toString());
			sb.append(", ");
		}
		sb.delete(sb.length() - 2, sb.length() -1);
		sb.append(']');
		return sb.toString();
	}
	
}
