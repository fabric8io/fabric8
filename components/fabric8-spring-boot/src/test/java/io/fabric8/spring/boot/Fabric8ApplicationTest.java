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

import io.fabric8.annotations.Protocol;
import io.fabric8.annotations.ServiceName;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {ClientFactory.class, Fabric8Application.class})
public class Fabric8ApplicationTest {

    private static final String KUBERNETES_NAMESPACE = "KUBERNETES_NAMESPACE";
    private static final String DEFAULT_NAMESPACE = "default";

    @BeforeClass
    public static void setUpClass() throws Exception {
        System.setProperty("MY_CONFIG_TEST", "value1");
        System.setProperty("MY_OTHER_CONFIG_TEST", "value2");
        System.setProperty("FABRIC8_CONSOLE_SERVICE_PROTOCOL", "https");
        System.setProperty("KUBERNETES_PROTOCOL", "https");

        System.setProperty(KUBERNETES_NAMESPACE, DEFAULT_NAMESPACE);
        System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
    }


    @Autowired
    private KubernetesClient kubernetes;

    @Autowired
    @ServiceName("fabric8-console-service")
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
