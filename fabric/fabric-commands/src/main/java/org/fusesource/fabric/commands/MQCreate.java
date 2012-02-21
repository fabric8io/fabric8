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
package org.fusesource.fabric.commands;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.MQService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.service.MQServiceImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Command(name = "mq-create", scope = "fabric", description = "Create a new broker")
public class MQCreate extends ContainerCreate {

    @Option(name = "--broker-name", description = "Broker name to use")
    protected String brokerName;
    @Option(name = "--config", description = "Configuration to use")
    protected String config;

    @Override
    protected Object doExecute() throws Exception {
        MQService service = new MQServiceImpl(fabricService);

        Map<String, String> configuration = new HashMap<String, String>();
        configuration.put("data", bundleContext.getDataFile(brokerName).getAbsolutePath());
        if (config != null) {
            configuration.put("config", service.getConfig(version, config));
        }

        Profile profile = service.createMQProfile(version, brokerName, configuration);
        if (profiles == null) {
            profiles = new ArrayList<String>();
        }
        profiles.add(profile.getId());

        if (parent == null && url == null) {
            url = "child://root";
        }
        return super.doExecute();
    }
}
