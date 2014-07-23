/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.itests.autoscale;

import io.fabric8.api.FabricRequirements;
import io.fabric8.testkit.FabricAssertions;
import io.fabric8.testkit.FabricController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 */
@RunAsClient
@RunWith(Arquillian.class)
public class AutoScaleSingleMessageBrokerTest {

    @ArquillianResource
    protected FabricController fabricController;

    @Test
    public void createProvisionedFabric() throws Exception {
        System.out.println("We are now inside a created fabric; lets define our requirements");

        FabricRequirements requirements = new FabricRequirements();
        requirements.profile("mq-default").minimumInstances(1);

        FabricAssertions.assertSetRequirementsAndTheyAreSatisfied(fabricController, requirements);
    }
}
