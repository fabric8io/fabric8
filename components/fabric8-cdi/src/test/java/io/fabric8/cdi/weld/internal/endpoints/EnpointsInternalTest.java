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
        createInstance(ServiceListWithoutEndpoint.class);
    }


    @Test
    public void testServiceInstanceWithEndpoint() {
        createInstance(ServiceInstanceWithEndpoint.class);
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
