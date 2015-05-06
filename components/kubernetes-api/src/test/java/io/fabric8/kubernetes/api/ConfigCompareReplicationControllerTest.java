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

import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerStatus;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.fabric8.kubernetes.api.ConfigCompareServiceTest.assertCompareConfig;

public class ConfigCompareReplicationControllerTest {

    private static final transient Logger LOG = LoggerFactory.getLogger(ConfigCompareReplicationControllerTest.class);

    @Test
    public void testReplicationControllersEqual() throws Exception {
        ReplicationController entity1 = new ReplicationControllerBuilder().withNewMetadata().withName("foo").
            addToLabels("label1", "value1").
            addToLabels("label2", "value2").
            endMetadata().
            withNewSpec().
                addToSelector("label1", "value1").
                addToSelector("label2", "value2").
                withReplicas(2).
                withNewTemplate().withNewMetadata().
                    addToLabels("podLabel1", "podValue1").
                    addToLabels("podLabel2", "podValue2").
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
                endTemplate().
            endSpec().
        build();

        ReplicationController entity2 = new ReplicationControllerBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
            addToLabels("label2", "value2").
                endMetadata().
            withNewSpec().
                addToSelector("label1", "value1").
                addToSelector("label2", "value2").
                withReplicas(2).
                withNewTemplate().withNewMetadata().
                addToLabels("podLabel1", "podValue1").
                    addToLabels("podLabel2", "podValue2").
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
                endTemplate().
            endSpec().
        build();

        assertCompareConfig(entity1, entity2, true);
    }

    @Test
    public void testReplicationControllersEqualWithDifferentStatus() throws Exception {
        ReplicationController entity1 = new ReplicationControllerBuilder().withNewMetadata().withName("foo").
            addToLabels("label1", "value1").
            addToLabels("label2", "value2").
            endMetadata().
            withNewSpec().
                addToSelector("label1", "value1").
                addToSelector("label2", "value2").
                withReplicas(2).
                withNewTemplate().withNewMetadata().
                    addToLabels("podLabel1", "podValue1").
                    addToLabels("podLabel2", "podValue2").
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
                endTemplate().
            endSpec().
        build();

        ReplicationControllerStatus status = new ReplicationControllerStatus();
        status.setReplicas(7);
        ReplicationController entity2 = new ReplicationControllerBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
            addToLabels("label2", "value2").
                endMetadata().
            withNewSpec().
                addToSelector("label1", "value1").
                addToSelector("label2", "value2").
                withReplicas(2).
                withNewTemplate().withNewMetadata().
                addToLabels("podLabel1", "podValue1").
                    addToLabels("podLabel2", "podValue2").
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
                endTemplate().
            endSpec().
            withStatus(status).
        build();

        assertCompareConfig(entity1, entity2, true);
    }

    @Test
    public void testReplicationControllersLabelsNotEqual() throws Exception {
        ReplicationController entity1 = new ReplicationControllerBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                endMetadata().
                withNewSpec().
                addToSelector("label1", "value1").
                    addToSelector("label2", "value2").
                withReplicas(2).
                withNewTemplate().withNewMetadata().
                addToLabels("podLabel1", "podValue1").
                    addToLabels("podLabel2", "podValue2").
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
                    endTemplate().
                endSpec().
                build();

        ReplicationController entity2 = new ReplicationControllerBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                endMetadata().
                withNewSpec().
                    addToSelector("label1", "value1").
                    addToSelector("label2", "value2").
                    withReplicas(2).
                    withNewTemplate().withNewMetadata().
                    addToLabels("notSame", "podValue1").
                    addToLabels("podLabel2", "podValue2").
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
                    endTemplate().
                endSpec().
                build();

        assertCompareConfig(entity1, entity2, false);
    }

    @Test
    public void testReplicationControllersSelectorNotEqual() throws Exception {
        ReplicationController entity1 = new ReplicationControllerBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                endMetadata().
                withNewSpec().
                addToSelector("label1", "value1").
                    addToSelector("label2", "value2").
                withReplicas(2).
                withNewTemplate().withNewMetadata().
                addToLabels("podLabel1", "podValue1").
                    addToLabels("podLabel2", "podValue2").
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
                    endTemplate().
                endSpec().
                build();

        ReplicationController entity2 = new ReplicationControllerBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                endMetadata().
                withNewSpec().
                addToSelector("label1", "value1").
                    addToSelector("label2", "notSame").
                withReplicas(2).
                withNewTemplate().withNewMetadata().
                addToLabels("podLabel1", "podValue1").
                    addToLabels("podLabel2", "podValue2").
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
                    endTemplate().
                endSpec().
                build();

        assertCompareConfig(entity1, entity2, false);
    }

    @Test
    public void testReplicationControllersReplicasNotEqual() throws Exception {
        ReplicationController entity1 = new ReplicationControllerBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                endMetadata().
                withNewSpec().
                addToSelector("label1", "value1").
                    addToSelector("label2", "value2").
                withReplicas(2).
                withNewTemplate().withNewMetadata().
                addToLabels("podLabel1", "podValue1").
                    addToLabels("podLabel2", "podValue2").
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
                endTemplate().
            endSpec().
        build();

        ReplicationController entity2 = new ReplicationControllerBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
            addToLabels("label2", "value2").
            endMetadata().
            withNewSpec().
                addToSelector("label1", "value1").
                addToSelector("label2", "value2").
                withReplicas(4).
                withNewTemplate().withNewMetadata().
                addToLabels("podLabel1", "podValue1").
                    addToLabels("podLabel2", "podValue2").
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
                endTemplate().
            endSpec().
        build();

        assertCompareConfig(entity1, entity2, false);
    }

