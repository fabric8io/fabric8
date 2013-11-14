package org.fusesource.fabric.agent;

import java.io.IOException;
import java.util.Properties;
import java.util.Set;

import org.junit.Test;

import static org.fusesource.fabric.agent.DeploymentAgent.filterOverrides;
import static org.fusesource.fabric.agent.DeploymentAgent.getPrefixedProperties;
import static org.fusesource.fabric.utils.DataStoreUtils.toMap;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for {@link DeploymentAgent}
 */
public class DeploymentAgentTest {

    @Test
    public void testFilterOverrides() throws IOException {
        Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream("overrides.properties"));

        Set<String> filtered = filterOverrides(getPrefixedProperties(toMap(properties), "override."));

        assertTrue("fabric-agent 7.1.0.fuse-121 should not have been filtered",
                   filtered.contains("mvn:org.fusesource.fabric/fabric-agent/7.1.0.fuse-121"));
        assertFalse("fabric-agent 7.1.0.fuse-112 should have been filtered",
                    filtered.contains("mvn:org.fusesource.fabric/fabric-agent/7.1.0.fuse-112"));

        assertTrue("URL that causes parser errors should not have been filtered",
                   filtered.contains("mvn:bogus/url"));
    }

}
