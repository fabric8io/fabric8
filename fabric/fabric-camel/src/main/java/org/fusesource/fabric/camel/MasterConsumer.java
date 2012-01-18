/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
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
        state.setId(endpoint.getId());
        cluster.join(state);
        cluster.add(new ChangeListener() {
            @Override
            public void connected() {
                System.out.println("=============== cluster connected!");
            }

            @Override
            public void changed() {
                System.out.println("=============== cluster changed!");
                if (cluster.isMaster()) {
                    System.out.println("=============== is master!");
                    onLockOwned();
                } else {
                    System.out.println("=============== not master!");
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
