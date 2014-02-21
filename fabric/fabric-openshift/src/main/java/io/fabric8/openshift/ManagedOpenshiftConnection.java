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

import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.OpenShiftConnectionFactory;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.Configurer;
import io.fabric8.utils.Strings;
import org.apache.felix.scr.annotations.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.Map;

@Component(name = "io.fabric8.openshift",
        description = "Fabric Openshift Connection",
        policy = ConfigurationPolicy.REQUIRE,
        immediate = true, metatype = true)
public class ManagedOpenshiftConnection extends AbstractComponent {

    private static final String SERVER_URL = "serverUrl";
    private static final String LOGIN = "login";
    private static final String PASSWORD = "password";
    private static final String FABRIC_CLIENT_ID = "fabric";

    private final OpenShiftConnectionFactory connectionFactory = new OpenShiftConnectionFactory();

    private ServiceRegistration<IOpenShiftConnection> registration;
    private IOpenShiftConnection connection;

    @Reference
    private Configurer configurer;
    @Property(name = "serverUrl", label = "Openshift Server URL", description = "The URL to the Openshift server", value = "openshift.redhat.com")
    String serverUrl;
    @Property(name = "login", label = "Login", description = "The openshift account login")
    String login;
    @Property(name = "password", label = "Password", description = "The openshift account password")
    String password;
    @Property(name="default.cartridge.url", label = "Default Cartridge URL", value = "${default.cartridge.url}")
    private String defaultCartridgeUrl;

    @Activate
    public void activate(BundleContext bundleContext, Map<String, String> properties) throws Exception {
        if (isConfigurationValid(properties)) {
            configurer.configure(properties, this);

            if (serverUrl != null && login != null && password != null) {
                connection = connectionFactory.getConnection(FABRIC_CLIENT_ID, login, password, serverUrl);
                registration = bundleContext.registerService(IOpenShiftConnection.class, connection, null);
            }
        }
    }

    @Deactivate
    public void deactivate() {
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
    }

    private boolean isConfigurationValid(Map<String, String> properties) {
        return properties != null
                && properties.containsKey(SERVER_URL) && Strings.isNotBlank(properties.get(SERVER_URL))
                && properties.containsKey(LOGIN) && Strings.isNotBlank(properties.get(LOGIN))
                && properties.containsKey(PASSWORD) && Strings.isNotBlank(properties.get(PASSWORD));
    }
}
