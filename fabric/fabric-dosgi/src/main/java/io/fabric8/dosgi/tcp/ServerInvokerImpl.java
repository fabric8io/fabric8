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
package io.fabric8.dosgi.tcp;

import io.fabric8.dosgi.api.Dispatched;
import io.fabric8.dosgi.api.ObjectSerializationStrategy;
import io.fabric8.dosgi.api.Serialization;
import io.fabric8.dosgi.api.SerializationStrategy;
import io.fabric8.dosgi.io.*;
import org.fusesource.hawtbuf.*;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerInvokerImpl implements ServerInvoker, Dispatched {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ServerInvokerImpl.class);
    static private final HashMap<String, Class> PRIMITIVE_TO_CLASS = new HashMap<String, Class>(8, 1.0F);
    static {
        PRIMITIVE_TO_CLASS.put("Z", boolean.class);
        PRIMITIVE_TO_CLASS.put("B", byte.class);
        PRIMITIVE_TO_CLASS.put("C", char.class);
        PRIMITIVE_TO_CLASS.put("S", short.class);
        PRIMITIVE_TO_CLASS.put("I", int.class);
        PRIMITIVE_TO_CLASS.put("J", long.class);
        PRIMITIVE_TO_CLASS.put("F", float.class);
        PRIMITIVE_TO_CLASS.put("D", double.class);
    }

    protected final ExecutorService blockingExecutor = Executors.newFixedThreadPool(8);
    protected final DispatchQueue queue;
    private final Map<String, SerializationStrategy> serializationStrategies;
    protected final TransportServer server;
    protected final Map<UTF8Buffer, ServiceFactoryHolder> holders = new HashMap<UTF8Buffer, ServiceFactoryHolder>();

    static class MethodData {

        private final SerializationStrategy serializationStrategy;
        final InvocationStrategy invocationStrategy;
        final Method method;

        MethodData(InvocationStrategy invocationStrategy, SerializationStrategy serializationStrategy, Method method) {
            this.invocationStrategy = invocationStrategy;
            this.serializationStrategy = serializationStrategy;
            this.method = method;
        }
    }

    class ServiceFactoryHolder {

        private final ServiceFactory factory;
        private final ClassLoader loader;
        private final Class clazz;
        private HashMap<Buffer, MethodData> method_cache = new HashMap<Buffer, MethodData>();

        public ServiceFactoryHolder(ServiceFactory factory, ClassLoader loader) {
            this.factory = factory;
            this.loader = loader;
            Object o = factory.get();
            clazz = o.getClass();
            factory.unget();
        }

        private MethodData getMethodData(Buffer data) throws IOException, NoSuchMethodException, ClassNotFoundException {
            MethodData rc = method_cache.get(data);
            if( rc == null ) {
                String[] parts = data.utf8().toString().split(",");
                String name = parts[0];
                Class params[] = new Class[parts.length-1];
                for( int  i=0; i < params.length; i++) {
                    params[i] = decodeClass(parts[i+1]);
                }
                Method method = clazz.getMethod(name, params);


                Serialization annotation = method.getAnnotation(Serialization.class);
                SerializationStrategy serializationStrategy;
                if( annotation!=null ) {
                    serializationStrategy = serializationStrategies.get(annotation.value());
                    if( serializationStrategy==null ) {
                        throw new RuntimeException("Could not find the serialization strategy named: "+annotation.value());
                    }
                } else {
                    serializationStrategy = ObjectSerializationStrategy.INSTANCE;
                }


                final InvocationStrategy invocationStrategy;
                if( AsyncInvocationStrategy.isAsyncMethod(method) ) {
                    invocationStrategy = AsyncInvocationStrategy.INSTANCE;
                } else {
                    invocationStrategy = BlockingInvocationStrategy.INSTANCE;
                }

                rc = new MethodData(invocationStrategy, serializationStrategy, method);
                method_cache.put(data, rc);
            }
            return rc;
        }

        private Class<?> decodeClass(String s) throws ClassNotFoundException {
            if( s.startsWith("[")) {
                Class<?> nested = decodeClass(s.substring(1));
                return Array.newInstance(nested,0).getClass();
            }
            String c = s.substring(0,1);
            if( c.equals("L") ) {
                return loader.loadClass(s.substring(1));
            } else {
                return PRIMITIVE_TO_CLASS.get(c);
            }
        }

    }


    public ServerInvokerImpl(String address, DispatchQueue queue, Map<String, SerializationStrategy> serializationStrategies) throws Exception {
        this.queue = queue;
        this.serializationStrategies = serializationStrategies;
        this.server = new TcpTransportFactory().bind(address);
        this.server.setDispatchQueue(queue);
        this.server.setAcceptListener(new InvokerAcceptListener());
    }

    public InetSocketAddress getSocketAddress() {
        return this.server.getSocketAddress();
    }


    public DispatchQueue queue() {
        return queue;
    }

    public String getConnectAddress() {
        return this.server.getConnectAddress();
    }

    public void registerService(final String id, final ServiceFactory service, final ClassLoader classLoader) {
        queue().execute(new Runnable() {
            public void run() {
                holders.put(new UTF8Buffer(id), new ServiceFactoryHolder(service, classLoader));
            }
        });
    }

    public void unregisterService(final String id) {
        queue().execute(new Runnable() {
            public void run() {
                holders.remove(new UTF8Buffer(id));
            }
        });
    }

    public void start() throws Exception {
        start(null);
    }

    public void start(Runnable onComplete) throws Exception {
        this.server.start(onComplete);
    }

    public void stop() {
        stop(null);
    }

    public void stop(final Runnable onComplete) {
        this.server.stop(new Runnable() {
            public void run() {
                blockingExecutor.shutdown();
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }


    protected void onCommand(final Transport transport, Object data) {
        try {
            final DataByteArrayInputStream bais = new DataByteArrayInputStream((Buffer) data);
            final int size = bais.readInt();
            final long correlation = bais.readVarLong();

            // Use UTF8Buffer instead of string to avoid encoding/decoding UTF-8 strings
            // for every request.
            final UTF8Buffer service = readBuffer(bais).utf8();
            final Buffer encoded_method = readBuffer(bais);

            final ServiceFactoryHolder holder = holders.get(service);
            final MethodData methodData = holder.getMethodData(encoded_method);

            final Object svc = holder.factory.get();

            Runnable task = new Runnable() {
                public void run() {

                    final DataByteArrayOutputStream baos = new DataByteArrayOutputStream();
                    try {
                        baos.writeInt(0); // make space for the size field.
                        baos.writeVarLong(correlation);
                    } catch (IOException e) { // should not happen
                        throw new RuntimeException(e);
                    }

                    // Lets decode the remaining args on the target's executor
                    // to take cpu load off the
                    methodData.invocationStrategy.service(methodData.serializationStrategy, holder.loader, methodData.method, svc, bais, baos, new Runnable() {
                        public void run() {
                            holder.factory.unget();
                            final Buffer command = baos.toBuffer();

                            // Update the size field.
                            BufferEditor editor = command.buffer().bigEndianEditor();
                            editor.writeInt(command.length);

                            queue().execute(new Runnable() {
                                public void run() {
                                    transport.offer(command);
                                }
                            });
                        }
                    });
                }
            };

            Executor executor;
            if( svc instanceof Dispatched ) {
                executor = ((Dispatched)svc).queue();
            } else {
                executor = blockingExecutor;
            }
            executor.execute(task);

        } catch (Exception e) {
            LOGGER.info("Error while reading request", e);
        }
    }

    private Buffer readBuffer(DataByteArrayInputStream bais) throws IOException {
        byte b[] = new byte[bais.readVarInt()];
        bais.readFully(b);
        return new Buffer(b);
    }

    class InvokerAcceptListener implements TransportAcceptListener {

        public void onAccept(TransportServer transportServer, TcpTransport transport) {
            transport.setProtocolCodec(new LengthPrefixedCodec());
            transport.setDispatchQueue(queue());
            transport.setTransportListener(new InvokerTransportListener());
            transport.start();
        }

        public void onAcceptError(TransportServer transportServer, Exception error) {
            LOGGER.info("Error accepting incoming connection", error);
        }
    }

    class InvokerTransportListener implements TransportListener {

        public void onTransportCommand(Transport transport, Object command) {
            ServerInvokerImpl.this.onCommand(transport, command);
        }

        public void onRefill(Transport transport) {
        }

        public void onTransportFailure(Transport transport, IOException error) {
            if (!transport.isDisposed() && !(error instanceof EOFException)) {
                LOGGER.info("Transport failure", error);
            }
        }

        public void onTransportConnected(Transport transport) {
            transport.resumeRead();
        }

        public void onTransportDisconnected(Transport transport) {
        }
    }

}
