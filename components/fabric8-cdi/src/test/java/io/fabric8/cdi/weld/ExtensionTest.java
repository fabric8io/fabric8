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
package io.fabric8.cdi.weld;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.mockwebserver.Dispatcher;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.google.mockwebserver.RecordedRequest;
import io.fabric8.cdi.deltaspike.DeltaspikeTestBase;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.KubernetesFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.inject.New;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import java.io.File;
import java.net.URL;
import java.util.Set;

@RunWith(Arquillian.class)
public class ExtensionTest {

    private static final MockWebServer server = new MockWebServer();

    private static String KUBERNETES_SERVICE_JSON;
    private static String FABRIC8_CONSOLE_SERVICE_JSON;
    private static String APP_LIBRARY_SERVICE_JSON;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addClasses(StringToURL.class, URLToConnection.class, NestingFactoryBean.class, ServiceStringBean.class, ServiceUrlBean.class)
                .addClasses(DeltaspikeTestBase.getDeltaSpikeHolders())
                .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                .addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml")
                        .resolve(
                                "org.apache.deltaspike.core:deltaspike-core-impl",
                                "com.google.mockwebserver:mockwebserver"
                        )
                        .withTransitivity().as(File.class));
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        System.setProperty("MY_CONFIG_TEST", "value1");
        System.setProperty("MY_OTHER_CONFIG_TEST", "value2");
        System.setProperty("FABRIC8_CONSOLE_SERVICE_PROTOCOL", "https");
        System.setProperty("KUBERNETES_PROTOCOL", "https");

        KUBERNETES_SERVICE_JSON = Resources.toString(ExtensionTest.class.getResource("/mock/kubernetes-service.json"), Charsets.UTF_8);
        FABRIC8_CONSOLE_SERVICE_JSON = Resources.toString(ExtensionTest.class.getResource("/mock/kubernetes-service.json"), Charsets.UTF_8);
        APP_LIBRARY_SERVICE_JSON = Resources.toString(ExtensionTest.class.getResource("/mock/app-library-service.json"), Charsets.UTF_8);

        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (request.getPath().equals("/api/v1/namespaces/default/services/kubernetes")) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody(KUBERNETES_SERVICE_JSON);
                } else if (request.getPath().matches("/api/[^/]+/namespaces/[^/]+/services/fabric8-console-service")) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody(FABRIC8_CONSOLE_SERVICE_JSON);
                } else if (request.getPath().matches("/api/[^/]+/namespaces/[^/]+/services/app-library")) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody(APP_LIBRARY_SERVICE_JSON);
                } else if (request.getPath().matches("/oapi/[^/]+/namespaces/[^/]+/routes")) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody("{}");
                } else if (request.getPath().matches("/api/[^/]+/namespaces/[^/]+/endpoints")) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody("{}");
                } else {
                    return new MockResponse().setResponseCode(404);
                }
            }
        });

        server.play();
        System.setProperty(KubernetesFactory.KUBERNETES_MASTER_SYSTEM_PROPERTY, "http://" + server.getHostName() + ":" + server.getPort());
        System.setProperty(KubernetesFactory.KUBERNETES_TRUST_ALL_CERIFICATES, "true");
        System.setProperty(KubernetesFactory.KUBERNETES_VERIFY_SYSTEM_PROPERTY, "false");

    }

    @Inject
    private KubernetesClient kubernetesClient;

    @Inject
    @New
    private ServiceStringBean serviceLocationBean;

    @Inject
    @New
    private ServiceUrlBean serviceUrlBean;

    @Inject
    @New
    private StringToURL stringToURL;

    @Inject
    @New
    private NestingFactoryBean nestingFactoryBean;

    @Test
    public void testClientInjection() {
        Assert.assertNotNull(kubernetesClient);
    }

    @Test
    public void testServiceInjection() {
        Assert.assertNotNull(serviceLocationBean);
        Assert.assertNotNull(serviceLocationBean.getKubernetesUrl());
        Assert.assertNotNull(serviceLocationBean.getConsoleUrl());
    }

    @Test
    public void testProtocolOveride() {
        Assert.assertTrue(serviceLocationBean.getTestUrl().startsWith("tst"));
    }

    @Test
    public void testConfigInjection() {
        Assert.assertNotNull(serviceLocationBean);
        Assert.assertEquals("value1", serviceLocationBean.getConfigBean().getProperty());
        Assert.assertEquals("value2", serviceLocationBean.getOtherConfigBean().getProperty());
    }


    @Test
    public void testFactory() {
        Assert.assertNotNull(serviceUrlBean);
        Assert.assertNotNull(serviceUrlBean.getKubernetesUrl());
        Assert.assertNotNull(serviceUrlBean.getConsoleUrl());
        Assert.assertTrue(serviceUrlBean.getConsoleUrl().toString().startsWith("https"));
        Assert.assertTrue(serviceUrlBean.getKubernetesUrl().toString().startsWith("https"));
    }

    @Test
    public void testAlias() {
        Set<Bean<?>> beans = CDI.current().getBeanManager().getBeans("cool-id");
        Assert.assertNotNull(beans);
        Assert.assertEquals(1, beans.size());
        Assert.assertEquals(URL.class, beans.iterator().next().getBeanClass());
    }


    @Test
    public void testNestingFactories() {
        Assert.assertNotNull(nestingFactoryBean);
        Assert.assertNotNull(nestingFactoryBean.getConsoleConnection());
        Assert.assertNotNull(nestingFactoryBean.getAppLibraryConnection());
    }
}
