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
package io.fabric8.forge.devops;

import io.fabric8.forge.devops.BuildConfigs;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.utils.cxf.JsonHelper;
import org.junit.Test;

import java.util.Map;

/**
 */
public class BuilderTest {
    @Test
    public void testGenerateBuild() throws Exception {
        String buildName = "my-build";
        String imageTag = "test";
        String image = "fabric8/quickstart-camel-cdi";
        String webhookSecret = "secret101";

        Map<String, String> labels = BuildConfigs.createBuildLabels(buildName);
        ImageStream imageRepository = BuildConfigs.imageRepository(buildName, labels);
        System.out.println("Generated ImageStream: " + JsonHelper.toJson(imageRepository));

        String gitUrl = "https://github.com/jstrachan/example-cd-workflow.git";
        BuildConfig buildConfig = BuildConfigs.createBuildConfig(buildName, labels, gitUrl, imageTag, image, webhookSecret);

        System.out.println("Generated BuildConfig: " + JsonHelper.toJson(buildConfig));
    }


}
