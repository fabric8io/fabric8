/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.api;

import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import io.fabric8.utils.IOHelpers;
import org.junit.Test;

import java.io.File;

/**
 */
public class YamlSerialiseTest {
    private String basedir = System.getProperty("basedir");

    @Test
    public void testSerialiseYaml() throws Exception {
        Deployment deployment = new DeploymentBuilder().
                withNewMetadata().withName("foo").endMetadata().
                withNewSpec().withReplicas(1).
                withNewTemplate().withNewSpec().addNewContainer().withImage("cheese").endContainer().endSpec().endTemplate().
                endSpec().build();

        File outFile = new File(new File(basedir), "target/test-data/deployment.yml");
        outFile.getParentFile().mkdirs();

        KubernetesHelper.saveYamlNotEmpty(deployment, outFile);

        String yaml = IOHelpers.readFully(outFile);
        System.out.println("YAML: " + yaml);
    }

}
