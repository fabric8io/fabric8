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
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.fusesource.fabric.dosgi.io.ClientInvoker;
import org.fusesource.fabric.dosgi.io.ProtocolCodec;
import org.fusesource.fabric.dosgi.io.Transport;
import org.fusesource.fabric.dosgi.util.ClassLoaderObjectInputStream;
import org.fusesource.hawtbuf.*;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientInvokerImpl implements ClientInvoker {

    public static final long DEFAULT_TIMEOUT = TimeUnit.MINUTES.toMillis(5);

    protected static final Logger LOGGER = LoggerFactory.getLogger(ClientInvokerImpl.class);

    private final static HashMap<Class,String> CLASS_TO_PRIMITIVE = new HashMap<Class, String>(8, 1.0F);

    static {
        CLASS_TO_PRIMITIVE.put(boolean.class,"Z");
        CLASS_TO_PRIMITIVE.put(byte.class,"B");
        CLASS_TO_PRIMITIVE.put(char.class,"C");
        CLASS_TO_PRIMITIVE.put(short.class,"S");
        CLASS_TO_PRIMITIVE.put(int.class,"I");
        CLASS_TO_PRIMITIVE.put(long.class,"J");
        CLASS_TO_PRIMITIVE.put(float.class,"F");
        CLASS_TO_PRIMITIVE.put(double.class,"D");
    }

    protected final AtomicLong correlationGenerator = new AtomicLong();
    protected final DispatchQueue queue;
    protected final Map<String, TransportPool> transports = new HashMap<String, TransportPool>();
    protected final AtomicBoolean running = new AtomicBoolean(false);
    protected final Map<Long, ResponseFuture> requests = new HashMap<Long, ResponseFuture>();
    protected final long timeout;

    public ClientInvokerImpl(DispatchQueue queue) {
        this(queue, DEFAULT_TIMEOUT);
    }

    public ClientInvokerImpl(DispatchQueue queue, long timeout) {
        this.queue = queue;
        this.timeout = timeout;
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
            DataByteArrayInputStream bais = new DataByteArrayInputStream( (Buffer) data);
            int size = bais.readInt();
            long correlation = bais.readVarLong();
            ClassLoaderObjectInputStream ois = new ClassLoaderObjectInputStream(bais);
            ResponseFuture response = requests.remove(correlation);
            if( response!=null ) {
                ois.setClassLoader(response.getClassLoader());
                Throwable error = (Throwable) ois.readObject();
                Object value = ois.readObject();
                if (error != null) {
                    response.setException(error);
                } else {
                    response.set(value);
                }
            }
        } catch (Exception e) {
            LOGGER.info("Error while reading response", e);
        }
    }

    static final WeakHashMap<Method, Buffer> method_cache = new WeakHashMap<Method, Buffer>();

    private Buffer encodeMethod(Method method) throws IOException {
        Buffer rc = null;
        synchronized (method_cache) {
            rc = method_cache.get(method);
        }
        if( rc==null ) {
            StringBuilder sb = new StringBuilder();
            sb.append(method.getName());
            sb.append(",");
            Class<?>[] types = method.getParameterTypes();
            for(int i=0; i < types.length; i++) {
                if( i!=0 ) {
                    sb.append(",");
                }
                sb.append(encodeClassName(types[i]));
            }
            rc = new UTF8Buffer(sb.toString()).buffer();
            synchronized (method_cache) {
                method_cache.put(method, rc);
            }
        }
        return rc;
    }

    String encodeClassName(Class<?> type) {
        if( type.getComponentType()!=null ) {
            return "["+ encodeClassName(type.getComponentType());
        }
        if( type.isPrimitive() ) {
            return CLASS_TO_PRIMITIVE.get(type);
        } else {
            return "L"+type.getName();
        }
    }

    protected Object request(ProxyInvocationHandler handler, final String address, final UTF8Buffer service, final ClassLoader classLoader, final Method method, final Object[] args) throws ExecutionException, InterruptedException, IOException, TimeoutException {

        final long correlation = correlationGenerator.incrementAndGet();

        // Encode the request before we try to pass it onto
        // IO layers so that #1 we can report encoding error back to the caller
        // and #2 reduce CPU load done in the execution queue since it's
        // serially executed.

        DataByteArrayOutputStream baos = new DataByteArrayOutputStream((int) (handler.lastRequestSize*1.10));
        baos.writeInt(0); // we don't know the size yet...
        baos.writeVarLong(correlation);
        writeBuffer(baos, service);
        writeBuffer(baos, encodeMethod(method));

        // TODO: perhaps use a different encoding method for the args based on annotations found on the method.
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(args);

        // toBuffer() is better than toByteArray() since it avoids an
        // array copy.
        final Buffer command = baos.toBuffer();


        // Update the field size.
        BufferEditor editor = command.buffer().bigEndianEditor();
        editor.writeInt(command.length);
        handler.lastRequestSize = command.length;

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
                    requests.put(correlation, future);
                    pool.offer(command);
                } catch (Exception e) {
                    LOGGER.info("Error while sending request", e);
                }
            }
        });
        // TODO: make that configurable, that's only for tests
        return future.get(timeout, TimeUnit.MILLISECONDS);
    }

    private void writeBuffer(DataByteArrayOutputStream baos, Buffer value) throws IOException {
        baos.writeVarInt(value.length);
        baos.write(value);
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
        final UTF8Buffer service;
        final ClassLoader classLoader;
        int lastRequestSize = 250;

        public ProxyInvocationHandler(String address, String service, ClassLoader classLoader) {
            this.address = address;
            this.service = new UTF8Buffer(service);
            this.classLoader = classLoader;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return request(this, address, service, classLoader, method, args);
        }

    }

    protected class InvokerTransportPool extends TransportPool {

        public InvokerTransportPool(String uri, DispatchQueue queue) {
            super(uri, queue, TransportPool.DEFAULT_POOL_SIZE, timeout << 1);
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
