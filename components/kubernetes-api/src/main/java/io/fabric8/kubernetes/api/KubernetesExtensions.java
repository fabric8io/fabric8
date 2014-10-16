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
package io.fabric8.kubernetes.api;

import io.fabric8.kubernetes.api.model.PodSchema;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * Various Kubernetes extensions defined in the OpenShift project
 */
@Path("osapi/v1beta1")
@Produces("application/json")
@Consumes("application/json")
public interface KubernetesExtensions {

    @POST
    @Path("configs")
    @Consumes("application/json")
    String createConfig(Object entity) throws Exception;

    @POST
    @Path("templateConfigs")
    @Consumes("application/json")
    String createTemplateConfig(Object entity) throws Exception;

    @POST
    @Path("template")
    @Consumes("application/json")
    String createTemplate(Object entity) throws Exception;
}
