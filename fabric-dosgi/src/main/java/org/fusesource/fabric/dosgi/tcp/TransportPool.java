/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.dosgi.tcp;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.fusesource.fabric.dosgi.io.ProtocolCodec;
import org.fusesource.fabric.dosgi.io.Service;
import org.fusesource.fabric.dosgi.io.Transport;
import org.fusesource.fabric.dosgi.io.TransportListener;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TransportPool implements Service {

    protected static final Logger LOGGER = LoggerFactory.getLogger(TransportPool.class);

    public static int DEFAULT_POOL_SIZE = 2;

    protected final String uri;
    protected final DispatchQueue queue;
    protected final List<Object> pending = new LinkedList<Object>();
    protected final Map<Transport, State> transports = new HashMap<Transport, State>();
    protected AtomicBoolean running = new AtomicBoolean(false);
    protected int poolSize;

    enum State {
        Creating,
        Writing,
        Idle
    }

    public TransportPool(String uri, DispatchQueue queue) {
        this(uri, queue, DEFAULT_POOL_SIZE);
    }

    public TransportPool(String uri, DispatchQueue queue, int poolSize) {
        this.uri = uri;
        this.queue = queue;
        this.poolSize = poolSize;
    }

    protected abstract Transport createTransport(String uri) throws Exception;

    protected abstract ProtocolCodec createCodec();

    protected abstract void onCommand(Object command);

    public void offer(final Object data) {
        if (!running.get()) {
            throw new IllegalStateException("Transport pool stopped");
        }
        queue.execute(new Runnable() {
            public void run() {
                Transport idleTransport = getIdleTransport();
                if (idleTransport != null) {
                    idleTransport.offer(data);
                } else {
                    pending.add(data);
                }
            }
        });
    }

    protected Transport getIdleTransport() {
        for (Map.Entry<Transport, State> entry : transports.entrySet()) {
            if (entry.getValue() == State.Idle) {
                return entry.getKey();
            }
        }
        if (transports.size() < poolSize) {
            try {
                startNewTransport();
            } catch (Exception e) {
                LOGGER.info("Unable to start new transport", e);
            }
        }
        return null;
    }

    public void start() throws Exception {
        start(null);
    }

    public void start(Runnable onComplete) throws Exception {
        running.set(true);
    }

    public void stop() {
        stop(null);
    }

    public void stop(final Runnable onComplete) {
        if (running.compareAndSet(true, false)) {
            queue.execute(new Runnable() {
                public void run() {
                    final AtomicInteger latch = new AtomicInteger(transports.size());
                    final Runnable coutDown = new Runnable() {
                        public void run() {
                            if (latch.decrementAndGet() == 0) {
                                pending.clear();
                                onComplete.run();
                            }
                        }
                    };
                    for (Map.Entry<Transport, State> entry : transports.entrySet()) {
                        entry.getKey().stop(coutDown);
                    }
                }
            });
        } else {
            onComplete.run();
        }
    }

    protected void startNewTransport() throws Exception {
        Transport transport = createTransport(this.uri);
        transport.setDispatchQueue(queue);
        transport.setProtocolCodec(createCodec());
        transport.setTransportListener(new Listener());
        transports.put(transport, State.Creating);
        transport.start();
    }

    protected class Listener implements TransportListener {

        public void onTransportCommand(Transport transport, Object command) {
            TransportPool.this.onCommand(command);
        }

        public void onRefill(Transport transport) {
            if (pending.size() > 0) {
                Object data = pending.remove(0);
                if (transport.offer(data)) {
                    transports.put(transport, State.Writing);
                } else {
                    pending.add(data);
                }
            } else {
                transports.put(transport, State.Idle);
            }
        }

        public void onTransportFailure(Transport transport, IOException error) {
            transports.remove(transport);
            transport.stop();
        }

        public void onTransportConnected(Transport transport) {
            transport.resumeRead();
            onRefill(transport);
        }

        public void onTransportDisconnected(Transport transport) {
            transports.remove(transport);
        }
    }
}
