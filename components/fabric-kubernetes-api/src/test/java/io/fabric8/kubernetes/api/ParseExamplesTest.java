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
package io.fabric8.kubernetes.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.DesiredState;
import io.fabric8.kubernetes.api.model.ManifestContainer;
import io.fabric8.kubernetes.api.model.ManifestSchema;
import io.fabric8.kubernetes.api.model.PodListSchema;
import io.fabric8.kubernetes.api.model.PodSchema;
import org.junit.Test;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static io.fabric8.common.util.Files.assertDirectoryExists;
import static io.fabric8.common.util.Files.assertFileExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Parses the example JSON
 */
public class ParseExamplesTest {
    public static final String SYSTEM_PROPERTY_KUBE_DIR = "kube.dir";

    @Test
    public void testParsePodList() throws Exception {
        PodListSchema podList = assertParseExampleFile("pod-list.json", PodListSchema.class);
        List<PodSchema> items = podList.getItems();
        assertNotEmpty("items", items);

        PodSchema pod = items.get(0);
        assertNotNull("pod1", pod);
        assertEquals("pod1.id", "my-pod-1", pod.getId());
        DesiredState desiredState = pod.getDesiredState();
        assertNotNull("pod1.desiredState", desiredState);
        ManifestSchema manifest = desiredState.getManifest();
        assertNotNull("pod1.desiredState.manifest", manifest);
        List<ManifestContainer> containers = manifest.getContainers();
        assertNotEmpty("pod1.desiredState.manifest.containers", containers);
        ManifestContainer container = containers.get(0);
        assertNotNull("pod1.desiredState.manifest.container[0]", container);
        assertEquals("pod1.desiredState.manifest.container[0].name", "nginx", container.getName());
        assertEquals("pod1.desiredState.manifest.container[0].image", "dockerfile/nginx", container.getImage());

        System.out.println("pod1 container1 " + container);
    }

    @Test
    public void testParsePodListEmptyResults() throws Exception {
        PodListSchema podList = assertParseExampleFile("pod-list-empty-results.json", PodListSchema.class);
        List<PodSchema> items = podList.getItems();
        assertNotEmpty("items", items);

        PodSchema pod = items.get(0);
        assertNotNull("pod1", pod);
        assertEquals("127.0.0.1", pod.getDesiredState().getHost());
        assertEquals("pod1.desiredState.manifest.version", "__EMPTY__",pod.getDesiredState().getManifest().getVersion().name());


    }

    @Test
    public void testParsePod() throws Exception {
        assertParseExampleFile("pod.json", PodSchema.class);
    }

    public static void assertNotEmpty(String name, Collection collection) {
        assertNotNull(name + " is null!", collection);
        assertFalse(name + " should not be empty!", collection.isEmpty());
    }

    public static <T> T assertParseExampleFile(String fileName, Class<T> clazz) throws Exception {
        ObjectMapper mapper = KubernetesFactory.createObjectMapper();
        File exampleFile = new File(getKubernetesExamplesDir(), fileName);
        assertFileExists(exampleFile);
        T answer = mapper.reader(clazz).readValue(exampleFile);
        assertNotNull("Null returned while unmarshalling " + exampleFile, answer);
        System.out.println("Parsed: " + fileName + " as: " + answer);
        System.out.println();
        return answer;
    }

    public static File getKubernetesSourceDir() {
        String path = System.getProperty(SYSTEM_PROPERTY_KUBE_DIR, "../../../kubernetes");
        File kubeDir = new File(path);
        assertTrue("Kube directory " + kubeDir
                + " does not exist! Please supply the correct value in the " + SYSTEM_PROPERTY_KUBE_DIR + " system property value",
                kubeDir.exists());
        return kubeDir;
    }

    public static File getKubernetesExamplesDir() {
        File answer = new File(getKubernetesSourceDir(), "api/examples");
        assertDirectoryExists(answer);
        return answer;
    }
}
