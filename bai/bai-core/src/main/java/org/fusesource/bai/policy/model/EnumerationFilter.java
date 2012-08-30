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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.fusesource.bai.policy.model.Constants.ScopeElement;

/**
 * A filter whose filtering criteria is an Enumeration.
 * @author Raul Kripalani
 *
 */
public class EnumerationFilter extends Filter {

	private Set<String> enumValues = new HashSet<String>();

	public EnumerationFilter(ScopeElement e, Policy p) {
		super(e, p);
	}
	
	public EnumerationFilter(ScopeElement e, Policy p, String... enumValues) {
		super(e, p);
		this.getEnumValues().addAll(Arrays.asList(enumValues));
	}
	
	public EnumerationFilter enumValues(String... v) {
		enumValues.addAll(Arrays.asList(v));
		return this;
	}
	
	public Set<String> getEnumValues() {
		if (enumValues == null) {
			enumValues = new HashSet<String>();
		}
		return enumValues;
	}
	
	public String getOneEnumValue() {
		return getEnumValues().size() == 0 ? null : enumValues.iterator().next();
	}
	
	public void setEnumValues(Set<String> enumValues) {
		this.enumValues = enumValues;
	}
	
	@SuppressWarnings("rawtypes")
	public boolean matches(Object o) {
		if (o instanceof String) {
			return enumValues.contains((String) o);
		}
		// If we get a collection, all items must exist
		if (o instanceof Collection) {
			Collection col = (Collection) o;
			for (Object item : col) {
				if (!enumValues.contains(item.toString())) {
					return false;
				}
			}
			return true;
		}
		// Use the toString representation
		return enumValues.contains(o.toString());

	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("EnumerationFilter [enumValues=");
		builder.append(enumValues);
		builder.append(", element=");
		builder.append(element);
		builder.append("]");
		return builder.toString();
	}
	
}
