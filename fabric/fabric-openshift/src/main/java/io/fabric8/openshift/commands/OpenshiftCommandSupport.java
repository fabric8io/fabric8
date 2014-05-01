/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.openshift.commands;

import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.OpenShiftConnectionFactory;
import io.fabric8.openshift.commands.support.OpenshiftConnectionListener;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;

public abstract class OpenshiftCommandSupport extends OsgiCommandSupport {

    @Option(name = "--server-url", required = true, description = "The url to the openshift server.")
    String serverUrl;

    @Option(name = "--login", required = true, description = "The login name to use.")
    String login;

    @Option(name = "--password", required = true, description = "The password to use.")
    String password;

    private final OpenShiftConnectionFactory connectionFactory = new OpenShiftConnectionFactory();

    OpenshiftConnectionListener connectionListener;

    /**
     * Returns an existing connection or attempts to create one.
     */
    IOpenShiftConnection getOrCreateConnection() {
        IOpenShiftConnection connection = null;
        if (connectionListener != null) {
            connection = connectionListener.getConnection();
        }
        if (connection == null) {
            connection = createConnection();
        }
        return connection;
    }

    /**
     * Creates a connection based on the specified options.
     */
    IOpenShiftConnection createConnection() {
        return connectionFactory.getConnection("fabric", login, password, serverUrl);
    }

    public OpenshiftConnectionListener getConnectionListener() {
        return connectionListener;
    }

    public void setConnectionListener(OpenshiftConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }
}
