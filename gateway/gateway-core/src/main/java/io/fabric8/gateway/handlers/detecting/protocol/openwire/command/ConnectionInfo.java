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
package io.fabric8.gateway.handlers.detecting.protocol.openwire.command;

import org.fusesource.hawtbuf.UTF8Buffer;

/**
 * 
 * @openwire:marshaller code="3"
 * @version $Revision: 1.11 $
 */
public class ConnectionInfo extends BaseCommand {

    public static final byte DATA_STRUCTURE_TYPE = CommandTypes.CONNECTION_INFO;

    protected ConnectionId connectionId;
    protected UTF8Buffer clientId;
    protected UTF8Buffer clientIp;
    protected UTF8Buffer userName;
    protected UTF8Buffer password;
    protected BrokerId[] brokerPath;
    protected boolean brokerMasterConnector;
    protected boolean manageable;
    protected boolean clientMaster = true;
    protected boolean faultTolerant = false;
    protected transient Object transportContext;
    private boolean failoverReconnect;

    public ConnectionInfo() {
    }

    public ConnectionInfo(ConnectionId connectionId) {
        this.connectionId = connectionId;
    }

    public byte getDataStructureType() {
        return DATA_STRUCTURE_TYPE;
    }

    public ConnectionInfo copy() {
        ConnectionInfo copy = new ConnectionInfo();
        copy(copy);
        return copy;
    }

    private void copy(ConnectionInfo copy) {
        super.copy(copy);
        copy.connectionId = connectionId;
        copy.clientId = clientId;
        copy.userName = userName;
        copy.password = password;
        copy.brokerPath = brokerPath;
        copy.brokerMasterConnector = brokerMasterConnector;
        copy.manageable = manageable;
        copy.clientMaster = clientMaster;
        copy.transportContext = transportContext;
        copy.clientIp = clientIp;
    }

    /**
     * @openwire:property version=1 cache=true
     */
    public ConnectionId getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(ConnectionId connectionId) {
        this.connectionId = connectionId;
    }

    /**
     * @openwire:property version=1
     */
    public UTF8Buffer getClientId() {
        return clientId;
    }

    public void setClientId(UTF8Buffer clientId) {
        this.clientId = clientId;
    }

    public RemoveInfo createRemoveCommand() {
        RemoveInfo command = new RemoveInfo(getConnectionId());
        command.setResponseRequired(isResponseRequired());
        return command;
    }

    /**
     * @openwire:property version=1
     */
    public UTF8Buffer getPassword() {
        return password;
    }

    public void setPassword(UTF8Buffer password) {
        this.password = password;
    }

    /**
     * @openwire:property version=1
     */
    public UTF8Buffer getUserName() {
        return userName;
    }

    public void setUserName(UTF8Buffer userName) {
        this.userName = userName;
    }

    /**
     * The route of brokers the command has moved through.
     * 
     * @openwire:property version=1 cache=true
     */
    public BrokerId[] getBrokerPath() {
        return brokerPath;
    }

    public void setBrokerPath(BrokerId[] brokerPath) {
        this.brokerPath = brokerPath;
    }

    /**
     * @openwire:property version=1
     */
    public boolean isBrokerMasterConnector() {
        return brokerMasterConnector;
    }

    /**
     * @param slaveBroker The brokerMasterConnector to set.
     */
    public void setBrokerMasterConnector(boolean slaveBroker) {
        this.brokerMasterConnector = slaveBroker;
    }

    /**
     * @openwire:property version=1
     */
    public boolean isManageable() {
        return manageable;
    }

    /**
     * @param manageable The manageable to set.
     */
    public void setManageable(boolean manageable) {
        this.manageable = manageable;
    }

    /**
     * Transports may wish to associate additional data with the connection. For
     * example, an SSL transport may use this field to attach the client
     * certificates used when the conection was established.
     * 
     * @return the transport context.
     */
    public Object getTransportContext() {
        return transportContext;
    }

    /**
     * Transports may wish to associate additional data with the connection. For
     * example, an SSL transport may use this field to attach the client
     * certificates used when the conection was established.
     * 
     * @param transportContext value used to set the transport context
     */
    public void setTransportContext(Object transportContext) {
        this.transportContext = transportContext;
    }

    /**
     * @openwire:property version=2
     * @return the clientMaster
     */
    public boolean isClientMaster() {
        return this.clientMaster;
    }

    /**
     * @param clientMaster the clientMaster to set
     */
    public void setClientMaster(boolean clientMaster) {
        this.clientMaster = clientMaster;
    }

    /**
     * @openwire:property version=6 cache=false
     * @return the faultTolerant
     */
    public boolean isFaultTolerant() {
        return this.faultTolerant;
    }

    /**
     * @param faultTolerant the faultTolerant to set
     */
    public void setFaultTolerant(boolean faultTolerant) {
        this.faultTolerant = faultTolerant;
    }

    /**
     * @openwire:property version=6 cache=false
     * @return failoverReconnect true if this is a reconnect
     */
    public boolean isFailoverReconnect() {
        return this.failoverReconnect;
    }

    public void setFailoverReconnect(boolean failoverReconnect) {
        this.failoverReconnect = failoverReconnect;
    }

    /**
     * @openwire:property version=8
     */
    public UTF8Buffer getClientIp() {
        return clientIp;
    }

    public void setClientIp(UTF8Buffer clientIp) {
        this.clientIp = clientIp;
    }
}
