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
package org.fusesource.fabric.service;

import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.MQService;
import org.fusesource.fabric.api.Profile;

import java.util.Map;

public class MQServiceImpl implements MQService {

    private FabricService fabricService;
    
    private static final String DEFAULT_MQ_PROFILE = "mq";
    private static final String DEFAULT_MQ_PID = "org.fusesource.mq.fabric.";

    public MQServiceImpl(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    @Override
    public Profile createMQProfile(String version, String brokerName, Map<String, String> configs) {
        Profile parentProfile = fabricService.getProfile(version, DEFAULT_MQ_PROFILE);
        Profile result = parentProfile;
        if (brokerName != null) {
            result = fabricService.getProfile(version, brokerName);
            // create a profile if it doesn't exist
            if (result == null) {
                result = fabricService.createProfile(version, brokerName);
                result.setParents(new Profile[]{parentProfile});               
            }
            Map config = parentProfile.getConfigurations().get(DEFAULT_MQ_PID + "template");
            config.put("broker-name", brokerName);
            if (configs != null) {
                config.putAll(configs);
            }
            Map<String, Map<String,String>> newConfigs = result.getConfigurations();
            newConfigs.put(DEFAULT_MQ_PID + "server-" + brokerName, config);
            result.setConfigurations(newConfigs);
        }
        
        return result;
    }

    @Override
    public String getConfig(String version, String config) {
        return "zk:/fabric/configs/versions/" + version + "/profiles/mq/" + config;
    }
}
