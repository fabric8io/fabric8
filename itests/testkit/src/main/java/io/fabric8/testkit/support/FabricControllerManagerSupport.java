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

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.fabric8.common.util.Strings.join;

/**
 */
public abstract class FabricControllerManagerSupport implements FabricControllerManager {
    public static final String JAVA_OPTS = "JAVA_OPTS";
    public static final String DEFAULT_JAVA_OPTS = "-XX:MaxPermSize=350m";

    protected File workDirectory;
    private Set<String> profiles = new HashSet<>();
    private String[] allowInheritedEnvironmentVariables = {"JAVA_HOME", "DYLD_LIBRARY_PATH", "LD_LIBRARY_PATH", "MAVEN_HOME", "PATH", "USER"};
    private Map<String,String> environmentVariables = new HashMap<>();

    protected FabricControllerManagerSupport() {
        profiles.add("autoscale");
    }

    public String[] getAllowInheritedEnvironmentVariables() {
        return allowInheritedEnvironmentVariables;
    }

    /**
     * Sets the names of the environment variables which are passed through to any child process created
     */
    public void setAllowInheritedEnvironmentVariables(String... allowInheritedEnvironmentVariables) {
        this.allowInheritedEnvironmentVariables = allowInheritedEnvironmentVariables;
    }

    public Set<String> getProfiles() {
        return profiles;
    }

    public void setProfiles(Collection<String> profiles) {
        Set<String> set = new HashSet<>();
        set.addAll(profiles);
        this.profiles = set;
    }

    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    protected Map<String, String> createChildEnvironmentVariables() {
        Map<String, String> answer = new HashMap<>();
        Map<String, String> current = System.getenv();
        for (String variable : allowInheritedEnvironmentVariables) {
            String value = current.get(variable);
            if (Strings.isNotBlank(value)) {
                answer.put(variable, value);
            }
        }
        if (environmentVariables != null) {
            answer.putAll(environmentVariables);
        }
        answer.put(EnvironmentVariables.FABRIC8_PROFILES, join(getProfiles(), ","));
        if (answer.get(JAVA_OPTS) == null) {
            answer.put(JAVA_OPTS, DEFAULT_JAVA_OPTS);
        }
        return answer;
    }

    public void setEnvironmentVariable(String name, String value) {
        if (environmentVariables == null) {
            environmentVariables = new HashMap<>();
        }
        if (value == null) {
            environmentVariables.remove(name);
        } else {
            environmentVariables.put(name, value);
        }
    }

    public File getWorkDirectory() {
        return workDirectory;
    }

    public void setWorkDirectory(File workDirectory) {
        this.workDirectory = workDirectory;
    }
}
