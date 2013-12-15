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
package io.fabric8.security.sso.activemq;

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerPlugin;
import io.fabric8.security.sso.client.OpenAMRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 * Adds the OpenAM authentication/authorization plugin
 *
 * @org.apache.xbean.XBean description="Provides an OpenAM authentication and authorization plugin"
 *
 *
 */
public class OpenAMAuthenticationPlugin implements BrokerPlugin {

    private static Logger LOG = LoggerFactory.getLogger(OpenAMAuthenticationPlugin.class);

    protected String configuration = "activemq-domain";
    protected OpenAMRestClient client = new OpenAMRestClient();
    protected boolean discoverLoginConfig = true;
    protected boolean authorizeSend = false;

    public Broker installPlugin(Broker broker) {
        LOG.info("Installing OpenAM Authentication plugin - {}", this);
        initialiseJaas();

        OpenAMAuthenticationBroker rc = new OpenAMAuthenticationBroker(broker, configuration, client);
        rc.setAuthorizeSend(authorizeSend);
        return rc;
    }

    @Override
    public String toString() {
        return "OpenAMAuthenticationPlugin{" +
                "configuration='" + configuration + '\'' +
                ", client=" + client +
                ", discoverLoginConfig=" + discoverLoginConfig +
                '}';
    }

    // Properties
    // -------------------------------------------------------------------------
    public String getConfiguration() {
        return configuration;
    }

    public void setClient(OpenAMRestClient client) {
        this.client = client;
    }

    public OpenAMRestClient getClient() {
        return client;
    }

    /**
     * Sets the JAAS configuration domain name used
     */
    public void setConfiguration(String jaasConfiguration) {
        this.configuration = jaasConfiguration;
    }


    public boolean isDiscoverLoginConfig() {
        return discoverLoginConfig;
    }

    /**
     * Controls whether or not the plugin will authorize messages
     * sent to the broker by a producer
     * @param authorizeSend
     */
    public void setAuthorizeSend(boolean authorizeSend) {
        this.authorizeSend = authorizeSend;
    }

    public boolean isAuthorizeSend() {
        return authorizeSend;
    }

    /**
     * Enables or disables the auto-discovery of the login.config file for JAAS to initialize itself.
     * This flag is enabled by default such that if the <b>java.security.auth.login.config</b> system property
     * is not defined then it is set to the location of the <b>login.config</b> file on the classpath.
     */
    public void setDiscoverLoginConfig(boolean discoverLoginConfig) {
        this.discoverLoginConfig = discoverLoginConfig;
    }

    // Implementation methods
    // -------------------------------------------------------------------------
    protected void initialiseJaas() {
        if (discoverLoginConfig) {
            LOG.info("Searching for login.config");
            String path = System.getProperty("java.security.auth.login.config");
            if (path == null) {
                //URL resource = Thread.currentThread().getContextClassLoader().getResource("login.config");
                URL resource = null;
                if (resource == null) {
                    resource = getClass().getClassLoader().getResource("login.config");
                }
                if (resource != null) {
                    path = resource.getFile();
                    System.setProperty("java.security.auth.login.config", path);
                }
            }
            LOG.info("login.config found at {}", path);
        }
    }
}
