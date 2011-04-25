/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.dosgi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.fusesource.fabric.dosgi.api.*;
import org.fusesource.fabric.dosgi.io.ServerInvoker;
import org.fusesource.fabric.dosgi.tcp.ClientInvokerImpl;
import org.fusesource.fabric.dosgi.tcp.ServerInvokerImpl;
import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.junit.Test;

import javax.management.StringValueExp;

import static org.junit.Assert.assertEquals;

public class InvocationTest {
    final static long MILLIS_IN_A_NANO = TimeUnit.MILLISECONDS.toNanos(1);
    final static long SECONDS_IN_A_NANO = TimeUnit.SECONDS.toNanos(1);

    @Test
    public void testInvoke() throws Exception {

        DispatchQueue queue = Dispatch.createQueue();
        HashMap<String, SerializationStrategy> map = new HashMap<String, SerializationStrategy>();
        map.put("protobuf", new ProtobufSerializationStrategy());

        ServerInvokerImpl server = new ServerInvokerImpl("tcp://localhost:0", queue, map);
        server.start();

        ClientInvokerImpl client = new ClientInvokerImpl(queue, map);
        client.start();

        try {
            server.registerService("service-id", new ServerInvoker.ServiceFactory() {
                public Object get() {
                    return new HelloImpl();
                }
                public void unget() {
                }
            }, HelloImpl.class.getClassLoader());


            InvocationHandler handler = client.getProxy(server.getConnectAddress(), "service-id", HelloImpl.class.getClassLoader());
            Hello hello  = (Hello) Proxy.newProxyInstance(HelloImpl.class.getClassLoader(), new Class[] { Hello.class }, handler);

            assertEquals("Hello Fabric!", hello.hello("Fabric"));

            // Verification the we can pick the right overloaded method even if using a mixure
            // of primitives / objects and array dimensions.
            assertEquals('a', hello.mix(0));
            assertEquals('b', hello.mix(new int[]{0}));
            assertEquals('c', hello.mix(new Integer(0)));
            assertEquals('d', hello.mix(new Integer[]{new Integer(0)}));
            assertEquals('e', hello.mix(new int[0][0]));
            assertEquals('f', hello.mix(new Integer[0][0]));

            AsyncCallbackFuture<String> future1 = new AsyncCallbackFuture<String>();
            hello.hello("Hiram", future1);
            assertEquals("Hello Hiram!", future1.get(2, TimeUnit.SECONDS));

            assertEquals("Hello Hiram!", hello.protobuf(stringValue("Hiram")).getValue());

            AsyncCallbackFuture<StringValue.Getter> future2 = new AsyncCallbackFuture<StringValue.Getter>();
            hello.protobuf(stringValue("Hiram Async"), future2);
            assertEquals("Hello Hiram Async!", future2.get(2, TimeUnit.SECONDS).getValue());

        }
        finally {
            server.stop();
            client.stop();
        }
    }

