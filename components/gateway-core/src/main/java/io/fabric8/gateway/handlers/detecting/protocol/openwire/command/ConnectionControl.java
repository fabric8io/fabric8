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
 * Used to start and stop transports as well as terminating clients.
 * 
 * @openwire:marshaller code="18"
 * @version $Revision: 1.1 $
 */
public class ConnectionControl extends BaseCommand {
    public static final UTF8Buffer EMPTY = new UTF8Buffer("");

    public static final byte DATA_STRUCTURE_TYPE = CommandTypes.CONNECTION_CONTROL;
    protected boolean suspend;
    protected boolean resume;
    protected boolean close;
    protected boolean exit;
    protected boolean faultTolerant;
    protected UTF8Buffer connectedBrokers = EMPTY;
    protected UTF8Buffer reconnectTo = EMPTY;
    protected boolean rebalanceConnection;

    public byte getDataStructureType() {
        return DATA_STRUCTURE_TYPE;
    }

    /**
     * @openwire:property version=1
     * @return Returns the close.
     */
    public boolean isClose() {
        return close;
    }

    /**
     * @param close The close to set.
     */
    public void setClose(boolean close) {
        this.close = close;
    }

    /**
     * @openwire:property version=1
     * @return Returns the exit.
     */
    public boolean isExit() {
        return exit;
    }

    /**
     * @param exit The exit to set.
     */
    public void setExit(boolean exit) {
        this.exit = exit;
    }

    /**
     * @openwire:property version=1
     * @return Returns the faultTolerant.
     */
    public boolean isFaultTolerant() {
        return faultTolerant;
    }

    /**
     * @param faultTolerant The faultTolerant to set.
     */
    public void setFaultTolerant(boolean faultTolerant) {
        this.faultTolerant = faultTolerant;
    }

    /**
     * @openwire:property version=1
     * @return Returns the resume.
     */
    public boolean isResume() {
        return resume;
    }

    /**
     * @param resume The resume to set.
     */
    public void setResume(boolean resume) {
        this.resume = resume;
    }

    /**
     * @openwire:property version=1
     * @return Returns the suspend.
     */
    public boolean isSuspend() {
        return suspend;
    }

    /**
     * @param suspend The suspend to set.
     */
    public void setSuspend(boolean suspend) {
        this.suspend = suspend;
    }

    /**
     * @openwire:property version=6 cache=false
     * @return connected brokers.
     */
    public UTF8Buffer getConnectedBrokers() {
        return this.connectedBrokers;
    }

    /**
     * @param connectedBrokers the connectedBrokers to set
     */
    public void setConnectedBrokers(UTF8Buffer connectedBrokers) {
        this.connectedBrokers = connectedBrokers;
    }

    /**
     *  @openwire:property version=6 cache=false
     * @return the reconnectTo
     */
    public UTF8Buffer getReconnectTo() {
        return this.reconnectTo;
    }

    /**
     * @param reconnectTo the reconnectTo to set
     */
    public void setReconnectTo(UTF8Buffer reconnectTo) {
        this.reconnectTo = reconnectTo;
    }

    /**
     * @return the rebalanceConnection
     *  @openwire:property version=6 cache=false
     */
    public boolean isRebalanceConnection() {
        return this.rebalanceConnection;
    }

    /**
     * @param rebalanceConnection the rebalanceConnection to set
     */
    public void setRebalanceConnection(boolean rebalanceConnection) {
        this.rebalanceConnection = rebalanceConnection;
    }
}
