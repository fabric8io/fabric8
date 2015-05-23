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

import io.fabric8.kubernetes.api.extensions.Templates;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.openshift.api.model.template.Template;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class TemplatesTest {

    private static final transient Logger LOG = LoggerFactory.getLogger(TemplatesTest.class);

    @Test
    public void testCombineResourcesIntoTemplate() throws Exception {
        Service templateService = new ServiceBuilder().withNewMetadata().withName("templateService").endMetadata().build();

        KubernetesList list = new KubernetesListBuilder().
                addNewServiceItem().withNewMetadata().withName("service1").endMetadata().endServiceItem().
                addNewTemplateItem().
                addNewParameter().withName("PARAM1").withValue("ABC").endParameter().
                addToObjects(templateService).endTemplateItem().
                addNewServiceItem().withNewMetadata().withName("service2").endMetadata().endServiceItem().build();

        Object result = Templates.combineTemplates(list);
        System.out.println("Combined as " + KubernetesHelper.toJson(result));
        assertThat(result).isInstanceOf(Template.class);

    }
}
