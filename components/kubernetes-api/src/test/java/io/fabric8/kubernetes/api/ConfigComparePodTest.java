/**
 * Copyright 2005-2014 Red Hat, Inc.
 * <p/>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.kubernetes.api;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodStatus;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.fabric8.kubernetes.api.ConfigCompareServiceTest.assertCompareConfig;

public class ConfigComparePodTest {

    private static final transient Logger LOG = LoggerFactory.getLogger(ConfigComparePodTest.class);

    @Test
    public void testPodsEqual() throws Exception {
        Pod entity1 = new PodBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                addToAnnotations("podAnnotation1", "podAnnValue1").
                endMetadata().
                withNewSpec().
                        addNewContainer().
                            withImage("fabric8/jenkins").
                            addNewEnv().withName("foo").withValue("bar").endEnv().
                            // TODO....
                            // addNewPort().endPort().
                        endContainer().
                endSpec().
                build();

        Pod entity2 = new PodBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                addToAnnotations("podAnnotation1", "podAnnValue1").
                endMetadata().
                withNewSpec().
                addNewContainer().
                withImage("fabric8/jenkins").
                addNewEnv().withName("foo").withValue("bar").endEnv().
                            // TODO....
                            // addNewPort().endPort().
                        endContainer().
                endSpec().
                build();

        assertCompareConfig(entity1, entity2, true);
    }

    @Test
    public void testPodsEqualWithDifferentStatus() throws Exception {
        Pod entity1 = new PodBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                addToAnnotations("podAnnotation1", "podAnnValue1").
                endMetadata().
                withNewSpec().
                        addNewContainer().
                            withImage("fabric8/jenkins").
                            addNewEnv().withName("foo").withValue("bar").endEnv().
                            // TODO....
                            // addNewPort().endPort().
                        endContainer().
                endSpec().
                build();

        PodStatus status2 = new PodStatus();
        status2.setHostIP("abc");

        Pod entity2 = new PodBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                addToAnnotations("podAnnotation1", "podAnnValue1").
                endMetadata().
                withNewSpec().
                addNewContainer().
                withImage("fabric8/jenkins").
                addNewEnv().withName("foo").withValue("bar").endEnv().
                            // TODO....
                            // addNewPort().endPort().
                        endContainer().
                endSpec().
                withStatus(status2).
                build();

        assertCompareConfig(entity1, entity2, true);
    }

    @Test
    public void testPodsLabelsNotEqual() throws Exception {
        Pod entity1 = new PodBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                addToAnnotations("podAnnotation1", "podAnnValue1").
                endMetadata().
                withNewSpec().
                addNewContainer().
                withImage("fabric8/jenkins").
                addNewEnv().withName("foo").withValue("bar").endEnv().
                            // TODO....
                            // addNewPort().endPort().
                        endContainer().
                endSpec().
                build();

        Pod entity2 = new PodBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "notSame").
                addToAnnotations("podAnnotation1", "podAnnValue1").
                endMetadata().
                withNewSpec().
                addNewContainer().
                withImage("fabric8/jenkins").
                addNewEnv().withName("foo").withValue("bar").endEnv().
                            // TODO....
                            // addNewPort().endPort().
                        endContainer().
                endSpec().
                build();

        assertCompareConfig(entity1, entity2, false);
    }

    @Test
    public void testPodsAnnotationsNotEqual() throws Exception {
        Pod entity1 = new PodBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                addToAnnotations("podAnnotation1", "podAnnValue1").
                endMetadata().
                withNewSpec().
                addNewContainer().
                withImage("fabric8/jenkins").
                addNewEnv().withName("foo").withValue("bar").endEnv().
                            // TODO....
                            // addNewPort().endPort().
                        endContainer().
                endSpec().
                build();

        Pod entity2 = new PodBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                addToAnnotations("podAnnotation1", "notSame").
                endMetadata().
                withNewSpec().
                addNewContainer().
                withImage("fabric8/jenkins").
                addNewEnv().withName("foo").withValue("bar").endEnv().
                            // TODO....
                            // addNewPort().endPort().
                        endContainer().
                endSpec().
                build();

        assertCompareConfig(entity1, entity2, false);
    }

    @Test
    public void testPodsImageEqual() throws Exception {
        Pod entity1 = new PodBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                addToAnnotations("podAnnotation1", "podAnnValue1").
                endMetadata().
                withNewSpec().
                addNewContainer().
                withImage("fabric8/jenkins").
                addNewEnv().withName("foo").withValue("bar").endEnv().
                            // TODO....
                            // addNewPort().endPort().
                        endContainer().
                endSpec().
                build();

        Pod entity2 = new PodBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                addToAnnotations("podAnnotation1", "podAnnValue1").
                endMetadata().
                withNewSpec().
                addNewContainer().
                withImage("notSame").
                addNewEnv().withName("foo").withValue("bar").endEnv().
                            // TODO....
                            // addNewPort().endPort().
                        endContainer().
                endSpec().
                build();

        assertCompareConfig(entity1, entity2, false);
    }
    @Test
    public void testPodsContainerEnvEqual() throws Exception {
        Pod entity1 = new PodBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                addToAnnotations("podAnnotation1", "podAnnValue1").
                endMetadata().
                withNewSpec().
                addNewContainer().
                withImage("fabric8/jenkins").
                addNewEnv().withName("foo").withValue("bar").endEnv().
                            // TODO....
                            // addNewPort().endPort().
                        endContainer().
                endSpec().
                build();

        Pod entity2 = new PodBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                addToAnnotations("podAnnotation1", "podAnnValue1").
                endMetadata().
                withNewSpec().
                addNewContainer().
                withImage("fabric8/jenkins").
                addNewEnv().withName("foo").withValue("notSame").endEnv().
                            // TODO....
                            // addNewPort().endPort().
                        endContainer().
                endSpec().
                build();

        assertCompareConfig(entity1, entity2, false);
    }
}
