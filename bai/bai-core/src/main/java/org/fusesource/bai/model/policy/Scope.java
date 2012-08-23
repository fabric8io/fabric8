package org.fusesource.bai.model.policy;

import java.util.ArrayList;
import java.util.List;

import org.fusesource.bai.model.policy.Constants.FilterElement;
import org.fusesource.bai.model.policy.Constants.FilterMethod;

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
