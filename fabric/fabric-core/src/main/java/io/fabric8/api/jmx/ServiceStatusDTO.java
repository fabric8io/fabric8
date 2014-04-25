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
package io.fabric8.api.jmx;

/**
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
