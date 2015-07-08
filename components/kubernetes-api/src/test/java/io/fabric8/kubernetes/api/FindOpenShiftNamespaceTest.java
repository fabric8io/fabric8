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
package io.fabric8.kubernetes.api;

import io.fabric8.kubernetes.api.extensions.Configs;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FindOpenShiftNamespaceTest {

    @Test
    public void testFindsCorrectKubernetesNamespace() throws Exception {
        setKubernetesConfigFileProperty();
        String namespace = KubernetesClient.findDefaultKubernetesNamespace();

        assertEquals("default namespace", "jimmi-does-rock", namespace);
    }

    public static void setKubernetesConfigFileProperty() {
        String basedir = System.getProperty("basedir", ".");
        String configFile = basedir + "/src/test/resources/config.yml";

        System.setProperty(Configs.KUBERNETES_CONFIG_FILE_PROPERTY, configFile);
    }

}
