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

import io.fabric8.api.EnvironmentVariables;
import io.fabric8.common.util.Strings;
import io.fabric8.testkit.FabricControllerManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.fabric8.common.util.Strings.join;

/**
 */
public abstract class FabricControllerManagerSupport implements FabricControllerManager {
    private Set<String> profiles = new HashSet<>();
    private String[] allowedEnvironmentVariables = {"JAVA_HOME", "DYLD_LIBRARY_PATH", "LD_LIBRARY_PATH", "MAVEN_HOME", "PATH", "USER"};

    protected FabricControllerManagerSupport() {
        profiles.add("autoscale");
    }

    public String[] getAllowedEnvironmentVariables() {
        return allowedEnvironmentVariables;
    }

    public void setAllowedEnvironmentVariables(String[] allowedEnvironmentVariables) {
        this.allowedEnvironmentVariables = allowedEnvironmentVariables;
    }

    public Set<String> getProfiles() {
        return profiles;
    }

    public void setProfiles(Set<String> profiles) {
        this.profiles = profiles;
    }

    protected Map<String, String> createEnvironmentVariables() {
        Map<String, String> answer = new HashMap<>();
        Map<String, String> current = System.getenv();
        for (String variable : allowedEnvironmentVariables) {
            String value = current.get(variable);
            if (Strings.isNotBlank(value)) {
                answer.put(variable, value);
            }
        }
        answer.put(EnvironmentVariables.FABRIC8_PROFILES, join(getProfiles(), ","));
        return answer;
    }
}
