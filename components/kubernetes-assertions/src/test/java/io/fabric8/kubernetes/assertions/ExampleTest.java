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
import io.fabric8.utils.Block;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static io.fabric8.kubernetes.assertions.Assertions.assertThat;
import static io.fabric8.utils.Asserts.assertAssertionError;


/**
 */
public class ExampleTest {

    @Test
    public void testSomething() throws Exception {
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

}
