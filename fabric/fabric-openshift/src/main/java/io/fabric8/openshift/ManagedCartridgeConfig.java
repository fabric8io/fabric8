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
package io.fabric8.openshift;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.Properties;

import com.openshift.client.IApplication;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.OpenShiftConnectionFactory;

import org.fusesource.common.util.Maps;
import io.fabric8.api.DataStore;
import io.fabric8.api.FabricService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the configuration of a Fabric managed OpenShift cartridge so that the
 * {@link io.fabric8.openshift.agent.OpenShiftDeployAgent} can keep the cartridge's
 * git repository up to date with the deployment units defined in the Profile configuration.
 */
public class ManagedCartridgeConfig {
    private static final transient Logger LOG = LoggerFactory.getLogger(ManagedCartridgeConfig.class);

    private static final String KEY_DOMAIN = "domain";
    private static final String KEY_LOGIN = "login";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_SERVER_URL = "serverUrl";

    private static boolean substitutedStringValue = false;

    private final Properties properties;

    /**
     * Loads the managed cartridge configuration for the given containerId
     */
    public static ManagedCartridgeConfig loadConfig(FabricService fabricService, String containerId)
            throws IOException {
        String propertiesText = fabricService.getDataStore().getContainerAttribute(containerId,
                DataStore.ContainerAttribute.OpenShift, "", false, substitutedStringValue);

        if (propertiesText == null) {
            return null;
        }
        Properties properties = new Properties();
        properties.load(new StringReader(propertiesText));
        ManagedCartridgeConfig answer = new ManagedCartridgeConfig(properties);

        LOG.info("Loaded managed cartridge configuration " + answer);
        return answer;
    }

    /**
     * Saves the managed cartridge configuration data
     */
    public static ManagedCartridgeConfig saveConfig(FabricService fabricService,
                                                    String containerId,
                                                    CreateOpenshiftContainerOptions options,
                                                    IApplication application) throws IOException {

        ManagedCartridgeConfig config = new ManagedCartridgeConfig();
        config.setServerUrl(options.getServerUrl());
        config.setLogin(options.getLogin());
        config.setPassword(options.getPassword());


        StringWriter writer = new StringWriter();
        config.getProperties().store(writer, "Saved by " + config.getClass() + " at " + new Date());
        String propertiesText = writer.toString();

        LOG.info("Saved managed cartridge configuration: " + propertiesText);

        fabricService.getDataStore().setContainerAttribute(containerId,
                DataStore.ContainerAttribute.OpenShift, propertiesText);
        return config;
    }


    public ManagedCartridgeConfig() {
        this(new Properties());
    }

    public ManagedCartridgeConfig(Properties properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "ManagedCartridgeConfig(serverUrl: " + getServerUrl() + "; login: " + getLogin() + ")";
    }

    public Properties getProperties() {
        return properties;
    }


    /**
     * Returns a newly created connection to openshift for this configuration
     */
    public IOpenShiftConnection createConnection() {
        return new OpenShiftConnectionFactory()
                .getConnection("fabric", getLogin(), getPassword(), getServerUrl());
    }

    // Properties
    //-------------------------------------------------------------------------

    public String getLogin() {
        return Maps.stringValue(properties, KEY_LOGIN);
    }

    public void setLogin(String login) {
        Maps.setValue(properties, KEY_LOGIN, login);
    }

    public String getPassword() {
        return Maps.stringValue(properties, KEY_PASSWORD);
    }

    public void setPassword(String password) {
        Maps.setValue(properties, KEY_PASSWORD, password);
    }

    public String getServerUrl() {
        return Maps.stringValue(properties, KEY_SERVER_URL);
    }

    public void setServerUrl(String serverUrl) {
        Maps.setValue(properties, KEY_SERVER_URL, serverUrl);
    }

    public String getDomain() {
        return Maps.stringValue(properties, KEY_DOMAIN);
    }

    public void setDomain(String serverUrl) {
        Maps.setValue(properties, KEY_DOMAIN, serverUrl);
    }

}
