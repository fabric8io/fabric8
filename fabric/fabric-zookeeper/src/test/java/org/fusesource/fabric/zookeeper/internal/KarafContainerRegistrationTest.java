package org.fusesource.fabric.zookeeper.internal;

import org.junit.Test;
import static org.junit.Assert.*;

public class KarafContainerRegistrationTest {

    @Test
    public void testReplaceJmxHost() {
        //Jmx URL with 0.0.0.0
        String jmxUrl = "service:jmx:rmi://0.0.0.0:44444/jndi/rmi://0.0.0.0:1099/karaf-root";
        String expectedUrl = "service:jmx:rmi://HOST:44444/jndi/rmi://HOST:1099/karaf-root";
        assertEquals(expectedUrl, KarafContainerRegistration.replaceJmxHost(jmxUrl, "HOST"));

        //With local ip
        jmxUrl = "service:jmx:rmi://127.0.0.1:44444/jndi/rmi://127.0.0.1:1099/karaf-root";
        assertEquals(expectedUrl, KarafContainerRegistration.replaceJmxHost(jmxUrl, "HOST"));

        jmxUrl = "service:jmx:rmi://10.0.0.1:44444/jndi/rmi://10.0.0.1:1099/karaf-root";
        assertEquals(expectedUrl, KarafContainerRegistration.replaceJmxHost(jmxUrl, "HOST"));

        //With local hostname
        jmxUrl = "service:jmx:rmi://localhost:44444/jndi/rmi://localhost:1099/karaf-root";
        assertEquals(expectedUrl, KarafContainerRegistration.replaceJmxHost(jmxUrl, "HOST"));

        //With public domainname
        jmxUrl = "service:jmx:rmi://www.somedomain.com:44444/jndi/rmi://www.somedomain.com:1099/karaf-root";
        assertEquals(expectedUrl, KarafContainerRegistration.replaceJmxHost(jmxUrl, "HOST"));

        //With public domainname
        jmxUrl = "service:jmx:rmi://team-1.somedomain.com:44444/jndi/rmi://team-1.somedomain.com:1099/karaf-root";
        assertEquals(expectedUrl, KarafContainerRegistration.replaceJmxHost(jmxUrl, "HOST"));


    }
}
