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
package io.fabric8.kubernetes.api.builders;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.utils.Strings;

import java.util.ArrayList;
import java.util.List;

/**
 * A little helper class to build a <code>List<EnvVar></code> object
 */
public class ListEnvVarBuilder {
    private List<EnvVar> envVars = new ArrayList<>();

    public void withEnvVar(String name, String value) {
        if (Strings.isNotBlank(name) && value != null) {
            EnvVar envVar = new EnvVar();
            envVar.setName(name);
            envVar.setValue(value);
            envVars.add(envVar);
        }
    }

    public List<EnvVar> build() {
        return envVars;
    }
}
