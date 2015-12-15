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
package io.fabric8.cdi.weld.internal.endpoints;

import io.fabric8.cdi.Fabric8Extension;
import io.fabric8.cdi.weld.ClientProducer;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import java.util.ArrayList;
import java.util.List;

public class EnpointsInternalTest {

    private WeldContainer weld;

    @After
    public void cleanUp() {
        if (weld != null) {
            weld.shutdown();
        }
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testServiceListWithoutEndpoint() {
        ServiceListWithoutEndpoint obj = createInstance(ServiceListWithoutEndpoint.class);
        Assert.assertEquals(2, obj.getService().size());
        Assert.assertTrue(obj.getService().contains("tcp://10.0.0.1:8080"));
        Assert.assertTrue(obj.getService().contains("tcp://10.0.0.2:8080"));
    }

    @Test
    public void testServiceListInstanceWithEndpoint() {
        ServiceListInstanceWithEndpoint obj = createInstance(ServiceListInstanceWithEndpoint.class);
        Assert.assertEquals(2, obj.getService().get().size());
        Assert.assertTrue(obj.getService().get().contains("tcp://10.0.0.1:8080"));
        Assert.assertTrue(obj.getService().get().contains("tcp://10.0.0.2:8080"));
    }



    @Test
    public void testServiceInstanceWithEndpoint() {
        ServiceInstanceWithEndpoint obj = createInstance(ServiceInstanceWithEndpoint.class);
        Assert.assertTrue(obj.getService().equals("tcp://10.0.0.1:8080") || obj.getService().contains("tcp://10.0.0.2:8080"));
    }

    @Test
    public void testServiceInstanceWithMultiPortEndpoint() {
        ServiceInstanceWithMultiPortEndpoint obj = createInstance(ServiceInstanceWithMultiPortEndpoint.class);
        Assert.assertEquals("http://172.30.17.2:8082", obj.getService());
    }

    @Test
    public void testServiceInstanceWithFactoryAndMultipleEndpoints() {
        ServiceInstanceUsingFactoryAndEndpoints obj = createInstance(ServiceInstanceUsingFactoryAndEndpoints.class);
        Assert.assertNotNull(obj.getService());
    }

    @Test
    public void testChangingEndpoints() {
        ServiceListInstanceWithEndpoint2 obj = createInstance(ServiceListInstanceWithEndpoint2.class);
        List<String> endpoints = new ArrayList<>(obj.getService().get());

        Assert.assertTrue(endpoints.contains("tcp://10.0.0.1:8080"));
        Assert.assertTrue(endpoints.contains("tcp://10.0.0.2:8080"));

        endpoints = new ArrayList<>(obj.getService().get());

        Assert.assertTrue(endpoints.contains("tcp://10.0.0.1:8080"));
        Assert.assertFalse(endpoints.contains("tcp://10.0.0.2:8080"));
    }


    <T> T createInstance(Class<T> type) {
        weld = new Weld()
                .disableDiscovery()
                .extensions(new Fabric8Extension())
                .beanClasses(ClientProducer.class, RandomEndpointToUrl.class, type)
                .alternatives(ClientProducer.class)
                .initialize();

        CreationalContext ctx = weld.getBeanManager().createCreationalContext(null);
        for (Bean bean : weld.getBeanManager().getBeans(type)) {
            return (T) weld.getBeanManager().getReference(bean, type, ctx);
        }
        return null;
    }

}
