/**
 * Copyright 2005-2016 Red Hat, Inc.
 * <p>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.kubernetes.api.spaces;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static io.fabric8.kubernetes.api.spaces.Spaces.FABRIC8_SPACES;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SpacesTest {

    @Rule
    public OpenShiftServer server = new OpenShiftServer();

    protected KubernetesClient kubernetesClient;


    @Test
    public void testLoadSpaces() {
        String namespace = "myproject";
        String resourceName = "fabric8-spaces.yml";

        KubernetesClient client = getKubernetesClient();
        URL resource = getClass().getClassLoader().getResource(resourceName);
        assertNotNull("Failed to load resource from classpath: " + resourceName, resourceName);

        InputStream inputStream = null;
        try {
            inputStream = resource.openStream();
        } catch (IOException e) {
            fail("Failed to open " + resourceName + ". " + e);
        }
        assertNotNull("Failed to open resource from classpath: " + resourceName, resourceName);
        ConfigMap configMap = null;
        try {
            configMap = KubernetesHelper.loadYaml(inputStream, ConfigMap.class);
        } catch (IOException e) {
            fail("Failed to parse YAML: " + resourceName + ". " + e);
        }

        server.expect().withPath("/api/v1/namespaces/" + namespace + "/configmaps/" + FABRIC8_SPACES).andReturn(200, configMap).once();

        Spaces spaces = Spaces.load(kubernetesClient, namespace);
        List<Space> spaceList = new ArrayList<>(spaces.getSpaceSet());
        assertEquals("Size of spaceList: " + spaceList, 3, spaceList.size());
        Space space0 = spaceList.get(0);
        assertEquals("space0.name", "Foo", space0.getName());

    }


    public KubernetesClient getKubernetesClient() {
        if (kubernetesClient == null) {
            kubernetesClient = server.getKubernetesClient();
        }
        assertNotNull("No KubernetesClient was created by the mock!", kubernetesClient);
        return kubernetesClient;
    }

    public void setKubernetesClient(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }
}