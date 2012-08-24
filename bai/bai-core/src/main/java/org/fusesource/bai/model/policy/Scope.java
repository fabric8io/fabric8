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

import java.util.ArrayList;
import java.util.List;

import org.fusesource.bai.model.policy.Constants.FilterElement;
import org.fusesource.bai.model.policy.Constants.FilterMethod;

/**
 * Defines the filters for a particular policy, i.e. the Scope of action of a policy.
 * @author Raul Kripalani
 *
 */
public class Scope {
	
	/**
	 * This scope is defined by a filtering element: context, bundle, event, exchange.
	 */
	public FilterElement filterElement;
	
	/**
	 * Defines what method we used to perform the filter. Apply an expression? Enum value
	 */
	public FilterMethod filterMethod;
	
	//------ If the Filter Method is 'EXPRESSION', the expression is defined by these elements
	public String expressionLanguage;
	public String expression;
	
	//------ If the Filter Method is 'ENUM_VALUE_ONE' or 'ENUM_VALUE_MULTIPLE', the enumeration values are expressed here in their valueOf() form.
	//------ It's the responsibility of the consuming logic to determine against what enumeration these values need to match
	public List<String> enumValues = new ArrayList<String>();
	
}
