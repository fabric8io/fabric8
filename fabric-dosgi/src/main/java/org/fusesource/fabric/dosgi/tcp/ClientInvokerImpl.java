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
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.fusesource.fabric.dosgi.io.ClientInvoker;
import org.fusesource.fabric.dosgi.io.ProtocolCodec;
import org.fusesource.fabric.dosgi.io.Transport;
import org.fusesource.fabric.dosgi.util.ClassLoaderObjectInputStream;
import org.fusesource.fabric.dosgi.util.UuidGenerator;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.BufferEditor;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.fusesource.hawtbuf.ByteArrayOutputStream;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientInvokerImpl implements ClientInvoker {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ClientInvokerImpl.class);

    private final DispatchQueue queue;
    private final Map<String, TransportPool> transports = new HashMap<String, TransportPool>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Map<String, ResponseFuture> requests = new HashMap<String, ResponseFuture>();

    public ClientInvokerImpl(DispatchQueue queue) {
        this.queue = queue;
    }

    public void start() throws Exception {
        start(null);
    }

    public void start(Runnable onComplete) throws Exception {
        running.set(true);
        if (onComplete != null) {
            onComplete.run();
        }
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
                                if (onComplete != null) {
                                    onComplete.run();
                                }
                            }
                        }
                    };
                    for (TransportPool pool : transports.values()) {
                        pool.stop(coutDown);
                    }
                }
            });
        } else {
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    public InvocationHandler getProxy(String address, String service, ClassLoader classLoader) {
        return new ProxyInvocationHandler(address, service, classLoader);
    }

    protected void onCommand(Object data) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream( (Buffer) data);
            ClassLoaderObjectInputStream ois = new ClassLoaderObjectInputStream(bais);
            int size = ois.readInt();
            String correlation = ois.readUTF();
            ResponseFuture response = requests.remove(correlation);
            ois.setClassLoader(response.getClassLoader());
            Throwable error = (Throwable) ois.readObject();
            Object value = ois.readObject();
            if (error != null) {
                response.setException(error);
            } else {
                response.set(value);
            }
        } catch (Exception e) {
            LOGGER.info("Error while reading response", e);
        }
    }

    protected Object request(final String address, final String service, final ClassLoader classLoader, final Method method, final Object[] args) throws ExecutionException, InterruptedException, IOException {

        final String uuid = UuidGenerator.getUUID();

        // Encode the request before we try to pass it onto
        // IO layers so that #1 we can report encoding error back to the caller
        // and #2 reduce CPU load done in the execution queue since it's
        // serially executed.

        // We can probably get a nice perf boots if we track
        // average serialized request size so we can pick an optimal
        // initial byte array size.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeInt(0); // we don't know the size yet...
        oos.writeUTF(uuid);
        oos.writeUTF(service);
        oos.writeUTF(method.getName());
        oos.writeObject(method.getParameterTypes());
        oos.writeObject(args);

        // toBuffer() is better than toByteArray() since it avoids an
        // array copy.
        final Buffer command = baos.toBuffer();

        // Update the field size.
        BufferEditor editor = command.buffer().bigEndianEditor();
        editor.writeInt(command.length);

        final ResponseFuture future = new ResponseFuture(classLoader);
        queue.execute(new Runnable() {
            public void run() {
                try {
                    TransportPool pool = transports.get(address);
                    if (pool == null) {
                        pool = new InvokerTransportPool(address, queue);
                        transports.put(address,  pool);
                        pool.start();
                    }
                    requests.put(uuid, future);
                    pool.offer(command);
                } catch (Exception e) {
                    LOGGER.info("Error while sending request", e);
                }
            }
        });
        return future.get();
    }

    protected static class ResponseFuture extends FutureTask<Object> {

        private static final Callable<Object> EMPTY_CALLABLE = new Callable<Object>() {
            public Object call() {
                return null;
            }
        };
        private final ClassLoader classLoader;

        public ResponseFuture(ClassLoader classLoader) {
            super(EMPTY_CALLABLE);
            this.classLoader = classLoader;
        }

        public ClassLoader getClassLoader() {
            return classLoader;
        }

        @Override
        public void set(Object object) {
            super.set(object);
        }

        @Override
        public void setException(Throwable throwable) {
            super.setException(throwable);
        }
    }

    protected class ProxyInvocationHandler implements InvocationHandler {

        final String address;
        final String service;
        final ClassLoader classLoader;

        public ProxyInvocationHandler(String address, String service, ClassLoader classLoader) {
            this.address = address;
            this.service = service;
            this.classLoader = classLoader;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return request(address, service, classLoader, method, args);
        }

    }

    protected class InvokerTransportPool extends TransportPool {

        public InvokerTransportPool(String uri, DispatchQueue queue) {
            super(uri, queue);
        }

        @Override
        protected Transport createTransport(String uri) throws Exception {
            return new TcpTransportFactory().connect(uri);
        }

        @Override
        protected ProtocolCodec createCodec() {
            return new LengthPrefixedCodec();
        }

        @Override
        protected void onCommand(Object command) {
            ClientInvokerImpl.this.onCommand(command);
        }
    }

}
