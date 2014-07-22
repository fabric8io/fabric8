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
package io.fabric8.testkit.jolokia;

import io.fabric8.api.FabricRequirements;
import io.fabric8.api.jmx.FabricManagerMBean;
import io.fabric8.internal.RequirementsJson;
import io.fabric8.testkit.FabricRestApi;
import org.jolokia.client.J4pClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A simple implementation of {@link io.fabric8.testkit.FabricRestApi} using Jolokia
 */
public class JolokiaFabricRestApi implements FabricRestApi {
    private final J4pClient jolokia;
    private final FabricManagerMBean fabricManager;
    private String jolokiaUrl = "http://localhost:8181/jolokia";
    private String user = "admin";
    private String password = "admin";
    private String fabricMBean = "io.fabric8:type=Fabric";
    private String defaultRestAPI = "http://localhost:8181/api/fabric8";

    public JolokiaFabricRestApi() {
        jolokia = J4pClient.url(jolokiaUrl).user(user).password(password).build();
        fabricManager = JolokiaClients.createFabricManager(jolokia);
    }

    @Override
    public void setRequirements(FabricRequirements requirements) throws Exception {
        String json = RequirementsJson.toJSON(requirements);
        fabricManager.requirementsJson(json);
    }

    @Override
    public List<Map<String, Object>> containerProperties(String... properties) {
        List<String> list = Arrays.asList(properties);
        return fabricManager.containers(list);
    }

    @Override
    public List<String> containerIdsForProfile(String versionId, String profileId) {
        return fabricManager.containerIdsForProfile(versionId, profileId);
    }

    @Override
    public String getDefaultVersion() {
        return fabricManager.getDefaultVersion();
    }

    @Override
    public List<String> containerIds() throws Exception {
        String[] array = fabricManager.containerIds();
        if (array != null) {
            return Arrays.asList(array);
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    public J4pClient getJolokia() {
        return jolokia;
    }
}
