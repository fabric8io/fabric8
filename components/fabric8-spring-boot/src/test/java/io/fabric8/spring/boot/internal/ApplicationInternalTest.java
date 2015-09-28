/*
 * Copyright 2005-2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.spring.boot.internal;

import io.fabric8.annotations.Protocol;
import io.fabric8.annotations.ServiceName;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.spring.boot.Fabric8Application;
import io.fabric8.spring.boot.URLToConnection;
import io.fabric8.spring.boot.external.ClientFactory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.URLConnection;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {URLToConnection.class, Fabric8Application.class})
public class ApplicationInternalTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        System.setProperty("SERVICE1_PROTOCOL", "https");
        System.setProperty("SERVICE2_PROTOCOL", "https");
        System.setProperty("SERVICE3_PROTOCOL", "https");

        System.setProperty(KubernetesHelper.KUBERNETES_NAMESPACE_SYSTEM_PROPERTY, KubernetesHelper.DEFAULT_NAMESPACE);
        System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
    }

    @Autowired
    private KubernetesClient client;

    @Autowired
    @ServiceName("service1")
    private URLConnection service1;

    @Autowired
    @ServiceName("service2")
    private String service2;


    @Autowired
    @ServiceName("service3")
    @Protocol("http")
    private String service3;

    @Test
    public void testSpringBoot() {
        //Assert client is injected
        Assert.assertNotNull(client);

        //Assert injection as service
        Assert.assertNotNull(service1);

        //Assert injection as string
        Assert.assertNotNull(service2);

        //Assert injection as string with protocol
        Assert.assertNotNull(service3);
    }
}
