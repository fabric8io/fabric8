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
package io.fabric8.testkit.support;

import io.fabric8.api.FabricRequirements;
import io.fabric8.testkit.FabricRestApi;
import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.fabric8.testkit.FabricAssertions.getDTO;

/**
 * A simple implementation of {@link io.fabric8.testkit.FabricRestApi} using Jolokia
 */
public class JolokiaFabricRestApi implements FabricRestApi {
    private final J4pClient jolokia;
    private String jolokiaUrl = "http://localhost:8181/jolokia";
    private String user = "admin";
    private String password = "admin";
    private String fabricMBean = "io.fabric8:type=Fabric";
    private String defaultRestAPI = "http://localhost:8181/api/fabric8";

    public JolokiaFabricRestApi() {
        jolokia = J4pClient.url(jolokiaUrl).user(user).password(password).build();
    }

    @Override
    public void setRequirements(FabricRequirements requirements) {
        // TODO

    }

    @Override
    public Map<String, String> containers() throws Exception {
        String[] properties = {"id"};
        Object[] arguments = {properties};
        J4pExecRequest request = new J4pExecRequest(fabricMBean, "containers(java.util.List)", arguments);
        J4pResponse<J4pExecRequest> response = jolokia.execute(request, "POST");
        Object value = response.getValue();
        if (value instanceof List) {
            List list = (List) value;
            Map<String, String> answer = new HashMap<>();
            for (Object element : list) {
                if (element instanceof Map) {
                    Map map = (Map) element;
                    Object id = map.get("id");
                    if (id != null) {
                        String idText = id.toString();
                        answer.put(idText, defaultRestAPI + "/container/" + idText);
                    }
                }
            }
            return answer;
        } else {
            System.out.println("containers() got value: " + value);
            return null;
        }
    }

    public J4pClient getJolokia() {
        return jolokia;
    }
}
