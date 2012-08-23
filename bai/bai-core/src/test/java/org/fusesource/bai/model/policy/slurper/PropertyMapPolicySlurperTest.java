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

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.fusesource.bai.model.policy.Policy;
import org.fusesource.bai.model.policy.PolicySet;
import org.junit.Test;

/**
 * Unit Test for PropertyMapPolicySlurper
 * @author Raul Kripalani
 *
 */
public class PropertyMapPolicySlurperTest {

	@Test
	public void testLoadFile() throws IOException {
		PropertyMapPolicySlurper slurper = new PropertyMapPolicySlurper();
		Properties properties = new Properties();
		properties.load(this.getClass().getClassLoader().getResourceAsStream("audit-policy-test.cfg"));
		slurper.setProperties(properties);
		
		PolicySet policies = slurper.slurp();
		return;
		
	}

}
