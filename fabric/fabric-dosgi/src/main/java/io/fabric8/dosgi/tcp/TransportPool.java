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
package io.fabric8.dosgi.tcp;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.fabric8.dosgi.io.ProtocolCodec;
import io.fabric8.dosgi.io.Service;
import io.fabric8.dosgi.io.Transport;
import io.fabric8.dosgi.io.TransportListener;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TransportPool implements Service {

    protected static final Logger LOGGER = LoggerFactory.getLogger(TransportPool.class);

    public static final int DEFAULT_POOL_SIZE = 2;

    public static final long DEFAULT_EVICTION_DELAY = TimeUnit.MINUTES.toMillis(5);

    protected final String uri;
    protected final DispatchQueue queue;
    protected final LinkedList<Pair> pending = new LinkedList<Pair>();
    protected final Map<Transport, TransportState> transports = new HashMap<Transport, TransportState>();
    protected AtomicBoolean running = new AtomicBoolean(false);

    protected int poolSize;
    protected long evictionDelay;

    public TransportPool(String uri, DispatchQueue queue) {
        this(uri, queue, DEFAULT_POOL_SIZE, DEFAULT_EVICTION_DELAY);
    }

    public TransportPool(String uri, DispatchQueue queue, int poolSize, long evictionDelay) {
        this.uri = uri;
        this.queue = queue;
        this.poolSize = poolSize;
        this.evictionDelay = evictionDelay;
    }

    protected abstract Transport createTransport(String uri) throws Exception;

    protected abstract ProtocolCodec createCodec();

    protected abstract void onCommand(Object command);

    protected abstract void onFailure(Object id, Throwable throwable);

    protected void onDone(Object id) {
        for (TransportState state : transports.values()) {
            if (state.inflight.remove(id)) {
                break;
            }
        }
    }

    public void offer(final Object data, final Object id) {
        if (!running.get()) {
            throw new IllegalStateException("Transport pool stopped");
        }
        queue.execute(new Runnable() {
            public void run() {
                Transport transport = getIdleTransport();
                if (transport != null) {
                    doOffer(transport, data, id);
                    if( transport.full() ) {
                        transports.get(transport).time = 0L;
                    }
                } else {
                    pending.add(new Pair(data, id));
                }
            }
        });
    }

    protected boolean doOffer(Transport transport, Object command, Object id) {
        transports.get(transport).inflight.add(id);
        return transport.offer(command);
    }

    protected Transport getIdleTransport() {
        for (Map.Entry<Transport, TransportState> entry : transports.entrySet()) {
            if (entry.getValue().time > 0) {
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
                                while (!pending.isEmpty()) {
                                    Pair p = pending.removeFirst();
                                    onFailure(p.id, new IOException("Transport stopped"));
                                }
                                onComplete.run();
                            }
                        }
                    };
                    while (!transports.isEmpty()) {
                        Transport transport = transports.keySet().iterator().next();
                        TransportState state = transports.remove(transport);
                        if (state != null) {
                            for (Object id : state.inflight) {
                                onFailure(id, new IOException("Transport stopped"));
                            }
                        }
                        transport.stop(coutDown);
                    }
                }
            });
        } else {
            onComplete.run();
        }
    }

    protected void startNewTransport() throws Exception {
        LOGGER.debug("Creating new transport for: {}", this.uri);
        Transport transport = createTransport(this.uri);
        transport.setDispatchQueue(queue);
        transport.setProtocolCodec(createCodec());
        transport.setTransportListener(new Listener());
        transports.put(transport, new TransportState());
        transport.start();
    }

    protected static class Pair {
        Object command;
        Object id;

        public Pair(Object command, Object id) {
            this.command = command;
            this.id = id;
        }
    }

    protected static class TransportState {
        long time;
        final Set<Object> inflight;

        public TransportState() {
            time = 0;
            inflight = new HashSet<Object>();
        }
    }

    protected class Listener implements TransportListener {

        public void onTransportCommand(Transport transport, Object command) {
            TransportPool.this.onCommand(command);
        }

        public void onRefill(final Transport transport) {
            while (pending.size() > 0 &&  !transport.full()) {
                Pair pair = pending.removeFirst();
                boolean accepted = doOffer(transport, pair.command, pair.id);
                assert accepted: "Should have been accepted since the transport was not full";
            }

            if( transport.full() ) {
                transports.get(transport).time = 0L;
            } else {
                final long time = System.currentTimeMillis();
                transports.get(transport).time = time;
                if (evictionDelay > 0) {
                    queue.executeAfter(evictionDelay, TimeUnit.MILLISECONDS, new Runnable() {
                        public void run() {
                            TransportState state = transports.get(transport);
                            if (state != null && state.time == time) {
                                transports.remove(transport);
                                transport.stop();
                            }
                        }
                    });
                }
            }

        }

        public void onTransportFailure(Transport transport, IOException error) {
            if (!transport.isDisposed()) {
                LOGGER.info("Transport failure", error);
                TransportState state = transports.remove(transport);
                if (state != null) {
                    for (Object id : state.inflight) {
                        onFailure(id, error);
                    }
                }
                transport.stop();
                if (transports.isEmpty()) {
                    while (!pending.isEmpty()) {
                        Pair p = pending.removeFirst();
                        onFailure(p.id, error);
                    }
                }
            }
        }

        public void onTransportConnected(Transport transport) {
            transport.resumeRead();
            onRefill(transport);
        }

        public void onTransportDisconnected(Transport transport) {
            onTransportFailure(transport, new IOException("Transport disconnected"));
        }
    }
}
