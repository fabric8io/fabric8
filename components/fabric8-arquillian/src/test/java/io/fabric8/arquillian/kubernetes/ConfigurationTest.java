/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.arquillian.kubernetes;

import io.fabric8.kubernetes.client.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static io.fabric8.arquillian.kubernetes.Constants.ANSI_LOGGER_ENABLED;
import static io.fabric8.arquillian.kubernetes.Constants.ENVIRONMENT_CONFIG_RESOURCE_NAME;
import static io.fabric8.arquillian.kubernetes.Constants.ENVIRONMENT_CONFIG_URL;
import static io.fabric8.arquillian.kubernetes.Constants.ENVIRONMENT_DEPENDENCIES;
import static io.fabric8.arquillian.kubernetes.Constants.ENVIRONMENT_INIT_ENABLED;
import static io.fabric8.arquillian.kubernetes.Constants.FABRIC8_ENVIRONMENT;
import static io.fabric8.arquillian.kubernetes.Constants.GOFABRIC8_ENABLED;
import static io.fabric8.arquillian.kubernetes.Constants.KUBERNETES_DOMAIN;
import static io.fabric8.arquillian.kubernetes.Constants.KUBERNETES_MASTER;
import static io.fabric8.arquillian.kubernetes.Constants.KUBERNETES_NAMESPACE;
import static io.fabric8.arquillian.kubernetes.Constants.NAMESPACE_CLEANUP_CONFIRM_ENABLED;
import static io.fabric8.arquillian.kubernetes.Constants.NAMESPACE_CLEANUP_ENABLED;
import static io.fabric8.arquillian.kubernetes.Constants.NAMESPACE_CLEANUP_TIMEOUT;
import static io.fabric8.arquillian.kubernetes.Constants.NAMESPACE_LAZY_CREATE_ENABLED;
import static io.fabric8.arquillian.kubernetes.Constants.NAMESPACE_TO_USE;
import static io.fabric8.arquillian.kubernetes.Constants.WAIT_FOR_SERVICE_CONNECTION_ENABLED;
import static io.fabric8.arquillian.kubernetes.Constants.WAIT_FOR_SERVICE_CONNECTION_TIMEOUT;
import static io.fabric8.arquillian.kubernetes.Constants.WAIT_FOR_SERVICE_LIST;
import static io.fabric8.arquillian.kubernetes.Constants.WAIT_POLL_INTERVAL;
import static io.fabric8.arquillian.kubernetes.Constants.WAIT_TIMEOUT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ConfigurationTest {

    @Before
    public void setUp() {
        System.getProperties().remove(KUBERNETES_MASTER);
        System.getProperties().remove(KUBERNETES_DOMAIN);
        System.getProperties().remove(KUBERNETES_NAMESPACE);

        System.getProperties().remove(FABRIC8_ENVIRONMENT);
        System.getProperties().remove(NAMESPACE_LAZY_CREATE_ENABLED);
        System.getProperties().remove(NAMESPACE_CLEANUP_TIMEOUT);
        System.getProperties().remove(NAMESPACE_CLEANUP_CONFIRM_ENABLED);
        System.getProperties().remove(NAMESPACE_CLEANUP_ENABLED);
        System.getProperties().remove(NAMESPACE_TO_USE);

        System.getProperties().remove(ENVIRONMENT_INIT_ENABLED);
        System.getProperties().remove(ENVIRONMENT_CONFIG_URL);
        System.getProperties().remove(ENVIRONMENT_CONFIG_RESOURCE_NAME);
        System.getProperties().remove(ENVIRONMENT_DEPENDENCIES);

        System.getProperties().remove(WAIT_TIMEOUT);
        System.getProperties().remove(WAIT_POLL_INTERVAL);

        System.getProperties().remove(WAIT_FOR_SERVICE_LIST);
        System.getProperties().remove(WAIT_FOR_SERVICE_CONNECTION_ENABLED);
        System.getProperties().remove(WAIT_FOR_SERVICE_CONNECTION_TIMEOUT);

        System.getProperties().remove(ANSI_LOGGER_ENABLED);
        System.getProperties().remove(GOFABRIC8_ENABLED);
    }

    @After
    public void tearDown() {
        setUp();
    }

    @Test
    public void testWithConfigMap() {
        String expctedMaster = "http://expected.master:80";
        String expectedNamespace = "expected.namespace";
        String expectedDomain = "expected.domain";
        String expectedConfigUrl = "http://expected.env.config/kubernetes.json";

        Map<String, String> map = new HashMap<>();
        map.put(KUBERNETES_MASTER, expctedMaster);
        map.put(KUBERNETES_DOMAIN, expectedDomain);
        map.put(KUBERNETES_NAMESPACE, expectedNamespace);

        map.put(NAMESPACE_LAZY_CREATE_ENABLED, "true");
        map.put(NAMESPACE_CLEANUP_TIMEOUT, "0");
        map.put(NAMESPACE_CLEANUP_CONFIRM_ENABLED, "true");
        map.put(NAMESPACE_CLEANUP_ENABLED, "true");
        map.put(NAMESPACE_TO_USE, expectedNamespace);

        map.put(ENVIRONMENT_INIT_ENABLED, "true");
        map.put(ENVIRONMENT_CONFIG_URL, expectedConfigUrl);
        map.put(ENVIRONMENT_CONFIG_RESOURCE_NAME, "");
        map.put(ENVIRONMENT_DEPENDENCIES, "");

        map.put(WAIT_TIMEOUT, "0");
        map.put(WAIT_POLL_INTERVAL, "0");

        map.put(WAIT_FOR_SERVICE_LIST, "");
        map.put(WAIT_FOR_SERVICE_CONNECTION_ENABLED, "true");
        map.put(WAIT_FOR_SERVICE_CONNECTION_TIMEOUT, "0");

        map.put(ANSI_LOGGER_ENABLED, "true");
        map.put(GOFABRIC8_ENABLED, "true");
        Configuration configuration = Configuration.fromMap(map);

        assertEquals(expctedMaster, configuration.getMasterUrl());
        assertEquals(expectedNamespace, configuration.getNamespaceToUse());
        assertEquals(expectedDomain, configuration.getKubernetesDomain());

        assertEquals(0L, configuration.getWaitForServiceConnectionTimeout());
        assertEquals(0L, configuration.getWaitTimeout());
        assertEquals(0L, configuration.getWaitPollInterval());
        assertEquals(0L, configuration.getNamespaceCleanupTimeout());

        assertTrue(configuration.isAnsiLoggerEnabled());
        assertTrue(configuration.getGofabric8Enabled());
        assertTrue(configuration.isEnvironmentInitEnabled());
        assertTrue(configuration.isNamespaceCleanupEnabled());
        assertTrue(configuration.isNamespaceLazyCreateEnabled());
        assertTrue(configuration.isWaitForServiceConnectionEnabled());
    }

    @Test
    public void testFallbackToClientsDefaults() {
        //Let's provide a fake kubeconfig for the client.
        System.setProperty(Config.KUBERNETES_KUBECONFIG_FILE, getClass().getResource("/test-kubeconfig").getFile());
        Configuration config = Configuration.fromMap(new HashMap<String, String>());
        assertNotNull(config);

        assertEquals("https://from.kube.config:8443/", config.getMasterUrl());
    }

    @Test
    public void testConfigWithSystemProperties() {
        String expctedMaster = "http://expected.master:80";
        String expectedNamespace = "expected.namespace";
        String expectedDomain = "expected.domain";
        String expectedConfigUrl = "http://expected.env.config/kubernetes.json";


        System.setProperty(KUBERNETES_MASTER, expctedMaster);
        System.setProperty(KUBERNETES_DOMAIN, expectedDomain);
        System.setProperty(KUBERNETES_NAMESPACE, expectedNamespace);

        System.setProperty(NAMESPACE_LAZY_CREATE_ENABLED, "true");
        System.setProperty(NAMESPACE_CLEANUP_TIMEOUT, "0");
        System.setProperty(NAMESPACE_CLEANUP_CONFIRM_ENABLED, "true");
        System.setProperty(NAMESPACE_CLEANUP_ENABLED, "true");
        System.setProperty(NAMESPACE_TO_USE, expectedNamespace);

        System.setProperty(ENVIRONMENT_INIT_ENABLED, "true");
        System.setProperty(ENVIRONMENT_CONFIG_URL, expectedConfigUrl);
        System.setProperty(ENVIRONMENT_CONFIG_RESOURCE_NAME, "");
        System.setProperty(ENVIRONMENT_DEPENDENCIES, "");

        System.setProperty(WAIT_TIMEOUT, "0");
        System.setProperty(WAIT_POLL_INTERVAL, "0");

        System.setProperty(WAIT_FOR_SERVICE_LIST, "");
        System.setProperty(WAIT_FOR_SERVICE_CONNECTION_ENABLED, "true");
        System.setProperty(WAIT_FOR_SERVICE_CONNECTION_TIMEOUT, "0");

        System.setProperty(ANSI_LOGGER_ENABLED, "true");
        System.setProperty(GOFABRIC8_ENABLED, "true");
        Configuration configuration = Configuration.fromMap(new HashMap<String, String>());

        assertEquals(expctedMaster, configuration.getMasterUrl());
        assertEquals(expectedNamespace, configuration.getNamespaceToUse());
        assertEquals(expectedDomain, configuration.getKubernetesDomain());

        assertEquals(0L, configuration.getWaitForServiceConnectionTimeout());
        assertEquals(0L, configuration.getWaitTimeout());
        assertEquals(0L, configuration.getWaitPollInterval());
        assertEquals(0L, configuration.getNamespaceCleanupTimeout());

        assertTrue(configuration.isAnsiLoggerEnabled());
        assertTrue(configuration.getGofabric8Enabled());
        assertTrue(configuration.isEnvironmentInitEnabled());
        assertTrue(configuration.isNamespaceCleanupEnabled());
        assertTrue(configuration.isNamespaceLazyCreateEnabled());
        assertTrue(configuration.isWaitForServiceConnectionEnabled());
    }

    @Test
    public void testConfigWithSystemPropertiesAndConfigMap() {
        String expctedMaster = "http://expected.master:80";
        String expectedNamespace = "expected.namespace";
        String expectedDomain = "expected.domain";
        String expectedConfigUrl = "http://expected.env.config/kubernetes.json";


        System.setProperty(KUBERNETES_MASTER, expctedMaster);
        System.setProperty(KUBERNETES_DOMAIN, expectedDomain);
        System.setProperty(KUBERNETES_NAMESPACE, expectedNamespace);

        System.setProperty(NAMESPACE_LAZY_CREATE_ENABLED, "true");
        System.setProperty(NAMESPACE_CLEANUP_TIMEOUT, "0");
        System.setProperty(NAMESPACE_CLEANUP_CONFIRM_ENABLED, "true");
        System.setProperty(NAMESPACE_CLEANUP_ENABLED, "true");
        System.setProperty(NAMESPACE_TO_USE, expectedNamespace);

        System.setProperty(ENVIRONMENT_INIT_ENABLED, "true");
        System.setProperty(ENVIRONMENT_CONFIG_URL, expectedConfigUrl);
        System.setProperty(ENVIRONMENT_CONFIG_RESOURCE_NAME, "");
        System.setProperty(ENVIRONMENT_DEPENDENCIES, "");

        System.setProperty(WAIT_TIMEOUT, "0");
        System.setProperty(WAIT_POLL_INTERVAL, "0");

        System.setProperty(WAIT_FOR_SERVICE_LIST, "");
        System.setProperty(WAIT_FOR_SERVICE_CONNECTION_ENABLED, "true");
        System.setProperty(WAIT_FOR_SERVICE_CONNECTION_TIMEOUT, "0");

        System.setProperty(ANSI_LOGGER_ENABLED, "true");
        System.setProperty(GOFABRIC8_ENABLED, "true");

        String overridenMaster = "http://overriden.master:80";
        String overridenNamespace = "overriden.namespace";
        String overridenDomain = "overriden.domain";

        Map<String, String> map = new HashMap<>();
        map.put(KUBERNETES_MASTER, overridenMaster);
        map.put(KUBERNETES_DOMAIN, overridenDomain);
        map.put(KUBERNETES_NAMESPACE, overridenNamespace);
        map.put(NAMESPACE_TO_USE, overridenNamespace);

        Configuration configuration = Configuration.fromMap(map);

        assertEquals(overridenMaster, configuration.getMasterUrl());
        assertEquals(overridenNamespace, configuration.getNamespaceToUse());
        assertEquals(overridenDomain, configuration.getKubernetesDomain());

        assertEquals(0L, configuration.getWaitForServiceConnectionTimeout());
        assertEquals(0L, configuration.getWaitTimeout());
        assertEquals(0L, configuration.getWaitPollInterval());
        assertEquals(0L, configuration.getNamespaceCleanupTimeout());

        assertTrue(configuration.isAnsiLoggerEnabled());
        assertTrue(configuration.getGofabric8Enabled());
        assertTrue(configuration.isEnvironmentInitEnabled());
        assertTrue(configuration.isNamespaceCleanupEnabled());
        assertTrue(configuration.isNamespaceLazyCreateEnabled());
        assertTrue(configuration.isWaitForServiceConnectionEnabled());
    }


    @Test(expected = IllegalStateException.class)
    public void testNamespaceConflict() {
        Map<String, String> map = new HashMap<>();
        map.put(NAMESPACE_TO_USE, "namesapce1");
        map.put(FABRIC8_ENVIRONMENT, "testing");
        map.put("testing.namesapce", "namespace2");
        Configuration.fromMap(map);
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingEnvironmentNamespace() {
        Map<String, String> map = new HashMap<>();
        map.put(FABRIC8_ENVIRONMENT, "testing");
        Configuration.fromMap(map);
    }
}