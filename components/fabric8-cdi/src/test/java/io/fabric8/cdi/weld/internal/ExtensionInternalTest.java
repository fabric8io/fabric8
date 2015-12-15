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
package io.fabric8.cdi.weld.internal;

import io.fabric8.cdi.deltaspike.DeltaspikeTestBase;
import io.fabric8.cdi.weld.ClientProducer;
import io.fabric8.cdi.weld.NestingFactoryBean;
import io.fabric8.cdi.weld.SimpleBean;
import io.fabric8.cdi.weld.StringToURL;
import io.fabric8.cdi.weld.URLToConnection;
import io.fabric8.cdi.weld.UrlBean;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
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
public class ExtensionInternalTest {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addClasses(StringToURL.class, URLToConnection.class, NestingFactoryBean.class, SimpleBean.class, UrlBean.class, ClientProducer.class)
                .addClasses(DeltaspikeTestBase.getDeltaSpikeHolders())
                .addAsWebInfResource("META-INF/beans.xml")
                .addAsLibraries(Maven.resolver().loadPomFromFile("pom.xml")
                        .resolve(
                                "org.apache.deltaspike.core:deltaspike-core-impl"
                        )
                        .withTransitivity().as(File.class));
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        System.setProperty("CONFIG1_TEST", "value1");
        System.setProperty("CONFIG2_TEST", "value2");
        System.setProperty("SERVICE1_SOURCE_PROTOCOL", "http");
        System.setProperty("SERVICE1_TARGET_PROTOCOL", "https");
        System.setProperty(KubernetesHelper.KUBERNETES_NAMESPACE_SYSTEM_PROPERTY, KubernetesHelper.DEFAULT_NAMESPACE);
    }

    @Inject
    private KubernetesClient client;

    @Inject
    @New
    private SimpleBean simpleBean;

    @Inject
    @New
    private UrlBean urlBean;

    @Inject
    @New
    private StringToURL stringToURL;

    @Inject
    @New
    private NestingFactoryBean nestingFactoryBean;

    @Test
    public void testClientInjection() {
        Assert.assertNotNull(client);
    }

    @Test
    public void testServiceInjection() {
        Assert.assertNotNull(simpleBean);
        Assert.assertNotNull(simpleBean.getOptionalUrl());
        Assert.assertNotNull(simpleBean.getUrl());
    }

    @Test
    public void testProtocolOveride() {
        Assert.assertTrue(simpleBean.getTestUrl().startsWith("tst"));
    }

    @Test
    public void testConfigInjection() {
        Assert.assertNotNull(simpleBean);
        Assert.assertEquals("value1", simpleBean.getConfig1().getProperty());
        Assert.assertEquals("value2", simpleBean.getConfig2().getProperty());
    }


    @Test
    public void testFactory() {
        Assert.assertNotNull(urlBean);
        Assert.assertNotNull(urlBean.getService3());
        Assert.assertNotNull(urlBean.getService1());
        Assert.assertTrue(urlBean.getService1().toString().startsWith("http"));
        Assert.assertTrue(urlBean.getService3().toString().startsWith("http"));
    }

    @Test
    public void testAlias() {
        Set<Bean<?>> beans = CDI.current().getBeanManager().getBeans("cool-id");
        Assert.assertNotNull(beans);
        Assert.assertEquals(1, beans.size());
        Assert.assertEquals(URL.class, beans.iterator().next().getBeanClass());
    }

    @Test
    public void testMultiport() {
        Assert.assertNotNull(simpleBean);
        Assert.assertTrue(simpleBean.getMultiportDefault().endsWith("8081"));
        Assert.assertTrue(simpleBean.getMultiport2().endsWith("8082"));
    }

    @Test
    public void testNestingFactories() {
        Assert.assertNotNull(nestingFactoryBean);
        Assert.assertNotNull(nestingFactoryBean.getService1());
        Assert.assertNotNull(nestingFactoryBean.getService2());
    }

    @Test
    public void testProtocol() {
        Assert.assertNotNull(simpleBean);
        Assert.assertTrue(simpleBean.getUrl().startsWith("tcp://"));
        Assert.assertTrue(simpleBean.getTestUrl().startsWith("tst://"));
    }
}
