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

import io.fabric8.kubernetes.api.model.Env;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 */
public class KubernetesHelper {

    /**
     * Returns a list of {@link Env} objects from an environment variables map
     */
    public static List<Env> createEnv(Map<String, String> environmentVariables) {
        List<Env> answer = new ArrayList<>();
        Set<Map.Entry<String, String>> entries = environmentVariables.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            Env env = new Env();
            env.setName(entry.getKey());
            env.setValue(entry.getValue());
            answer.add(env);
        }
        return answer;
    }
}
