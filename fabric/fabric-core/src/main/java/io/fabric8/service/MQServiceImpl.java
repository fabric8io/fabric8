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
package io.fabric8.service;

import io.fabric8.api.*;
import io.fabric8.utils.Strings;

import java.util.Map;

import static io.fabric8.api.MQService.Config.*;

public class MQServiceImpl implements MQService {

    private FabricService fabricService;


    public MQServiceImpl(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    @Override
    public Profile createOrUpdateMQProfile(String versionId, String profile, String brokerName, Map<String, String> configs, boolean replicated) {
        Version version = fabricService.getVersion(versionId);

        String parentProfileName = null;
        if (configs != null && configs.containsKey("parent")) {
            parentProfileName = configs.remove("parent");
        }
        if (Strings.isNullOrBlank(parentProfileName)) {
            parentProfileName = replicated ? MQ_PROFILE_REPLICATED : MQ_PROFILE_BASE;
        }

        Profile parentProfile = version.getProfile(parentProfileName);
        String pidName = getBrokerPID(brokerName);
        Profile result = parentProfile;
        if (brokerName != null && profile != null) {
            // lets check we have a config value

            // create a profile if it doesn't exist
            Map config = null;
            if (!version.hasProfile(profile)) {
                result = version.createProfile(profile);
                result.setParents(new Profile[]{parentProfile});
            } else {
                result = version.getProfile(profile);
                config = result.getConfiguration(pidName);
            }
            Map<String, String> parentProfileConfig = parentProfile.getConfiguration(MQ_PID_TEMPLATE);
            if (config == null) {
                config = parentProfileConfig;
            }

            config.put("broker-name", brokerName);
            if (configs != null) {
                config.putAll(configs);
            }

            // lets check we've a bunch of config values inherited from the template
            String[] propertiesToDefault = { CONFIG_URL, STANDBY_POOL, CONNECTORS };
            for (String key : propertiesToDefault) {
                if (config.get(key) == null) {
                    String defaultValue = parentProfileConfig.get(key);
                    if (Strings.isNotBlank(defaultValue)) {
                        config.put(key, defaultValue);
                    }
                }
            }
            result.setConfiguration(pidName, config);
        }
        return result;
    }

    @Override
    public Profile createOrUpdateMQClientProfile(String versionId, String profile, String group, String parentProfileName) {
        Version version = fabricService.getVersion(versionId);

        Profile parentProfile = null;
        if (Strings.isNotBlank(parentProfileName)) {
            parentProfile = version.getProfile(parentProfileName);
        }
        Profile result = parentProfile;
        if (group != null && profile != null) {
            // create a profile if it doesn't exist
            Map config = null;
            if (!version.hasProfile(profile)) {
                result = version.createProfile(profile);
            } else {
                result = version.getProfile(profile);
            }

            // set the parent if its specified
            if (parentProfile != null) {
                result.setParents(new Profile[]{parentProfile});
            }

            Map<String, String> parentProfileConfig = result.getConfiguration(MQ_CONNECTION_FACTORY_PID);
            if (config == null) {
                config = parentProfileConfig;
            }
            config.put(GROUP, group);
            result.setConfiguration(MQ_CONNECTION_FACTORY_PID, config);
        }
        return result;
    }


    @Override
    public String getConfig(String version, String config) {
        return "profile:" + config;
    }

    protected String getBrokerPID(String brokerName) {
        return MQ_FABRIC_SERVER_PID_PREFIX + brokerName;
    }

}
