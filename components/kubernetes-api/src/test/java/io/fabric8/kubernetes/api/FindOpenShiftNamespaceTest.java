/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.api;

import io.fabric8.kubernetes.api.extensions.Configs;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FindOpenShiftNamespaceTest {

    @Test
    public void testFindsCorrectOpenShiftNamespace() throws Exception {
        setOPenShfitConfigFileProperty();
        String namespace = KubernetesClient.findDefaultOpenShiftNamespace();

        assertEquals("default namespace", "jimmi-does-rock", namespace);
    }

    public static void setOPenShfitConfigFileProperty() {
        String basedir = System.getProperty("basedir", ".");
        String configFile = basedir + "/src/test/resources/config.yml";

        System.setProperty(Configs.OPENSHIFT_CONFIG_FILE_PROPERTY, configFile);
    }

}
