package io.fabric8.openshift.commands.support;

import com.openshift.client.IOpenShiftConnection;

public class OpenshiftConnectionListener {

    private IOpenShiftConnection connection;

    public IOpenShiftConnection getConnection() {
        return connection;
    }

    public void bindConnection(IOpenShiftConnection connection) {
        this.connection = connection;
    }

    public void unbindConnection(IOpenShiftConnection connection) {
        this.connection = null;
    }
}
