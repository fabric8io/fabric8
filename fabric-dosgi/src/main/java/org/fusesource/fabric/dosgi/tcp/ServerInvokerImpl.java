/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.dosgi.tcp;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.fusesource.fabric.dosgi.io.ServerInvoker;
import org.fusesource.fabric.dosgi.io.Transport;
import org.fusesource.fabric.dosgi.io.TransportAcceptListener;
import org.fusesource.fabric.dosgi.io.TransportListener;
import org.fusesource.fabric.dosgi.io.TransportServer;
import org.fusesource.fabric.dosgi.util.ClassLoaderObjectInputStream;
import org.fusesource.hawtbuf.*;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerInvokerImpl implements ServerInvoker {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ServerInvokerImpl.class);

    protected final ExecutorService executor = Executors.newCachedThreadPool();
    protected final DispatchQueue queue;
    protected final TransportServer server;
    protected final Map<UTF8Buffer, ServiceFactory> handlers = new HashMap<UTF8Buffer, ServiceFactory>();
    protected final Map<UTF8Buffer, ClassLoader> loaders = new HashMap<UTF8Buffer, ClassLoader>();

    public ServerInvokerImpl(String address, DispatchQueue queue) throws Exception {
        this.queue = queue;
        this.server = new TcpTransportFactory().bind(address);
        this.server.setDispatchQueue(queue);
        this.server.setAcceptListener(new InvokerAcceptListener());
    }

    public String getConnectAddress() {
        return this.server.getConnectAddress();
    }

    public void registerService(final String id, final ServiceFactory service, final ClassLoader classLoader) {
        queue.execute(new Runnable() {
            public void run() {
                handlers.put(new UTF8Buffer(id), service);
                loaders.put(new UTF8Buffer(id), classLoader);
            }
        });
    }

    public void unregisterService(final String id) {
        queue.execute(new Runnable() {
            public void run() {
                handlers.remove(new UTF8Buffer(id));
                loaders.remove(new UTF8Buffer(id));
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
                executor.shutdown();
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    protected void onCommand(final Transport transport, Object data) {
        try {
            DataByteArrayInputStream bais = new DataByteArrayInputStream((Buffer) data);
            final int size = bais.readInt();
            final long correlation = bais.readVarLong();

            // Use UTF8Buffer instead of string to avoid encoding/decoding UTF-8 strings
            // for every request.
            final UTF8Buffer service = readUTF8Buffer(bais);

            final ClassLoaderObjectInputStream ois = new ClassLoaderObjectInputStream(bais);

            ois.setClassLoader(loaders.get(service));
            final ServiceFactory factory = handlers.get(service);

            executor.submit(new Runnable() {
                public void run() {

                    // Lets decode the remaining args on the target's executor
                    // to take cpu load off the

                    final String name;
                    final Class[] types;
                    final Object[] args;
                    try {
                        name = ois.readUTF();
                        types = (Class[]) ois.readObject();
                        args = (Object[]) ois.readObject();
                    } catch (Exception e) {
                        LOGGER.info("Error while reading request", e);
                        return;
                    }

                    Object svc = factory.get();
                    Object value = null;
                    Throwable error = null;
                    try {
                        Method method = svc.getClass().getMethod(name, types);
                        value = method.invoke(svc, args);
                    } catch (Throwable t) {
                        if (t instanceof InvocationTargetException) {
                            error = t.getCause();
                        } else {
                            error = t;
                        }
                    } finally {
                        factory.unget();
                    }

                    // Encode the response...
                    try {
                        DataByteArrayOutputStream baos = new DataByteArrayOutputStream();
                        baos.writeInt(0); // make space for the size field.
                        baos.writeVarLong(correlation);
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        oos.writeObject(error);
                        oos.writeObject(value);

                        // toBuffer() is better than toByteArray() since it avoids an
                        // array copy.
                        final Buffer command = baos.toBuffer();

                        // Update the size field.
                        BufferEditor editor = command.buffer().bigEndianEditor();
                        editor.writeInt(command.length);

                        queue.execute(new Runnable() {
                            public void run() {
                                transport.offer(command);
                            }
                        });
                    } catch (Exception e) {
                        LOGGER.info("Error while writing answer");
                    }

                }
            });


        } catch (Exception e) {
            LOGGER.info("Error while reading request", e);
        }
    }

    private UTF8Buffer readUTF8Buffer(DataByteArrayInputStream bais) throws IOException {
        byte b[] = new byte[bais.readVarInt()];
        bais.readFully(b);
        return new UTF8Buffer(b);
    }

    class InvokerAcceptListener implements TransportAcceptListener {

        public void onAccept(TransportServer transportServer, TcpTransport transport) {
            transport.setProtocolCodec(new LengthPrefixedCodec());
            transport.setDispatchQueue(queue);
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
