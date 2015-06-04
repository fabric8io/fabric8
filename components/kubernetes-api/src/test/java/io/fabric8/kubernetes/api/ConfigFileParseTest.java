/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.api;

import io.fabric8.kubernetes.api.model.config.Config;
import io.fabric8.kubernetes.api.extensions.Configs;
import io.fabric8.kubernetes.api.model.config.Context;
import org.junit.Test;

import java.io.File;

import static io.fabric8.kubernetes.api.extensions.Configs.getOpenShiftConfigFile;
import static org.assertj.core.api.Assertions.assertThat;

public class ConfigFileParseTest {
    @Test
    public void testParseConfig() throws Exception {
        FindOpenShiftNamespaceTest.setOPenShfitConfigFileProperty();

        File file = getOpenShiftConfigFile();
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

}
