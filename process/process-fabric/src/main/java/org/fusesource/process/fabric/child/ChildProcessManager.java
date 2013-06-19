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

        System.out.println("child map: " + map);
        if (map != null) {
            // lets lets build a model for all the containers we think we should have
            Map<String, ContainerRequirements> containerRequirements = loadContainerRequiremnets(map);


            // now for each container, lets either create it if its not already created,
            // or modify its configuration if its created (stopping it first for any removals
            // or changes ot the shared libraries

            // TODO
        }
    }

    private Map<String, ContainerRequirements> loadContainerRequiremnets(Map<String, String> properties) {
        Map<String,ContainerRequirements> answer = new HashMap<String, ContainerRequirements>();

        Set<Map.Entry<String,String>> entries = properties.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            String value = entry.getValue();

            // lets build up the model of the containers

            // TODO make the containers...
        }

        return answer;
    }


}
