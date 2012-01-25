/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.cxf;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.AbstractConduitSelector;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import java.io.IOException;
import java.util.logging.Logger;

public class LoadBalanceTargetSelector extends AbstractConduitSelector {
    protected volatile Conduit selectedConduit;
    protected LoadBalanceStrategy loadBalanceStrategy;

    private static final Logger LOG =
            LogUtils.getL7dLogger(FailOverTargetSelector.class);

    public LoadBalanceTargetSelector() {
        super(null);
    }

    public LoadBalanceTargetSelector(Conduit c) {
        super(c);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    public void prepare(Message message) {
        // setup the conduit first
        getSelectedConduit(message);
    }

    public Conduit selectConduit(Message message) {
        return getSelectedConduit(message);
    }

    public void complete(Exchange exchange) {
        super.complete(exchange);
        // here we just reset the selectConduit for next around connection
        selectedConduit = null;
    }

    protected synchronized Conduit getSelectedConduit(Message message) {
        if (selectedConduit == null) {
            selectedConduit = getNextConduit(message);
        }
        return selectedConduit;
    }

    protected Conduit getNextConduit(Message message) {
        Conduit answer = null;
        Exchange exchange = message.getExchange();
        EndpointInfo ei = endpoint.getEndpointInfo();
        //TODO check the address from  Message.ENDPOINT_ADDRESS
        String address = loadBalanceStrategy.getNextAlternateAddress();
        try {
            ConduitInitiatorManager conduitInitiatorMgr = exchange.getBus()
                    .getExtension(ConduitInitiatorManager.class);
            if (conduitInitiatorMgr != null) {
                ConduitInitiator conduitInitiator =
                        conduitInitiatorMgr.getConduitInitiatorForUri(address);
                if (conduitInitiator != null) {
                    EndpointReferenceType epr = new EndpointReferenceType();
                    AttributedURIType ad = new AttributedURIType();
                    ad.setValue(address);
                    epr.setAddress(ad);
                    answer = conduitInitiator.getConduit(ei, epr);

                    MessageObserver observer =
                            exchange.get(MessageObserver.class);
                    if (observer != null) {
                        answer.setMessageObserver(observer);
                    } else {
                        getLogger().warning("MessageObserver not found");
                    }
                } else {
                    getLogger().warning("ConduitInitiator not found: "
                            + ei.getAddress());
                }
            } else {
                getLogger().warning("ConduitInitiatorManager not found");
            }
        } catch (IOException ex) {
            throw new Fault(ex);
        }
        return answer;
    }

    public LoadBalanceStrategy getLoadBalanceStrategy() {
        return loadBalanceStrategy;
    }

    public void setLoadBalanceStrategy(LoadBalanceStrategy loadBalanceStrategy) {
        this.loadBalanceStrategy = loadBalanceStrategy;
    }
}
