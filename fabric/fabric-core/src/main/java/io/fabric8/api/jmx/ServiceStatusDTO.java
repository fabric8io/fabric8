package io.fabric8.api.jmx;

/**
 * @author Stan Lewis
 */
public class ServiceStatusDTO {

    private boolean clientValid;
    private boolean clientConnected;
    private String clientConnectionError;
    private boolean provisionComplete;
    private boolean managed;

    public boolean isClientValid() {
        return clientValid;
    }

    public void setClientValid(boolean clientValid) {
        this.clientValid = clientValid;
    }

    public boolean isClientConnected() {
        return clientConnected;
    }

    public void setClientConnected(boolean clientConnected) {
        this.clientConnected = clientConnected;
    }

    public String getClientConnectionError() {
        return clientConnectionError;
    }

    public void setClientConnectionError(String clientConnectionError) {
        this.clientConnectionError = clientConnectionError;
    }

    public boolean isProvisionComplete() {
        return provisionComplete;
    }

    public void setProvisionComplete(boolean provisionComplete) {
        this.provisionComplete = provisionComplete;
    }

    public boolean isManaged() {
        return managed;
    }

    public void setManaged(boolean managed) {
        this.managed = managed;
    }
}
