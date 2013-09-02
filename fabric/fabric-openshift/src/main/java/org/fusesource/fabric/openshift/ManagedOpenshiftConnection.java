/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.fusesource.fabric.openshift;

import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.OpenShiftConnectionFactory;
import com.openshift.internal.client.utils.Assert;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.fusesource.fabric.utils.Strings;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.Map;

@Component(name = "org.fusesource.fabric.openshift",
        description = "Fabric Openshift Connection",
        immediate = true)
public class ManagedOpenshiftConnection  {

    private static final String SERVER_URL = "serverUrl";
    private static final String LOGIN = "login";
    private static final String PASSWORD = "password";
    private static final String FABRIC_CLIENT_ID = "fabric";

    private final OpenShiftConnectionFactory connectionFactory = new OpenShiftConnectionFactory();

    private ServiceRegistration<IOpenShiftConnection> registration;
    private IOpenShiftConnection connection;


    @Activate
    public void init(BundleContext bundleContext, Map<String, String> properties) {
        if (registration != null) {
            registration.unregister();
        }

        if (isConfigurationValid(properties)) {
            String serverUrl = properties.get(SERVER_URL);
            String login = properties.get(LOGIN);
            String password = properties.get(PASSWORD);

            if (serverUrl != null && login != null && password != null) {
                connection = connectionFactory.getConnection(FABRIC_CLIENT_ID, login, password, serverUrl);
                registration = bundleContext.registerService(IOpenShiftConnection.class, connection, null);
            }
        }
    }

    private boolean isConfigurationValid(Map<String, String> properties) {
        return properties != null
                && properties.containsKey(SERVER_URL) && Strings.isNotBlank(properties.get(SERVER_URL))
                && properties.containsKey(LOGIN) && Strings.isNotBlank(properties.get(LOGIN))
                && properties.containsKey(PASSWORD) && Strings.isNotBlank(properties.get(PASSWORD));
    }

    @Deactivate
    public void destroy() {
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
    }
}
