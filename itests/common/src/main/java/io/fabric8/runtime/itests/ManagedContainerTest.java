/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.runtime.itests;


import io.fabric8.runtime.container.ContainerConfiguration;
import io.fabric8.runtime.container.ContainerConfigurationBuilder;
import io.fabric8.runtime.container.ManagedContainer;

import org.jboss.gravia.runtime.RuntimeType;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test basic {@link ManagedContainer} functionality
 *
 * @since 26-Feb-2014
 */
public class ManagedContainerTest {

    @Test
    public void testContainerLifecycle() throws Exception {
        RuntimeType runtimeType = RuntimeType.getRuntimeType(System.getProperty("target.container"));
        ContainerConfigurationBuilder builder = ContainerConfigurationBuilder.create(runtimeType);
        ContainerConfiguration configuration = builder.setTargetDirectory("target/managed-container").getConfiguration();
        ManagedContainer<?> container = ManagedContainer.Factory.create(configuration);
        Assert.assertNotNull("ManagedContainer not null", container);
        try {
            Assert.assertNotNull("Container home not null", container.getContainerHome());
            container.start();
        } finally {
            container.destroy();
        }
    }
}