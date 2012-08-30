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

package org.fusesource.bai.model.policy.slurper;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Properties;

import org.fusesource.bai.policy.model.Constants;
import org.fusesource.bai.policy.model.Constants.ScopeElement;
import org.fusesource.bai.policy.model.EnumerationFilter;
import org.fusesource.bai.policy.model.ExpressionFilter;
import org.fusesource.bai.policy.model.FilterSet;
import org.fusesource.bai.policy.model.PolicySet;
import org.fusesource.bai.policy.slurper.PropertyMapPolicySlurper;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

/**
 * Unit Test for PropertyMapPolicySlurper
 * @author Raul Kripalani
 *
 */
public class PropertyMapPolicySlurperTest {

	PolicySet policySet;
	
	@Before
	public  void loadFile() throws IOException {
		PropertyMapPolicySlurper slurper = new PropertyMapPolicySlurper();
		Properties properties = new Properties();
		properties.load(this.getClass().getClassLoader().getResourceAsStream("audit-policy-test.cfg"));
		slurper.setProperties(properties);
		policySet = slurper.slurp();
	}
	
	@Test
	public void testGlobalContextPolicies() throws IOException {
        assertEquals("Two context policies were expected", 2, 
        		policySet.policiesWithExactScopeElements(ScopeElement.BUNDLE, ScopeElement.CONTEXT).size());
        assertEquals("One excluding context policies was expected", 1, 
        		policySet.policiesWithExactScopeElements(ScopeElement.BUNDLE, ScopeElement.CONTEXT).queryAllExclusions().size());
		
	}
	
	@Test
	public void testBundlePolicies() throws IOException {
        assertEquals("Six bundle policies were expected", 6, policySet.policiesContainingAnyScopeElements(Constants.ScopeElement.BUNDLE).size());
		
	}
	
	@Test
	public void testFilterNavigation() throws IOException {
        assertEquals("Policies that apply to event 'CREATED' should be 2", 2, 
        		policySet.filtersForScopeElement(ScopeElement.EVENT).filtersOfType(EnumerationFilter.class)
        		.allMatchesFor("CREATED").size());
        assertEquals("Policies that apply to event 'CREATED' should be 2", 2, 
        		policySet.filtersForScopeElement(ScopeElement.EVENT).filtersOfType(EnumerationFilter.class).countMatches("CREATED"));

        FilterSet<ExpressionFilter> filterSet = policySet.filtersForScopeElement(ScopeElement.CONTEXT).filtersOfType(ExpressionFilter.class);
        assertEquals("Policies for camelContext1 should be 1", 1, Sets.filter(filterSet, new Predicate<ExpressionFilter>() {
			@Override
			public boolean apply(ExpressionFilter ef) {
				return ef.getExpression().contains("camelContext1"); 
			}
		}).size());
	}

}
