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
package io.fabric8.spring.boot;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.mockwebserver.Dispatcher;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.google.mockwebserver.RecordedRequest;
import io.fabric8.annotations.Protocol;
import io.fabric8.annotations.ServiceName;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Fabric8Application.class)
public class Fabric8ApplicationTest {


    private static final MockWebServer server = new MockWebServer();
    private static final String KUBERNETES_NAMESPACE = "KUBERNETES_NAMESPACE";
    private static final String DEFAULT_NAMESPACE = "default";


    private static String SERVICES_JSON;
    private static String FABRIC8_CONSOLE_SERVICE_JSON;
    private static String KUBERNETES_SERVICE_JSON;
    private static String APP_LIBRARY_SERVICE_JSON;


    @BeforeClass
    public static void setUpClass() throws Exception {
        System.setProperty("MY_CONFIG_TEST", "value1");
        System.setProperty("MY_OTHER_CONFIG_TEST", "value2");
        System.setProperty("FABRIC8_CONSOLE_SERVICE_PROTOCOL", "https");
        System.setProperty("KUBERNETES_PROTOCOL", "https");

        FABRIC8_CONSOLE_SERVICE_JSON = Resources.toString(Fabric8ApplicationTest.class.getResource("/mock/fabric8-console-service.json"), Charsets.UTF_8);
        KUBERNETES_SERVICE_JSON = Resources.toString(Fabric8ApplicationTest.class.getResource("/mock/kubernetes-service.json"), Charsets.UTF_8);
        APP_LIBRARY_SERVICE_JSON = Resources.toString(Fabric8ApplicationTest.class.getResource("/mock/app-library-service.json"), Charsets.UTF_8);
        SERVICES_JSON = Resources.toString(Fabric8ApplicationTest.class.getResource("/mock/services.json"), Charsets.UTF_8);

        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (request.getPath().matches("/api/[^/]/services[/]?") || request.getPath().matches("/api/[^/]/namespaces/default/services[/]?")) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody(SERVICES_JSON);
                } else if (request.getPath().matches("/api/[^/]+/namespaces/[^/]+/services/kubernetes")) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody(KUBERNETES_SERVICE_JSON);
                } else if (request.getPath().matches("/api/[^/]+/namespaces/[^/]+/services/app-library")) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody(APP_LIBRARY_SERVICE_JSON);
                } else if (request.getPath().matches("/api/[^/]+/namespaces/[^/]+/services/fabric8")) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody(FABRIC8_CONSOLE_SERVICE_JSON);
                } else if (request.getPath().matches("/oapi/[^/]+/namespaces/[^/]+/routes[/]?")) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody("{}");
                } else if (request.getPath().matches("/api/[^/]+/namespaces/[^/]+/endpoints[/]?")) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody("{}");
                } else {
                    return new MockResponse().setResponseCode(401);
                }
            }
        });

        server.play();
        System.setProperty(DefaultKubernetesClient.KUBERNETES_MASTER_SYSTEM_PROPERTY, "http://" + server.getHostName() + ":" + server.getPort());
        System.setProperty(KUBERNETES_NAMESPACE, DEFAULT_NAMESPACE);
        System.setProperty(DefaultKubernetesClient.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
    }


    @Autowired
    private KubernetesClient kubernetes;

    @Autowired
    @ServiceName("fabric8")
    private Service consoleService;

    @Autowired
    @ServiceName("app-library")
    private String appLibraryService;


   @Autowired
   @ServiceName("kubernetes")
   @Protocol("http")
    private String kubernetesService;

    @Test
    public void testSpringBoot() {
        //Assert client is injected
        Assert.assertNotNull(kubernetes);

        //Assert injection as service
        Assert.assertNotNull(consoleService);

        //Assert injection as string
        Assert.assertNotNull(appLibraryService);

        //Assert injection as string with protocol
        Assert.assertNotNull(kubernetesService);
    }
}
