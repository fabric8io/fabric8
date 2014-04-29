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
package io.fabric8.container.process;

import io.fabric8.api.CreateChildContainerMetadata;
import io.fabric8.api.CreateChildContainerOptions;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Profiles;
import io.fabric8.api.scr.support.Strings;
import io.fabric8.common.util.Objects;
import io.fabric8.process.manager.InstallOptions;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents the configuration of a {@link io.fabric8.process.manager.ProcessManager} based child container
 */
@Component(name = "io.fabric8.container.process", label = "Fabric8 Process Child Container Configuration", immediate = false, metatype = true)
public class ProcessContainerConfig {
    @Property(label = "Process name", description = "The descriptive name to refer to this process when listing the processes on this machine.")
    private String processName;

    @Property(label = "Distribution URL", cardinality = 1,
            description = "The URL (usually using maven coordinates) for the distribution to download and unpack.")
    private String url;

    @Property(name = "controllerPath", label = "Controller JSON Path", value = "controller.json",
            description = "The name of the JSON file in the Profile which is used to control the distribution; starting and stopping the process.")
    private String controllerPath = "controller.json";


    public InstallOptions createProcessInstallOptions(FabricService fabricService, CreateChildContainerMetadata metadata, CreateChildContainerOptions options, Map<String, String> environmentVariables) throws MalformedURLException {
        byte[] jsonData = null;
        Set<String> profileIds = options.getProfiles();
        String versionId = options.getVersion();
        List<Profile> profiles = Profiles.getProfiles(fabricService, profileIds, versionId);
        for (Profile profile : profiles) {
            jsonData = profile.getFileConfiguration(controllerPath);
            if (jsonData != null) {
                break;
            }
        }
        Objects.notNull(jsonData, "No JSON file found for path " + controllerPath + " in profiles: " + profileIds + " version: " + versionId);
        String controllerJson = new String(jsonData);
        String installName = getProcessName();
        if (Strings.isNullOrBlank(installName)) {
            if (profiles.size() > 0) {
                installName = profiles.get(0).getId();
            }
        }
        metadata.setContainerType(installName);
        return InstallOptions.builder().id(options.getName()).name(installName).url(url).controllerJson(controllerJson).environment(environmentVariables).build();
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getControllerPath() {
        return controllerPath;
    }

    public void setControllerPath(String controllerPath) {
        this.controllerPath = controllerPath;
    }
}
