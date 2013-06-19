/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.process.fabric.child;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.google.common.base.Strings;

import org.fusesource.common.util.Objects;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class ChildProcessManager {
    private static final transient Logger LOG = LoggerFactory.getLogger(ChildProcessManager.class);

    private FabricService fabricService;
    private Runnable checkConfigurations = new Runnable() {
        public void run() {
            checkChildProcessConfigurationsChanged();
        }
    };

    public void init() throws Exception {
        Objects.notNull(fabricService, "fabricService");
        fabricService.trackConfiguration(checkConfigurations);
    }

    public void destroy() throws Exception {
        Objects.notNull(fabricService, "fabricService");
        fabricService.unTrackConfiguration(checkConfigurations);
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    protected void checkChildProcessConfigurationsChanged() {
        Container container = fabricService.getCurrentContainer();
        Profile profile = container.getOverlayProfile();
        Map<String,Map<String,String>> configurations = profile.getConfigurations();

        Map<String, String> map = configurations
                .get("org.fusesource.process.fabric.child");

        if (map != null) {
            // lets lets build a model for all the containers we think we should have
            Map<String, ProcessRequirements> requirementsMap = loadProcessRequirements(map);

            System.out.println("Require containers: " + requirementsMap);

            // now for each container, lets either create it if its not already created,
            // or modify its configuration if its created (stopping it first for any removals
            // or changes ot the shared libraries

            // TODO

            for (ProcessRequirements requirements : requirementsMap.values()) {

            }
        }
    }

    private Map<String, ProcessRequirements> loadProcessRequirements(Map<String, String> properties) {
        Map<String,ProcessRequirements> answer = new HashMap<String, ProcessRequirements>();

        Set<Map.Entry<String,String>> entries = properties.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            String value = entry.getValue();

            // lets build up the model of the containers
            String[] split = key.split("\\.");
            if (split != null && split.length > 1) {
                String containerId = split[0];
                String propertyName = split[1];

                ProcessRequirements container = answer.get(containerId);
                if (container == null){
                    container = new ProcessRequirements(containerId);
                    answer.put(containerId, container);
                }

                if (split.length == 2) {
                    if ("kind".equals(propertyName)) {
                        container.setKind(value);
                    } else if ("url".equals(propertyName)) {
                        container.setUrl(value);
                    } else {
                        LOG.warn("Unknown property " + propertyName + " for container process " + containerId);
                    }
                } else if (split.length == 3 && "profile".equals(propertyName)) {
                    StringTokenizer iter = new StringTokenizer(value);
                    while (iter.hasMoreElements()) {
                        String token = iter.nextToken();
                        if (!Strings.isNullOrEmpty(token)) {
                            container.addProfile(value);
                        }
                    }
                } else {
                    LOG.warn("Ignored invalid entry " + key + " = " + value);
                }
            } else {
                LOG.warn("Ignored invalid entry " + key + " = " + value);
            }
            // TODO make the containers...
        }

        return answer;
    }


}
