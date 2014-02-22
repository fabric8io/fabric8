package io.fabric8.zookeeper.bootstrap;

import org.junit.Test;
import static io.fabric8.zookeeper.bootstrap.RuntimePropertiesService.toEnvVariable;
import static io.fabric8.zookeeper.bootstrap.RuntimePropertiesService.DEFAULT_ENV_PREFIX;
import static org.junit.Assert.assertEquals;

public class RuntimePropertiesServiceTest {


    @Test
    public void testSysToEnv() {
        assertEquals("FABRIC8_ZOOKEEPER_URL", toEnvVariable(DEFAULT_ENV_PREFIX, "zookeeper.url"));
        assertEquals("FABRIC8_ZOOKEEPER_PASSWORD", toEnvVariable(DEFAULT_ENV_PREFIX, "zookeeper.password"));
        assertEquals("FABRIC8_MY_ZOOKEEPER_URL", toEnvVariable(DEFAULT_ENV_PREFIX, "my-zookeeper.url"));
    }

}
