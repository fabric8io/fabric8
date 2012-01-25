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
package org.fusesource.fabric.camel;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.SuspendableService;
import org.apache.camel.impl.DefaultConsumer;
import org.apache.camel.util.ServiceHelper;
import org.fusesource.fabric.groups.ChangeListener;
import org.fusesource.fabric.groups.ClusteredSingleton;
import org.fusesource.fabric.groups.TextNodeState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A consumer which is only really active while it holds the master lock
 */
public class MasterConsumer extends DefaultConsumer {
    private static final transient Logger LOG = LoggerFactory.getLogger(MasterConsumer.class);

    private final MasterEndpoint endpoint;
    private final Processor processor;
    private Consumer delegate;
    private SuspendableService delegateService;

    public MasterConsumer(MasterEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;
        this.processor = processor;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        tryAcquireLock();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        try {
            stopConsumer();
        } finally {
            releaseLock();
        }
    }

    protected void stopConsumer() throws Exception {
        ServiceHelper.stopService(delegate);
        delegate = null;
        delegateService = null;
    }

    @Override
    protected void doResume() throws Exception {
        if (delegateService != null) {
            delegateService.resume();
        }
        super.doResume();
    }

    @Override
    protected void doSuspend() throws Exception {
        if (delegateService != null) {
            delegateService.suspend();
        }
        super.doSuspend();
    }

    protected void onLockOwned() {
        if (delegate == null) {
            try {
                delegate = endpoint.getChildEndpoint().createConsumer(processor);
                delegateService = null;
                if (delegate instanceof SuspendableService) {
                    delegateService = (SuspendableService) delegate;
                }
                ServiceHelper.startService(delegate);
            } catch (Exception e) {
                LOG.error("Failed to start master consumer for: " + endpoint + ". Reason: " + e, e);
            }
        }
    }

    protected void tryAcquireLock() {
        final ClusteredSingleton<TextNodeState> cluster = getCluster();
        TextNodeState state = new TextNodeState();
        String singletonId = endpoint.getSingletonId();
        LOG.debug("Attempting to become master for endpoint: " + endpoint + " in " + endpoint.getCamelContext() + " with singletonID: " + singletonId);
        state.setId(singletonId);
        cluster.join(state);
        cluster.add(new ChangeListener() {
            @Override
            public void connected() {
            }

            @Override
            public void changed() {
                if (cluster.connected()) {
                    if (cluster.isMaster()) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Master/Standby endpoint is Master for:  " + endpoint + " in " + endpoint.getCamelContext());
                        }
                        onLockOwned();
                    } else {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Master/Standby endpoint is Standby for: " + endpoint + " in " + endpoint.getCamelContext());
                        }
                    }
                }
            }

            @Override
            public void disconnected() {
                try {
                    stopConsumer();
                } catch (Exception e) {
                    LOG.error("Failed to stop master consumer for: " + endpoint + ". Reason: " + e, e);
                }
            }
        });
    }

    protected ClusteredSingleton<TextNodeState> getCluster() {
        return endpoint.getCluster();
    }

    protected void releaseLock() {
        getCluster().leave();
    }


}
