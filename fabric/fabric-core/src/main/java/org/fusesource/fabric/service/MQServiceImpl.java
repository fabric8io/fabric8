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

import org.fusesource.fabric.api.*;

import java.util.Map;

public class MQServiceImpl implements MQService {

    private FabricService fabricService;


    public MQServiceImpl(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    @Override
    public Profile createMQProfile(String versionId, String brokerName, Map<String, String> configs) {
        Version version = fabricService.getVersion(versionId);

        String parentProfileName = MQ_PROFILE_BASE;
        if( configs!=null && configs.containsKey("parent") ) {
            parentProfileName = configs.remove("parent");
        }

        Profile parentProfile = version.getProfile(parentProfileName);
        String pidName = "org.fusesource.mq.fabric.server-" + brokerName;
        Profile result = parentProfile;
        if (brokerName != null) {

            // create a profile if it doesn't exist
            Map config = null;

            if (!version.hasProfile(brokerName)) {
                result = version.createProfile(brokerName);
                result.setParents(new Profile[]{parentProfile});
            } else {
                result = version.getProfile(brokerName);
                config = result.getConfigurations().get(pidName);
            }
            
            if (config == null) {
                config = parentProfile.getConfigurations().get(MQ_PID_TEMPLATE);
            }

            config.put("broker-name", brokerName);
            if (configs != null) {
                config.putAll(configs);
            }

            Map<String, Map<String,String>> newConfigs = result.getConfigurations();
            newConfigs.put(pidName, config);
            result.setConfigurations(newConfigs);
        }
        
        return result;
    }

    @Override
    public String getConfig(String version, String config) {
        return "zk:/fabric/configs/versions/" + version + "/profiles/mq-base/" + config;
    }
}
