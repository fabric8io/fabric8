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
package io.fabric8.kubernetes.assertions;

import io.fabric8.kubernetes.api.model.PodSchema;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static io.fabric8.kubernetes.assertions.Assertions.assertThat;
import static io.fabric8.kubernetes.assertions.Asserts.assertAssertionError;
import static org.assertj.core.api.Assertions.assertThat;


/**
 */
public class ExampleTest {

    @Test
    public void testSomething() throws Exception {
        String expectedId = "abc";
        Map<String,String> expectedLabels = new HashMap<>();
        expectedLabels.put("foo", "bar");

        final PodSchema pod = new PodSchema();
        pod.setId(expectedId);
        pod.setLabels(expectedLabels);

        assertThat(pod).hasId(expectedId).hasLabels(expectedLabels);

        assertAssertionError(new Block() {
            @Override
            public void invoke() throws Exception {
                assertThat(pod).hasId("cheese");
            }
        });

        assertAssertionError(new Block() {
            @Override
            public void invoke() throws Exception {
                Map<String,String> wrongLabels = new HashMap<>();
                wrongLabels.put("bar", "whatnot");
                assertThat(pod).hasLabels(wrongLabels);
            }
        });
    }

}
