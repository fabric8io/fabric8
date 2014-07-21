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
import io.fabric8.testkit.FabricAssertions;
import io.fabric8.testkit.FabricRestApi;

import java.io.IOException;
import java.util.Map;

import static io.fabric8.testkit.FabricAssertions.getDTO;
import static io.fabric8.testkit.FabricAssertions.getObjectMapper;

/**
 * A simple implementation of {@link FabricRestApi} until we move to CXF 3.x so can just use the JAXRS interfaces directly.
 *
 * TODO we should move to CXF 3 ASAP and then just refactor the *Resource classes in fabric-rest and use those interfaces directly
 * as the REST API
 */
public class SimpleFabricRestApi implements FabricRestApi {
    //private String baseUri = "http://admin:admin@localhost:8181/api/fabric8";
    private String baseUri = "http://localhost:8181/api/fabric8";

    @Override
    public void setRequirements(FabricRequirements requirements) {
        // TODO

    }

    @Override
    public Map<String, String> containers() throws IOException {
        return getDTO(baseUri + "/containers", Map.class);
    }
}
