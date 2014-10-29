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

/**
 * @openwire:marshaller code="91"
 * @version $Revision: 1.12 $
 */
public class NetworkBridgeFilter implements DataStructure, BooleanExpression {

    public static final byte DATA_STRUCTURE_TYPE = CommandTypes.NETWORK_BRIDGE_FILTER;

    private BrokerId networkBrokerId;
    private int networkTTL;

    public NetworkBridgeFilter() {
    }

    public NetworkBridgeFilter(BrokerId remoteBrokerPath, int networkTTL) {
        this.networkBrokerId = remoteBrokerPath;
        this.networkTTL = networkTTL;
    }

    public byte getDataStructureType() {
        return DATA_STRUCTURE_TYPE;
    }

    public boolean isMarshallAware() {
        return false;
    }

//    public boolean matches(Filterable filterable) throws FilterException {
//        return matchesForwardingFilter((Message)filterable);
//    }
//
//    public Object evaluate(Filterable filterable) throws FilterException {
//        return matches(filterable) ? Boolean.TRUE : Boolean.FALSE;
//    }

    protected boolean matchesForwardingFilter(Message message) {

        if (contains(message.getBrokerPath(), networkBrokerId)) {
            return false;
        }

        int hops = message.getBrokerPath() == null ? 0 : message.getBrokerPath().length;

        if (hops >= networkTTL) {
            return false;
        }

        // Don't propagate advisory messages about network subscriptions
        if (message.isAdvisory() && message.getDataStructure() != null && message.getDataStructure().getDataStructureType() == CommandTypes.CONSUMER_INFO) {
            ConsumerInfo info = (ConsumerInfo)message.getDataStructure();
            hops = info.getBrokerPath() == null ? 0 : info.getBrokerPath().length;
            if (hops >= networkTTL) {
                return false;
            }
        }
        return true;
    }

    public static boolean contains(BrokerId[] brokerPath, BrokerId brokerId) {
        if (brokerPath != null && brokerId != null) {
            for (int i = 0; i < brokerPath.length; i++) {
                if (brokerId.equals(brokerPath[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @openwire:property version=1
     */
    public int getNetworkTTL() {
        return networkTTL;
    }

    public void setNetworkTTL(int networkTTL) {
        this.networkTTL = networkTTL;
    }

    /**
     * @openwire:property version=1 cache=true
     */
    public BrokerId getNetworkBrokerId() {
        return networkBrokerId;
    }

    public void setNetworkBrokerId(BrokerId remoteBrokerPath) {
        this.networkBrokerId = remoteBrokerPath;
    }

}
