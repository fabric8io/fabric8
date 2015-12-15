/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.devops.connector;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.fabric8.devops.connector.DevOpsConnector.loadJenkinsBuildTemplate;
import static io.fabric8.devops.connector.DevOpsConnector.removeBuildParameter;
import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class RemoveBuildParameterTest {
    private static final transient Logger LOG = LoggerFactory.getLogger(RemoveBuildParameterTest.class);


    @Test
    public void testRemoveBuildParameterTest() throws Exception {
        String template = loadJenkinsBuildTemplate(LOG);
        assertThat(template).isNotEmpty();

        String transformed = removeBuildParameter(LOG, template, "GIT_URL");
        transformed = removeBuildParameter(LOG, transformed, "VERSION_PREFIX");
        LOG.info("Transformed XML to: " + transformed);

        assertThat(transformed).doesNotContain("hudson.model.ParametersDefinitionProperty");
        assertThat(transformed).doesNotContain("parameterDefinitions");

    }

}