    @Test
    public void testUnderLoad() throws Exception {
        HashMap<String, SerializationStrategy> map = new HashMap<String, SerializationStrategy>();

        DispatchQueue queue = Dispatch.createQueue();
        ServerInvokerImpl server = new ServerInvokerImpl("tcp://localhost:0", queue, map);
        server.start();
        ClientInvokerImpl client = new ClientInvokerImpl(queue, map);
        client.start();

        try {
            server.registerService("service-id", new ServerInvoker.ServiceFactory() {
                public Object get() {
                    return new HelloImpl();
                }
                public void unget() {
                }
            }, HelloImpl.class.getClassLoader());


            InvocationHandler handler = client.getProxy(server.getConnectAddress(), "service-id", HelloImpl.class.getClassLoader());

            final Hello hello  = (Hello) Proxy.newProxyInstance(HelloImpl.class.getClassLoader(), new Class[] { Hello.class }, handler);

            final int nbThreads = 100;
            final int nbInvocationsPerThread = 1000;

            final AtomicInteger requests = new AtomicInteger(0);
            final AtomicInteger failures = new AtomicInteger(0);
            final long latencies[] = new long[nbThreads*nbInvocationsPerThread];

            final long start = System.nanoTime();
            Thread[] threads = new Thread[nbThreads];
            for (int t = 0; t < nbThreads; t++) {
                final int thread_idx = t;
                threads[t] = new Thread() {
                    public void run() {
                        for (int i = 0; i < nbInvocationsPerThread; i++) {
                            try {
                                requests.incrementAndGet();
                                String response;

                                final long start = System.nanoTime();
                                response = hello.hello("Fabric");
                                final long end = System.nanoTime();
                                latencies[(thread_idx*nbInvocationsPerThread)+i] = end-start;

                                assertEquals("Hello Fabric!", response);
                            } catch (Throwable t) {
                                latencies[(thread_idx*nbInvocationsPerThread)+i] = -1;
                                failures.incrementAndGet();
                                if (t instanceof UndeclaredThrowableException) {
                                    t = ((UndeclaredThrowableException) t).getUndeclaredThrowable();
                                }
                                System.err.println("Error: " + t.getClass().getName() + (t.getMessage() != null ? " (" + t.getMessage() + ")" : ""));
                            }
                        }
                    }
                };
                threads[t].start();
            }

            for (int t = 0; t < nbThreads; t++) {
                threads[t].join();
            }
            final long end = System.nanoTime();

            long latency_sum = 0;
            for (int t = 0; t < latencies.length; t++) {
                if( latencies[t] != -1 ) {
                    latency_sum += latencies[t];
                }
            }
            double latency_avg = ((latency_sum * 1.0d)/requests.get()) / MILLIS_IN_A_NANO;
            double request_rate = ((requests.get() * 1.0d)/(end-start)) * SECONDS_IN_A_NANO;

            System.err.println(String.format("Requests/Second: %,.2f", request_rate));
            System.err.println(String.format("Average request latency: %,.2f ms", latency_avg));
            System.err.println("Error Ratio: " + failures.get() + " / " + requests.get());
        }
        finally {
            server.stop();
            client.stop();
        }
    }

    public static interface Hello {
        String hello(String name);

        // async version of the hello method.
        void hello(String name, AsyncCallback<String> callback);

        char mix(int value);
        char mix(int[] value);
        char mix(Integer value);
        char mix(Integer[] value);
        char mix(int[][] value);
        char mix(Integer[][] value);

        @Serialization("protobuf")
        StringValue.Getter protobuf(StringValue.Getter name);

        @Serialization("protobuf")
        void protobuf(StringValue.Getter name, AsyncCallback<StringValue.Getter> callback);

    }

    static private StringValue.Bean stringValue(String hello) {
        StringValue.Bean rc = new StringValue.Bean();
        rc.setValue(hello);
        return rc;
    }

    public static class HelloImpl implements Hello, Dispatched {

        DispatchQueue queue = Dispatch.createQueue();

        public DispatchQueue queue() {
            return queue;
        }

        private void queueCheck() {
            if( !queue.isExecuting() ) {
                throw new IllegalStateException("Not executing on our dispatch queue");
            }
        }

        public String hello(String name) {
            queueCheck();
            return "Hello " + name + "!";
        }

        @Serialization("protobuf")
        public StringValue.Getter protobuf(StringValue.Getter name) {
            return stringValue(hello(name.getValue()));
        }

        @Serialization("protobuf")
        public void protobuf(StringValue.Getter name, AsyncCallback<StringValue.Getter> callback) {
            callback.onSuccess(protobuf(name));
        }

        public void hello(String name, AsyncCallback<String> callback) {
            queueCheck();
            callback.onSuccess(hello(name));
        }

        public char mix(int value) {
            queueCheck();
            return 'a';
        }

        public char mix(int[] value) {
            queueCheck();
            return 'b';
        }

        public char mix(Integer value) {
            queueCheck();
            return 'c';
        }

        public char mix(Integer[] value) {
            queueCheck();
            return 'd';
        }

        public char mix(int[][] value) {
            queueCheck();
            return 'e';
        }
        public char mix(Integer[][] value) {
            queueCheck();
            return 'f';
        }

    }


}
