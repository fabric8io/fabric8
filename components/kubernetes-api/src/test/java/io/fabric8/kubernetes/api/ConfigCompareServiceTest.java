/**
 * Copyright 2005-2014 Red Hat, Inc.
 * <p/>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.kubernetes.api;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.util.IntOrString;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class ConfigCompareServiceTest {

    private static final transient Logger LOG = LoggerFactory.getLogger(ConfigCompareServiceTest.class);

    @Test
    public void testServicesEqual() throws Exception {
        Service entity1 = new ServiceBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                endMetadata().
                withNewSpec().
                addToSelector("label1", "value1").
                addToSelector("label2", "value2").
                addNewPort().
                withPort(123).
                withTargetPort(new IntOrString(456)).
                endPort().
                endSpec().
                build();

        Service entity2 = new ServiceBuilder().withNewMetadata().withName("foo").
                addToLabels("label2", "value2").
                addToLabels("label1", "value1").
                endMetadata().
                withNewSpec().
                addToSelector("label1", "value1").
                addToSelector("label2", "value2").
                addNewPort().
                withPort(123).
                withTargetPort(new IntOrString(456)).
                endPort().
                endSpec().
                build();

        assertCompareConfig(entity1, entity2, true);
    }
    @Test

    public void testServicesEqualWithStatusdifferences() throws Exception {
        Service entity1 = new ServiceBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                withResourceVersion("1").
                endMetadata().
                withNewSpec().
                addToSelector("label1", "value1").
                addToSelector("label2", "value2").
                addNewPort().
                withPort(123).
                withTargetPort(new IntOrString(456)).
                endPort().
                endSpec().
                build();

        Service entity2 = new ServiceBuilder().withNewMetadata().withName("foo").
                addToLabels("label2", "value2").
                addToLabels("label1", "value1").
                withResourceVersion("2").
                endMetadata().
                withNewSpec().
                addToSelector("label1", "value1").
                addToSelector("label2", "value2").
                addNewPort().
                withPort(123).
                withTargetPort(new IntOrString(456)).
                endPort().
                endSpec().
                build();

        assertCompareConfig(entity1, entity2, true);
    }

    @Test
    public void testServicesPortNotEqual() throws Exception {
        Service entity1 = new ServiceBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                endMetadata().
                withNewSpec().
                addToSelector("label1", "value1").
                addToSelector("label2", "value2").
                addNewPort().
                withPort(123).
                withTargetPort(new IntOrString(456)).
                endPort().
                endSpec().
                build();

        Service entity2 = new ServiceBuilder().withNewMetadata().withName("foo").
                addToLabels("label2", "value2").
                addToLabels("label1", "value1").
                endMetadata().
                withNewSpec().
                addToSelector("label1", "value1").
                addToSelector("label2", "value2").
                addNewPort().
                withPort(456).
                withTargetPort(new IntOrString(456)).
                endPort().
                endSpec().
                build();

        assertCompareConfig(entity1, entity2, false);
    }

    @Test
    public void testServicesContainerPortNotEqual() throws Exception {
        Service entity1 = new ServiceBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                endMetadata().
                withNewSpec().
                addToSelector("label1", "value1").
                addToSelector("label2", "value2").
                addNewPort().
                withPort(123).
                withTargetPort(new IntOrString(456)).
                        endPort().
                        endSpec().
                        build();

        Service entity2 = new ServiceBuilder().withNewMetadata().withName("foo").
                addToLabels("label2", "value2").
                addToLabels("label1", "value1").
                endMetadata().
                withNewSpec().
                addToSelector("label1", "value1").
                addToSelector("label2", "value2").
                addNewPort().
                withPort(123).
                withTargetPort(new IntOrString(555)).
                endPort().
                endSpec().
                build();

        assertCompareConfig(entity1, entity2, false);
    }

    @Test
    public void testServicesLabelsNotEqual() throws Exception {
        Service entity1 = new ServiceBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                endMetadata().
                withNewSpec().
                addToSelector("label1", "value1").
                addToSelector("label2", "value2").
                addNewPort().
                withPort(123).
                withTargetPort(new IntOrString(456)).
                endPort().
                endSpec().
                build();

        Service entity2 = new ServiceBuilder().withNewMetadata().withName("foo").
                addToLabels("label2", "value2").
                addToLabels("notSame", "value1").
                endMetadata().
                withNewSpec().
                addToSelector("label1", "value1").
                addToSelector("label2", "value2").
                addNewPort().
                withPort(123).
                withTargetPort(new IntOrString(456)).
                endPort().
                endSpec().
                build();

        assertCompareConfig(entity1, entity2, false);
    }

    @Test
    public void testServicesSelectorNotEqual() throws Exception {
        Service entity1 = new ServiceBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                endMetadata().
                withNewSpec().
                    addToSelector("label1", "value1").
                    addToSelector("label2", "value2").
                    addNewPort().
                        withPort(123).
                        withTargetPort(new IntOrString(456)).
                endPort().
                endSpec().
                build();

        Service entity2 = new ServiceBuilder().withNewMetadata().withName("foo").
                addToLabels("label2", "value2").
                addToLabels("label1", "value1").
                endMetadata().
                withNewSpec().
                addToSelector("label1", "value1").
                addToSelector("notSame", "value2").
                addNewPort().
                withPort(123).
                withTargetPort(new IntOrString(456)).
                endPort().
                endSpec().
                build();

        assertCompareConfig(entity1, entity2, false);
    }

    public static void assertCompareConfig(Object entity1, Object entity2, boolean expected) {
        boolean actual = UserConfigurationCompare.configEqual(entity1, entity2);
        assertEquals("Configuration compare for " + entity1 + " and " + entity2, expected, actual);
    }
}