    @Test
    public void testReplicationControllersAnnotationsNotEqual() throws Exception {
        ReplicationController entity1 = new ReplicationControllerBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                endMetadata().
                withNewSpec().
                addToSelector("label1", "value1").
                    addToSelector("label2", "value2").
                withReplicas(2).
                withNewTemplate().withNewMetadata().
                addToLabels("podLabel1", "podValue1").
                    addToLabels("podLabel2", "podValue2").
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
                endTemplate().
            endSpec().
        build();

        ReplicationController entity2 = new ReplicationControllerBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                endMetadata().
                withNewSpec().
                    addToSelector("label1", "value1").
                    addToSelector("label2", "value2").
                    withReplicas(2).
                    withNewTemplate().withNewMetadata().
                    addToLabels("podLabel1", "podValue1").
                    addToLabels("podLabel2", "podValue2").
                    addToAnnotations("podAnnotation1", "notEqual").
                    endMetadata().
                    withNewSpec().
                        addNewContainer().
                            withImage("fabric8/jenkins").
                            addNewEnv().withName("foo").withValue("bar").endEnv().
                            // TODO....
                            // addNewPort().endPort().
                        endContainer().
                    endSpec().
                endTemplate().
            endSpec().
        build();

        assertCompareConfig(entity1, entity2, false);
    }

    @Test
    public void testReplicationControllersImageEqual() throws Exception {
        ReplicationController entity1 = new ReplicationControllerBuilder().withNewMetadata().withName("foo").
            addToLabels("label1", "value1").
            addToLabels("label2", "value2").
            endMetadata().
            withNewSpec().
                addToSelector("label1", "value1").
                addToSelector("label2", "value2").
                withReplicas(2).
                withNewTemplate().withNewMetadata().
                    addToLabels("podLabel1", "podValue1").
                    addToLabels("podLabel2", "podValue2").
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
                endTemplate().
            endSpec().
        build();

        ReplicationController entity2 = new ReplicationControllerBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
            addToLabels("label2", "value2").
            endMetadata().
            withNewSpec().
                addToSelector("label1", "value1").
                addToSelector("label2", "value2").
                withReplicas(2).
                withNewTemplate().withNewMetadata().
                addToLabels("podLabel1", "podValue1").
                    addToLabels("podLabel2", "podValue2").
                addToAnnotations("podAnnotation1", "podAnnValue1").
                    endMetadata().
                    withNewSpec().
                        addNewContainer().
                            withImage("notEqual").
                            addNewEnv().withName("foo").withValue("bar").endEnv().
                            // TODO....
                            // addNewPort().endPort().
                        endContainer().
                    endSpec().
                endTemplate().
            endSpec().
        build();

        assertCompareConfig(entity1, entity2, false);
    }

    @Test
    public void testReplicationControllersContainerEnvEqual() throws Exception {
        ReplicationController entity1 = new ReplicationControllerBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                endMetadata().
                withNewSpec().
                addToSelector("label1", "value1").
                    addToSelector("label2", "value2").
                withReplicas(2).
                withNewTemplate().withNewMetadata().
                addToLabels("podLabel1", "podValue1").
                    addToLabels("podLabel2", "podValue2").
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
                    endTemplate().
                endSpec().
                build();

        ReplicationController entity2 = new ReplicationControllerBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                endMetadata().
                withNewSpec().
                addToSelector("label1", "value1").
                    addToSelector("label2", "value2").
                withReplicas(2).
                withNewTemplate().withNewMetadata().
                addToLabels("podLabel1", "podValue1").
                    addToLabels("podLabel2", "podValue2").
                addToAnnotations("podAnnotation1", "podAnnValue1").
                    endMetadata().
                    withNewSpec().
                        addNewContainer().
                            withImage("fabric8/jenkins").
                            addNewEnv().withName("foo").withValue("notEqual").endEnv().
                            // TODO....
                            // addNewPort().endPort().
                        endContainer().
                    endSpec().
                endTemplate().
            endSpec().
        build();

        assertCompareConfig(entity1, entity2, false);
    }

    @Test
    public void testReplicationControllersAddVolumeNotEqual() throws Exception {
        ReplicationController entity1 = new ReplicationControllerBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                endMetadata().
                withNewSpec().
                addToSelector("label1", "value1").
                    addToSelector("label2", "value2").
                withReplicas(2).
                withNewTemplate().withNewMetadata().
                addToLabels("podLabel1", "podValue1").
                    addToLabels("podLabel2", "podValue2").
                addToAnnotations("podAnnotation1", "podAnnValue1").
                    endMetadata().
                    withNewSpec().
                        addNewContainer().
                            withImage("fabric8/jenkins").
                            addNewVolumeMount().withName("cheese").withMountPath("/foo/cheese").endVolumeMount().
                            addNewEnv().withName("foo").withValue("bar").endEnv().
                            // TODO....
                            // addNewPort().endPort().
                        endContainer().
                    endSpec().
                endTemplate().
            endSpec().
        build();

        ReplicationController entity2 = new ReplicationControllerBuilder().withNewMetadata().withName("foo").
                addToLabels("label1", "value1").
                addToLabels("label2", "value2").
                endMetadata().
                withNewSpec().
                addToSelector("label1", "value1").
                    addToSelector("label2", "value2").
                withReplicas(2).
                withNewTemplate().withNewMetadata().
                    addToLabels("podLabel1", "podValue1").
                    addToLabels("podLabel2", "podValue2").
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
                endTemplate().
            endSpec().
        build();

        assertCompareConfig(entity1, entity2, false);
    }

}
