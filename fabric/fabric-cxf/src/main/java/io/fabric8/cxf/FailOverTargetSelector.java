/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.cxf;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Retryable;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.transport.Conduit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


public class FailOverTargetSelector extends LoadBalanceTargetSelector {

    private static final Logger LOG =
            LogUtils.getL7dLogger(FailOverTargetSelector.class);

    protected Map<InvocationKey, InvocationContext> inProgress;

    protected List<Class> exceptionClasses;

    public FailOverTargetSelector(List<Class> exceptions) {
        this(null, exceptions);
    }

    public FailOverTargetSelector(Conduit c, List<Class> exceptions) {
        super(c);
        inProgress = new ConcurrentHashMap<InvocationKey, InvocationContext>();
        if (exceptions != null) {
            exceptionClasses = exceptions;
        } else {
            exceptionClasses = new ArrayList<Class>();
        }
        // FailOver the IOException by default
        if (!exceptionClasses.contains(IOException.class)) {
            exceptionClasses.add(IOException.class);
        }

    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    public synchronized void prepare(Message message) {
        Exchange exchange = message.getExchange();
        InvocationKey key = new InvocationKey(exchange);
        if (!inProgress.containsKey(key)) {
            Endpoint endpoint = exchange.get(Endpoint.class);
            BindingOperationInfo bindingOperationInfo =
                exchange.getBindingOperationInfo();
            Object[] params = message.getContent(List.class).toArray();
            Map<String, Object> context =
                CastUtils.cast((Map) message.get(Message.INVOCATION_CONTEXT));
            InvocationContext invocation =
                new InvocationContext(endpoint,
                                      bindingOperationInfo,
                                      params,
                                      context);
            inProgress.put(key, invocation);
        }
    }

    public void complete(Exchange exchange) {
        InvocationKey key = new InvocationKey(exchange);
        InvocationContext invocation = null;
        invocation = inProgress.get(key);

        boolean failOver = false;
        if (requiresFailOver(exchange)) {
            Endpoint failOverTarget = getFailOverTarget(exchange, invocation);
            if (failOverTarget != null) {
                setEndpoint(failOverTarget);
                selectedConduit.close();
                selectedConduit = null;
                Exception prevExchangeFault =
                    (Exception)exchange.remove(Exception.class.getName());
                Message outMessage = exchange.getOutMessage();
                Exception prevMessageFault =
                    outMessage.getContent(Exception.class);
                outMessage.setContent(Exception.class, null);
                overrideAddressProperty(invocation.getContext());
                Retryable retry = exchange.get(Retryable.class);
                exchange.clear();
                if (retry != null) {
                    try {
                        failOver = true;
                        retry.invoke(invocation.getBindingOperationInfo(),
                                     invocation.getParams(),
                                     invocation.getContext(),
                                     exchange);
                    } catch (Exception e) {
                        if (exchange.get(Exception.class) != null) {
                            exchange.put(Exception.class, prevExchangeFault);
                        }
                        if (outMessage.getContent(Exception.class) != null) {
                            outMessage.setContent(Exception.class,
                                                  prevMessageFault);
                        }
                    }
                }
            } else {
                setEndpoint(invocation.retrieveOriginalEndpoint(endpoint));
            }
        }
        if (!failOver) {
            getLogger().info("FailOver is not required.");

            inProgress.remove(key);

            super.complete(exchange);
        }
    }

    // Now we just fail over with the IOException
    protected boolean requiresFailOver(Exchange exchange) {
        Message outMessage = exchange.getOutMessage();
        Exception ex = outMessage.get(Exception.class) != null
                       ? outMessage.get(Exception.class)
                       : exchange.get(Exception.class);
        getLogger().log(Level.FINE,
                        "Check last invoke failed " + ex);
        Throwable curr = ex;
        boolean failOver = false;
        while (curr != null && !failOver) {
            failOver = checkExceptionClasses(curr);
            curr = curr.getCause();
        }
        getLogger().log(Level.INFO, "Check failure in transport " + ex + ", failOver is " + failOver);
        return failOver;
    }

    protected boolean checkExceptionClasses(Throwable current) {
        for (Class<?> exceptionClass : exceptionClasses) {
            if (exceptionClass.isInstance(current)) {
                return true;
            }
        }
        return false;
    }

    protected Endpoint getFailOverTarget(Exchange exchange,
                                       InvocationContext invocation) {

        Endpoint failOverTarget = null;
        if (invocation.getAlternateAddresses() == null) {
            invocation.setAlternateAddresses(getLoadBalanceStrategy().getAlternateAddressList());
            // Remove the first one as it is used
            invocation.getAlternateAddresses().remove(0);
        }
        String alternateAddress = null;
        if (invocation.getAlternateAddresses().size() > 0) {
            alternateAddress = invocation.getAlternateAddresses().remove(0);
        }
        if (alternateAddress != null) {
            // reuse the endpointInfo
            failOverTarget = getEndpoint();
            failOverTarget.getEndpointInfo().setAddress(alternateAddress);
        }
        return failOverTarget;
    }

    protected void overrideAddressProperty(Map<String, Object> context) {
        Map<String, Object> requestContext =
            CastUtils.cast((Map)context.get(Client.REQUEST_CONTEXT));
        if (requestContext != null) {
            requestContext.put(Message.ENDPOINT_ADDRESS,
                               getEndpoint().getEndpointInfo().getAddress());
            requestContext.put("javax.xml.ws.service.endpoint.address",
                               getEndpoint().getEndpointInfo().getAddress());
            // We should not replace the Address this time as the address is already override
            requestContext.put(LoadBalanceTargetSelector.OVERRIDE_ADDRESS, "false");
        }
    }

    protected static class InvocationKey {
        private Exchange exchange;

        InvocationKey(Exchange ex) {
            exchange = ex;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this.exchange);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof InvocationKey
                   && exchange == ((InvocationKey)o).exchange;
        }
    }


    /**
     * Records the context of an invocation.
     */
    protected class InvocationContext {
        private Endpoint originalEndpoint;
        private String originalAddress;
        private BindingOperationInfo bindingOperationInfo;
        private Object[] params;
        private Map<String, Object> context;
        private List<String> alternateAddresses;

        InvocationContext(Endpoint endpoint,
                          BindingOperationInfo boi,
                          Object[] prms,
                          Map<String, Object> ctx) {
            originalEndpoint = endpoint;
            originalAddress = endpoint.getEndpointInfo().getAddress();
            bindingOperationInfo = boi;
            params = prms;
            context = ctx;
        }

        Endpoint retrieveOriginalEndpoint(Endpoint endpoint) {
            if (endpoint != originalEndpoint) {
                getLogger().log(Level.INFO,
                                "Revert to original target " +
                                endpoint.getEndpointInfo().getName());
            }
            if (!endpoint.getEndpointInfo().getAddress().equals(originalAddress)) {
                endpoint.getEndpointInfo().setAddress(originalAddress);
                getLogger().log(Level.INFO,
                                "Revert to original address ",
                                endpoint.getEndpointInfo().getAddress());
            }
            return originalEndpoint;
        }

        BindingOperationInfo getBindingOperationInfo() {
            return bindingOperationInfo;
        }

        Object[] getParams() {
            return params;
        }

        Map<String, Object> getContext() {
            return context;
        }

        void setAlternateAddresses(List<String> alternates) {
            alternateAddresses = alternates;
        }

        List<String> getAlternateAddresses() {
            return alternateAddresses;
        }
    }





}
