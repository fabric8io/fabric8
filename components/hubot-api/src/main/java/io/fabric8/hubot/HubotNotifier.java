/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.hubot;

import io.fabric8.annotations.Protocol;
import io.fabric8.annotations.ServiceName;
import io.fabric8.utils.Strings;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.List;

import static io.fabric8.utils.cxf.WebClients.configureUserAndPassword;
import static io.fabric8.utils.cxf.WebClients.createProviders;
import static io.fabric8.utils.cxf.WebClients.disableSslChecks;

/**
 * A service for notifying a message to the <a href="http://hubot.github.com/">Hubot chat bot</a>
 */
public class HubotNotifier {
    public static final String HUBOT_SERVICE_NAME = "hubot";
    public static final String DEFAULT_ROOM_EXPRESSION = "#fabric8_${namespace}";

    private static final transient Logger LOG = LoggerFactory.getLogger(HubotNotifier.class);

    @Inject
    @Protocol("http")
    @ServiceName(HUBOT_SERVICE_NAME)
    private Instance<String> hubotUrlHolder;

    private final String username;
    private final String password;
    private final String roomExpression;
    private HubotRestApi api;
    private LoggingHubotRestApi logMessages = new LoggingHubotRestApi();
    private String hubotUrl;
    private boolean initalised;

    @Inject
    public HubotNotifier(@ConfigProperty(name = "HUBOT_USERNAME") String username,
                         @ConfigProperty(name = "HUBOT_PASSWORD") String password,
                         @ConfigProperty(name = "HUBOT_BUILD_ROOM", defaultValue = DEFAULT_ROOM_EXPRESSION) String roomExpression) {

        this.username = username;
        this.password = password;
        this.roomExpression = roomExpression;
    }

    public HubotNotifier(String hubotUrl, String username, String password, String roomExpression) {
        this.hubotUrl = hubotUrl;
        this.username = username;
        this.password = password;
        this.roomExpression = roomExpression;
    }

    public String getHubotUrl() {
        if (!initalised) {
            initalised = true;
            if (hubotUrl == null && hubotUrlHolder != null) {
                hubotUrl = hubotUrlHolder.get();
            }
            if (Strings.isNotBlank(hubotUrl)) {
                LOG.info("Starting HubotNotifier using address: " + hubotUrl);
            } else {
                LOG.warn("No kubernetes service found for " + HUBOT_SERVICE_NAME + " so chat messages just going to logs instead");
            }
        }
        return hubotUrl;
    }

    /**
     * Sends a message to a given room in the chat bot
     *
     * @param room the name of the room usually using IRC style starting with #
     * @param message the notification message to send which can include links or refer to people via @somenick
     */
    public void notifyRoom(String room, String message) {
        logMessages.notify(room, message);
        try {
            getHubotRestApi().notify(room, message);
        } catch (Exception e) {
            LOG.error("Failed to notify hubot room: " + room + " with message: " + message + ". Reason: " + e, e);
        }
    }

    /**
     * Notifies the room for the given namespace and build config name with the given message.
     *
     * By default this maps to a single logical room; which could be global or typically parameterised with
     * the namespace and/or buildConfig.
     *
     * @param namespace the kubernetes namespace for the build
     * @param buildConfig the name of the build configuration
     * @param message the notification message
     */
    public void notifyBuild(String namespace, String buildConfig, String message) {
        String room = roomExpression.replace("${namespace}", namespace).replace("${buildConfig}", buildConfig);
        notifyRoom(room, message);
    }


    protected HubotRestApi getHubotRestApi() {
        if (api == null) {
            String url = getHubotUrl();
            if (Strings.isNotBlank(url)) {
                api = createWebClient(HubotRestApi.class, url);
            } else {
                api = new NoopHubotRestApi();
            }
        }
        return api;
    }

    /**
     * Creates a JAXRS web client for the given JAXRS client
     */
    protected <T> T createWebClient(Class<T> clientType, String hubotUrl) {
        List<Object> providers = createProviders();
        WebClient webClient = WebClient.create(hubotUrl, providers);
        disableSslChecks(webClient);
        configureUserAndPassword(webClient, username, password);
        return JAXRSClientFactory.fromClient(webClient, clientType);
    }


}
