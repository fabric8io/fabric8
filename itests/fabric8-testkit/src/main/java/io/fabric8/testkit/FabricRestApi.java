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
package io.fabric8.testkit;

import io.fabric8.api.FabricRequirements;
import org.jolokia.client.exception.J4pException;

import java.io.IOException;
import java.util.Map;

/**
 * Represents a REST API to working with the Fabric.
 *
 * NOTE as soon as we go CXF 3.x we can reuse Java interfaces for the fabric-rest module and use that directly.
 *
 * Until then we'll hack a little REST API by hand...
 */
public interface FabricRestApi {
    void setRequirements(FabricRequirements requirements);

    Map<String,String> containers() throws Exception;
}
