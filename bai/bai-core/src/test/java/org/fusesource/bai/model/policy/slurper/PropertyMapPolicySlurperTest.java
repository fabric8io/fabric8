package org.fusesource.bai.model.policy.slurper;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.fusesource.bai.model.policy.Policy;
import org.junit.Test;

public class PropertyMapPolicySlurperTest {

	@Test
	public void testLoadFile() throws IOException {
		PropertyMapPolicySlurper slurper = new PropertyMapPolicySlurper();
		Properties properties = new Properties();
		properties.load(this.getClass().getClassLoader().getResourceAsStream("audit-policy-test.cfg"));
		slurper.setProperties(properties);
		
		List<Policy> policies = slurper.slurp();
		return;
		
	}

}
