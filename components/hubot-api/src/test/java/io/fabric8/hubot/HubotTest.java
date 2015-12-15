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
package io.fabric8.hubot;

import io.fabric8.cdi.deltaspike.DeltaspikeTestBase;
import org.apache.deltaspike.core.impl.config.DefaultConfigPropertyProducer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;

import javax.inject.Inject;

@Ignore
@RunWith(Arquillian.class)
public class HubotTest {

    @Deployment
    public static WebArchive createDeployment() {
        return DeltaspikeTestBase.createDeployment()
                .addClasses(DeltaspikeTestBase.getDeltaSpikeHolders())
                .addClasses(HubotRestApi.class, HubotNotifier.class, ClientProducer.class,
                        //We need that so that deltaspike can see our @ConfigProperty annotated classes
                        DefaultConfigPropertyProducer.class)
                .addAsWebInfResource("META-INF/beans.xml");
    }

    @Inject
    private HubotNotifier notifier;


    @Test
    public void testServiceInjection() {
        Assert.assertNotNull(notifier);
    }

}
