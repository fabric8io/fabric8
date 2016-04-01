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

import io.fabric8.kubernetes.api.model.Config;
import io.fabric8.kubernetes.api.extensions.Configs;
import io.fabric8.kubernetes.api.model.Context;
import org.junit.Test;

import java.io.File;

import static io.fabric8.kubernetes.api.extensions.Configs.getKubernetesConfigFile;
import static org.assertj.core.api.Assertions.assertThat;

public class ConfigFileParseTest {
    @Test
    public void testParseConfig() throws Exception {
        setKubernetesConfigFileProperty();

        File file = getKubernetesConfigFile();
        assertThat(file).isFile().exists();

        Config config = Configs.parseConfigs();
        assertThat(config).isNotNull();

        String currentContextName = config.getCurrentContext();
        assertThat(currentContextName).describedAs("currentContext").isEqualTo("default/localhost:8443/admin");
        System.out.println("Found current context name: " + currentContextName);

        Context context = Configs.getCurrentContext(config);
        assertThat(context).describedAs("currentContext").isNotNull();

        assertThat(context.getNamespace()).describedAs("namespace").isEqualTo("jimmi-does-rock");
        assertThat(context.getUser()).describedAs("user").isEqualTo("admin/localhost:8443");
        assertThat(context.getCluster()).describedAs("cluster").isEqualTo("172-28-128-4:8443");

        String token = Configs.getUserToken(config, context);
        assertThat(token).describedAs("token").isEqualTo("ExpectedToken");

        System.out.println("User " + context.getUser() + " has token: " + token);

    }

    public static void setKubernetesConfigFileProperty() {
        String basedir = System.getProperty("basedir", ".");
        String configFile = basedir + "/src/test/resources/config.yml";

        System.setProperty(Configs.KUBERNETES_CONFIG_FILE_PROPERTY, configFile);
    }
}
