/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.kubernetes.assertions;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.utils.Block;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static io.fabric8.kubernetes.assertions.Assertions.assertThat;
import static io.fabric8.utils.Asserts.assertAssertionError;


/**
 */
public class ExampleTest {

    @Test
    public void testNavigationAssertions() throws Exception {
        String expectedId = "abc";
        Map<String, String> expectedLabels = new HashMap<>();
        expectedLabels.put("foo", "bar");

        final Pod pod = new Pod();
        pod.setMetadata(new ObjectMeta());
        pod.getMetadata().setName(expectedId);
        pod.getMetadata().setLabels(expectedLabels);


        assertThat(pod).metadata().name().isEqualTo(expectedId);
        assertThat(pod).metadata().labels().isEqualTo(expectedLabels);

        assertAssertionError(new Block() {
            @Override
            public void invoke() throws Exception {
                assertThat(pod).metadata().name().isEqualTo("cheese");
            }
        });

        assertAssertionError(new Block() {
            @Override
            public void invoke() throws Exception {
                assertThat(pod).describedAs("my pod").metadata().name().isEqualTo("cheese");
            }
        });

        assertAssertionError(new Block() {
            @Override
            public void invoke() throws Exception {
                Map<String, String> wrongLabels = new HashMap<>();
                wrongLabels.put("bar", "whatnot");
                assertThat(pod).metadata().labels().isEqualTo(wrongLabels);
            }
        });
    }

    @Test
    public void testNullNavigationOnPod() throws Exception {
        final Pod pod = new Pod();
        pod.setMetadata(null);

        assertAssertionError(new Block() {
            @Override
            public void invoke() throws Exception {
                assertThat(pod).metadata().name().isEqualTo("cheese");
            }
        });
    }

    @Test
    public void testNullNavigationOnRC() throws Exception {
        final ReplicationController rc = new ReplicationController();

        assertAssertionError(new Block() {
            @Override
            public void invoke() throws Exception {
                assertThat(rc).spec().template().spec().containers().first().image().isEqualTo("someDockerImageName");
            }
        });
    }

    @Test
    public void testNavigationListAssertions() throws Exception {
        final String id1 = "abc";
        final String id2 = "def";
        Map<String, String> labels1 = new HashMap<>();
        labels1.put("foo", "bar");
        Map<String, String> labels2 = new HashMap<>();
        labels2.put("whatnot", "cheese");

        final Pod pod1 = new Pod();
        pod1.setMetadata(new ObjectMeta());
        pod1.getMetadata().setName(id1);
        pod1.getMetadata().setLabels(labels1);

        final Pod pod2 = new Pod();
        pod2.setMetadata(new ObjectMeta());
        pod2.getMetadata().setName(id2);
        pod2.getMetadata().setLabels(labels2);


        final PodList emptyPodList = new PodList();
        final PodList podList = new PodList();
        podList.setItems(new ArrayList<Pod>(Arrays.asList(pod1, pod2)));

        assertThat(emptyPodList).describedAs("emptyPodList").items().isEmpty();
        assertThat(podList).describedAs("podListWith2Items").items().first().metadata().name().isEqualTo(id1);
        assertThat(podList).describedAs("podListWith2Items").items().last().metadata().name().isEqualTo(id2);

        assertAssertionError(new Block() {
            @Override
            public void invoke() throws Exception {
                assertThat(podList).describedAs("podListWith2Items").items().item(-1).isNotNull();
            }
        });

        assertAssertionError(new Block() {
            @Override
            public void invoke() throws Exception {
                assertThat(podList).describedAs("podListWith2Items").items().item(2).isNotNull();
            }
        });

        assertAssertionError(new Block() {
            @Override
            public void invoke() throws Exception {
                assertThat(podList).describedAs("podListWith2Items").items().first().metadata().name().isEqualTo("shouldNotMatch");
            }
        });
    }

}
