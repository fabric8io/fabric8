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
package io.fabric8.kubernetes.provider;

import org.apache.felix.scr.annotations.Component;

import java.util.List;

/**
 * Represents the configuration of a kubernetes profile for running controllers, pods or services.
 */
@Component(name = KubernetesConstants.KUBERNETES_PID,
        label = "Kubernetes",
        description = "The configuration for running kubernetes controllers, pods or services",
        immediate = true, metatype = true)
public class KubernetesConfig {
    private List<String> definitions;

    public List<String> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(List<String> definitions) {
        this.definitions = definitions;
    }
}
