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
package io.fabric8.cdi.weld.nonamespace;

import io.fabric8.cdi.Fabric8Extension;
import io.fabric8.cdi.weld.ClientProducer;
import org.hamcrest.CoreMatchers;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.matchers.ThrowableMessageMatcher;
import org.junit.rules.ExpectedException;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

public class NoNamespaceTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testServiceInjection() {
        expectedException.expect(ThrowableMessageMatcher.hasMessage(CoreMatchers.equalTo("No kubernetes service could be found for name: service1 in namespace: null")));
        createInstance(MyBean.class);
    }


    void createInstance(Class type) {
        WeldContainer weld = new Weld()
                .disableDiscovery()
                .extensions(new Fabric8Extension())
                .beanClasses(ClientProducer.class, MyBean.class)
                .alternatives(ClientProducer.class)
                .initialize();
        CreationalContext ctx = weld.getBeanManager().createCreationalContext(null);
        for (Bean bean : weld.getBeanManager().getBeans(type)) {
            weld.getBeanManager().getReference(bean, type, ctx);
        }
    }

}
