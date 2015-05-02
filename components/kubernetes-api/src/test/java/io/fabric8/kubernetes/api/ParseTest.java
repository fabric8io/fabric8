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
package io.fabric8.kubernetes.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.extensions.Templates;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerManifest;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodState;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.openshift.api.model.template.Template;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static io.fabric8.kubernetes.api.KubernetesHelper.getContainerPort;
import static io.fabric8.kubernetes.api.KubernetesHelper.getId;
import static io.fabric8.kubernetes.api.KubernetesHelper.toJson;
import static io.fabric8.kubernetes.api.ParseExamplesTest.assertNotEmpty;
import static io.fabric8.utils.Files.assertDirectoryExists;
import static io.fabric8.utils.Files.assertFileExists;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Parses the example JSON
 */
public class ParseTest {

    private static final transient Logger LOG = LoggerFactory.getLogger(ParseTest.class);

    public static final String SYSTEM_PROPERTY_KUBE_DIR = "kube.dir";

    @Test
    public void testParsePodList() throws Exception {
        PodList podList = assertParseExampleFile("pod-list.json", PodList.class);
        List<Pod> items = podList.getItems();
        assertNotEmpty("items", items);

        Pod pod = items.get(0);
        assertNotNull("pod1", pod);
        assertEquals("pod1.id", "my-pod-1", getId(pod));
        PodState desiredState = pod.getDesiredState();
        assertNotNull("pod1.desiredState", desiredState);
        ContainerManifest manifest = desiredState.getManifest();
        assertNotNull("pod1.desiredState.manifest", manifest);
        List<Container> containers = manifest.getContainers();
        assertNotEmpty("pod1.desiredState.manifest.containers", containers);
        Container container = containers.get(0);
        assertNotNull("pod1.desiredState.manifest.container[0]", container);
        assertEquals("pod1.desiredState.manifest.container[0].name", "nginx", container.getName());
        assertEquals("pod1.desiredState.manifest.container[0].image", "dockerfile/nginx", container.getImage());

        LOG.info("pod1 container1 " + container);

        String json = toJson(podList);
        LOG.info("Got JSON: " + json);
    }

    @Test
    public void testParsePodListEmptyResults() throws Exception {
        PodList podList = assertParseExampleFile("pod-list-empty-results.json", PodList.class);
        List<Pod> items = podList.getItems();
        assertNotEmpty("items", items);

        Pod pod = items.get(0);
        assertNotNull("pod1", pod);
        assertEquals("127.0.0.1", pod.getCurrentState().getHost());
        assertEquals("pod1.desiredState.manifest.version", "", pod.getDesiredState().getManifest().getVersion());
    }

    @Test
    public void testParseService() throws Exception {
        Service service = assertParseExampleFile("service.json", Service.class);

        assertEquals("Service", service.getKind());

        int expectedPort = 80;
        assertEquals(expectedPort, getContainerPort(service));

        ObjectMapper mapper = KubernetesFactory.createObjectMapper();

        //mapper.writer().writeValue(System.out, service);
    }

    @Test
    public void testParsePod() throws Exception {
        assertParseExampleFile("pod.json", Pod.class);
    }

    @Ignore("see https://github.com/fabric8io/origin-schema-generator/issues/27")
    public void testParseTemplate() throws Exception {
        Template template = assertParseExampleFile("template.json", Template.class);
        List<Object> objects = Templates.getTemplateObjects(template);
        assertNotEmpty("objects", objects);
        assertTrue("size is " + objects.size(), objects.size() > 3);
        Object service = objects.get(0);
        assertThat(service).isInstanceOf(Service.class);

        Object rc = objects.get(2);
        assertThat(rc).isInstanceOf(ReplicationController.class);

        System.out.println("Generated JSON: " + toJson(template));
    }

    @Test
    public void testParseList() throws Exception {
        KubernetesList list = assertParseExampleFile("list.json", KubernetesList.class);
        List<Object> objects = list.getItems();
        assertNotEmpty("objects", objects);
        assertEquals("size", 2, objects.size());
        Object service = objects.get(0);
        assertThat(service).isInstanceOf(Service.class);

        Object rc = objects.get(1);
        assertThat(rc).isInstanceOf(ReplicationController.class);

        System.out.println("Generated JSON: " + toJson(list));
    }

    public static <T> T assertParseExampleFile(String fileName, Class<T> clazz) throws Exception {
        ObjectMapper mapper = KubernetesFactory.createObjectMapper();
        File exampleFile = new File(getKubernetesExamplesDir(), fileName);
        assertFileExists(exampleFile);
        Object answer = mapper.reader(Object.class).readValue(exampleFile);
        assertNotNull("Null returned while unmarshalling " + exampleFile, answer);
        LOG.info("Parsed: " + fileName + " as: " + answer);
        assertTrue("Is not an instance of " + clazz.getSimpleName() + " was: "+ answer.getClass().getName(), clazz.isInstance(answer));
        return clazz.cast(answer);
    }

    public static File getKubernetesSourceDir() {
        //String path = System.getProperty(SYSTEM_PROPERTY_KUBE_DIR, "../../../kubernetes");
        String basedir = System.getProperty("basedir", ".");
        String kubeSourceDir = basedir + "/src/main/kubernetes";
        String path = System.getProperty(SYSTEM_PROPERTY_KUBE_DIR, kubeSourceDir);
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
