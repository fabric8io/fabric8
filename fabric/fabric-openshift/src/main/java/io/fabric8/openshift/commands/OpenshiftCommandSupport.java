package io.fabric8.openshift.commands;

import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.OpenShiftConnectionFactory;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import io.fabric8.openshift.commands.support.OpenshiftConnectionListener;

public abstract class OpenshiftCommandSupport extends OsgiCommandSupport {

    @Option(name = "--server-url", required = false, description = "The url to the openshift server.")
    String serverUrl;

    @Option(name = "--login", required = false, description = "The login name to use.")
    String login;

    @Option(name = "--password", required = false, description = "The password to use.")
    String password;

    final OpenShiftConnectionFactory connectionFactory = new OpenShiftConnectionFactory();

    OpenshiftConnectionListener connectionListener;


    /**
     * Returns an existing connection or attempts to create one.
     * @return
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
     * @return
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
