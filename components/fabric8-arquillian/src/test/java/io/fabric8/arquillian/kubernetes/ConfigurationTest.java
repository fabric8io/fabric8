/**
 *  Copyright 2005-2016 Red Hat, Inc.
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

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static io.fabric8.arquillian.kubernetes.Constants.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ConfigurationTest {

    @Rule
    public OpenShiftServer server = new OpenShiftServer();

    protected KubernetesClient kubernetesClient;

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
        Configuration configuration = Configuration.fromMap(map, getKubernetesClient());

        assertEquals(expctedMaster, configuration.getMasterUrl());
        assertEquals(expectedNamespace, configuration.getNamespace());
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
        String file = getClass().getResource("/test-kubeconfig").getFile();
        System.setProperty(Config.KUBERNETES_KUBECONFIG_FILE, file);
        Configuration.resetFallbackConfig();

        Configuration config = Configuration.fromMap(new HashMap<String, String>(), getKubernetesClient());
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
        Configuration configuration = Configuration.fromMap(new HashMap<String, String>(), getKubernetesClient());

        assertEquals(expctedMaster, configuration.getMasterUrl());
        assertEquals(expectedNamespace, configuration.getNamespace());
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

        Configuration configuration = Configuration.fromMap(map, getKubernetesClient());

        assertEquals(overridenMaster, configuration.getMasterUrl());
        assertEquals(overridenNamespace, configuration.getNamespace());
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

    @Test
    public void testNamespaceFoundFromConfigMap() {
        String devNamespace = "myproject";
        String environmentKey = "testing";
        String testNamespace = "myproject-testing";


        Map<String, String> data = new HashMap<>();
        data.put(environmentKey, "    name: Testing\n" +
                "    namespace: " + testNamespace + "\n" +
                "    order: 0");
        server.expect().withPath("/api/v1/namespaces/" + devNamespace + "/configmaps/fabric8-environments").andReturn(200, new ConfigMapBuilder().withNewMetadata().withName("fabric8-environments").endMetadata().withData(data).build()).once();


        Map<String, String> map = new HashMap<>();
        map.put(FABRIC8_ENVIRONMENT, environmentKey);
        map.put(DEVELOPMENT_NAMESPACE, devNamespace);

        Configuration configuration = Configuration.fromMap(map, getKubernetesClient());

        assertEquals(testNamespace, configuration.getNamespace());
        
        assertTrue(configuration.isAnsiLoggerEnabled());
        assertTrue(configuration.isEnvironmentInitEnabled());
        assertTrue(configuration.isNamespaceLazyCreateEnabled());
        assertFalse(configuration.isNamespaceCleanupEnabled());
        assertFalse(configuration.isCreateNamespaceForTest());
    }

    @Ignore
    public void testEnvironmentKeyButNoConfigMapLocalOnly() {
        String devNamespace = "myproject";
        String environmentKey = "testing";
        String testNamespace = devNamespace;


        Map<String, String> data = new HashMap<>();
        data.put("staging", "    name: Staging\n" +
                "    namespace: myproject-staging\n" +
                "    order: 0");
        server.expect().withPath("/api/v1/namespaces/" + devNamespace + "/configmaps/fabric8-environments").andReturn(404, "Not found").once();


        Map<String, String> map = new HashMap<>();
        map.put(FABRIC8_ENVIRONMENT, environmentKey);

        KubernetesClient kubernetesClient = getKubernetesClient();
        Config config = new Config();
        config.setNamespace(devNamespace);
        config.setMasterUrl(kubernetesClient.getMasterUrl().toString());
        DefaultKubernetesClient clientWithDefaultNamespace = new DefaultKubernetesClient(config);
        Configuration configuration = Configuration.fromMap(map, clientWithDefaultNamespace);

        assertEquals(testNamespace, configuration.getNamespace());

        assertTrue(configuration.isAnsiLoggerEnabled());
        assertTrue(configuration.isEnvironmentInitEnabled());
        assertTrue(configuration.isNamespaceLazyCreateEnabled());
        assertFalse(configuration.isNamespaceCleanupEnabled());
        assertFalse(configuration.isCreateNamespaceForTest());
    }

    @Test
    public void testEnvironmentKeyButNoConfigMap() {
        String devNamespace = "myproject";
        String environmentKey = "testing";
        String testNamespace = devNamespace;


        Map<String, String> data = new HashMap<>();
        data.put("staging", "    name: Staging\n" +
                "    namespace: myproject-staging\n" +
                "    order: 0");
        server.expect().withPath("/api/v1/namespaces/" + devNamespace + "/configmaps/fabric8-environments").andReturn(404, "Not found").once();


        Map<String, String> map = new HashMap<>();
        map.put(FABRIC8_ENVIRONMENT, environmentKey);
        map.put(DEVELOPMENT_NAMESPACE, devNamespace);

        KubernetesClient kubernetesClient = getKubernetesClient();
        Config config = new Config();
        config.setNamespace(devNamespace);
        config.setMasterUrl(kubernetesClient.getMasterUrl().toString());
        DefaultKubernetesClient clientWithDefaultNamespace = new DefaultKubernetesClient(config);
        Configuration configuration = Configuration.fromMap(map, clientWithDefaultNamespace);

        assertEquals(testNamespace, configuration.getNamespace());

        assertTrue(configuration.isAnsiLoggerEnabled());
        assertTrue(configuration.isEnvironmentInitEnabled());
        assertTrue(configuration.isNamespaceLazyCreateEnabled());
        assertFalse(configuration.isNamespaceCleanupEnabled());
        assertFalse(configuration.isCreateNamespaceForTest());
    }

    @Ignore
    public void testNamespaceNotFoundFromConfigMap() {
        String devNamespace = "myproject";
        String environmentKey = "testing";
        String testNamespace = devNamespace;


        Map<String, String> data = new HashMap<>();
        data.put("staging", "    name: Staging\n" +
                "    namespace: myproject-staging\n" +
                "    order: 0");
        server.expect().withPath("/api/v1/namespaces/" + devNamespace + "/configmaps/fabric8-environments").andReturn(200, new ConfigMapBuilder().withNewMetadata().withName("fabric8-environments").endMetadata().withData(data).build()).once();


        Map<String, String> map = new HashMap<>();
        map.put(FABRIC8_ENVIRONMENT, environmentKey);
        map.put(DEVELOPMENT_NAMESPACE, devNamespace);

        Configuration configuration = Configuration.fromMap(map, kubernetesClient);

        assertEquals(testNamespace, configuration.getNamespace());

        assertTrue(configuration.isAnsiLoggerEnabled());
        assertTrue(configuration.isEnvironmentInitEnabled());
        assertTrue(configuration.isNamespaceLazyCreateEnabled());
        assertFalse(configuration.isNamespaceCleanupEnabled());
        assertFalse(configuration.isCreateNamespaceForTest());
    }

    @Test(expected = IllegalStateException.class)
    public void testFailIfEnvironmentNamespaceNotFoundFromConfigMap() {
        String devNamespace = "myproject";
        String environmentKey = "testing";

        Map<String, String> data = new HashMap<>();
        data.put("staging", "    name: Staging\n" +
                "    namespace: myproject-staging\n" +
                "    order: 0");
        server.expect().withPath("/api/v1/namespaces/" + devNamespace + "/configmaps/fabric8-environments").andReturn(200, new ConfigMapBuilder().withNewMetadata().withName("fabric8-environments").endMetadata().withData(data).build()).once();


        Map<String, String> map = new HashMap<>();
        map.put(FABRIC8_ENVIRONMENT, environmentKey);
        map.put(DEVELOPMENT_NAMESPACE, devNamespace);
        map.put(FAIL_ON_MISSING_ENVIRONMENT_NAMESPACE, "true");

        Configuration.fromMap(map, getKubernetesClient());
    }

    @Test(expected = IllegalStateException.class)
    public void testNamespaceConflict() {
        Map<String, String> map = new HashMap<>();
        map.put(NAMESPACE_TO_USE, "namesapce1");
        map.put(FABRIC8_ENVIRONMENT, "testing");
        map.put("testing.namespace", "namespace2");
        Configuration.fromMap(map, getKubernetesClient());
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingEnvironmentNamespace() {
        Map<String, String> map = new HashMap<>();
        map.put(FABRIC8_ENVIRONMENT, "testing");
        map.put(FAIL_ON_MISSING_ENVIRONMENT_NAMESPACE, "true");
        Configuration.fromMap(map, getKubernetesClient());
    }

    public KubernetesClient getKubernetesClient() {
        if (kubernetesClient == null) {
            kubernetesClient = server.getKubernetesClient();
        }
        assertNotNull("No KubernetesClient was created by the mock!", kubernetesClient);
        return kubernetesClient;
    }

    public void setKubernetesClient(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }
}