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
package io.fabric8.kubernetes.api;

import io.fabric8.kubernetes.api.extensions.Templates;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.openshift.api.model.Template;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.fabric8.kubernetes.api.KubernetesHelper.toJson;
import static io.fabric8.kubernetes.api.ParseExamplesTest.assertNotEmpty;
import static io.fabric8.kubernetes.api.ParseTest.assertParseExampleFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * Parses the example JSON
 */
public class ProcessTemplateLocallyTest {
    private static final transient Logger LOG = LoggerFactory.getLogger(ProcessTemplateLocallyTest.class);

    @Test
    public void testProcessTemplateLocally() throws Exception {
        Template template = assertParseExampleFile("template.json", Template.class);
        List<HasMetadata> objects = template.getObjects();
        assertNotEmpty("objects", objects);


        KubernetesList list = Templates.processTemplatesLocally(template, true);
        assertThat(list).describedAs("results").isNotNull();
        List<HasMetadata> items = list.getItems();
        assertThat(items).describedAs("items").isNotNull();

        ReplicationController rc = (ReplicationController) items.get(1);
        assertEquals("Template value not replaced: items[1].spec.template.spec.containers[0].env[0].value", "https://github.com/fabric8io/jenkins-pipeline-dsl.git", rc.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv().get(0).getValue());

        System.out.println("Created JSON: " + toJson(list));
    }

}
