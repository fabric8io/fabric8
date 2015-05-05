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

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class KubernetesHelperTest {

    @Test
    public void testRemoveEmptyPods() throws Exception {

        Pod pod1 = new Pod();
        pod1.setMetadata(new ObjectMeta());
        pod1.getMetadata().setName("test1");

        Pod pod2 = new Pod();
        pod2.setMetadata(new ObjectMeta());

        PodList podSchema = new PodList();
        podSchema.getItems().add(pod1);
        podSchema.getItems().add(pod2);

        KubernetesHelper.removeEmptyPods(podSchema);

        assertNotNull(podSchema);
        assertEquals(1, podSchema.getItems().size());
    }

    @Test
    public void testfilterMatchesIdOrLabels() throws Exception {
        String text = "container=java,name=foo,food=cheese";
        String id = "foo";
        HashMap<String, String> map = new HashMap<>();
        map.put("container", "java");
        map.put("name", "foo");
        map.put("food", "cheese");
        assertTrue(text + " should = " + map, KubernetesHelper.filterMatchesIdOrLabels(text, id, map));
    }

    @Test
    public void testfilterMatchesIdOrLabelsNoLabels() throws Exception {
        String text = "container=java,name=foo,food=cheese";
        String id = "foo";
        HashMap<String, String> map = null;
        assertFalse(text + " should not = " + map, KubernetesHelper.filterMatchesIdOrLabels(text, id, map));
    }

}
