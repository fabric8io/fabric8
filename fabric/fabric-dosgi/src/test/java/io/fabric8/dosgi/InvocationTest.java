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
package io.fabric8.dosgi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.fabric8.dosgi.api.*;
import io.fabric8.dosgi.io.ServerInvoker;
import io.fabric8.dosgi.tcp.ClientInvokerImpl;
import io.fabric8.dosgi.tcp.ServerInvokerImpl;
import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.junit.Test;

import static org.fusesource.hawtdispatch.Dispatch.createQueue;
import static org.junit.Assert.assertEquals;

public class InvocationTest {
    final static long MILLIS_IN_A_NANO = TimeUnit.MILLISECONDS.toNanos(1);
    final static long SECONDS_IN_A_NANO = TimeUnit.SECONDS.toNanos(1);

    final int BENCHMARK_CLIENTS = 100;
    final int BENCHMARK_INVOCATIONS_PER_CLIENT = 1000;


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
            assertEquals("Hello World!", hello.helloworld());

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
    public void testOverflowAsync() throws Exception {

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

    		char[] chars = new char[65*1024];
    		String payload = new String(chars);

    		final List<AsyncCallbackFuture<String>> futures = new ArrayList<AsyncCallbackFuture<String>>();
    		for(int i = 0; i < 100; i++) {
    			AsyncCallbackFuture<String> future = new AsyncCallbackFuture<String>();
    			hello.hello(payload, future);
    			futures.add(future);
    		}

    		for(Future<String> f : futures) {
    			f.get(3, TimeUnit.SECONDS);
    		}
//			future2.get(2, TimeUnit.SECONDS);
    		//assertEquals("Hello Hiram!", future1.get(2, TimeUnit.SECONDS));

    		//assertEquals("Hello Hiram!", hello.protobuf(stringValue(payload)).getValue());
    	}
    	finally {
    		server.stop();
    		client.stop();
    	}
    }

    @Test
    public void testOverflow() throws Exception {

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
    		final Hello hello  = (Hello) Proxy.newProxyInstance(HelloImpl.class.getClassLoader(), new Class[] { Hello.class }, handler);

            final AtomicInteger requests = new AtomicInteger(0);
            final AtomicInteger responses = new AtomicInteger(0);
            final AtomicInteger failures = new AtomicInteger(0);

    		char[] chars = new char[65*1024];
    		final String payload = new String(chars);

            Thread[] threads = new Thread[BENCHMARK_CLIENTS];
            for (int t = 0; t < BENCHMARK_CLIENTS; t++) {
                threads[t] = new Thread() {
                    public void run() {
                        try {
                            requests.incrementAndGet();

                            hello.hello(payload);

                            responses.incrementAndGet();
                        } catch (Throwable t) {
                            failures.incrementAndGet();
                        }
                    }
                };
                threads[t].start();
            }

            for (int t = 0; t < BENCHMARK_CLIENTS; t++) {
                threads[t].join(10000);
                System.err.format("REQUEST: %d of %d%n", requests.get(), BENCHMARK_CLIENTS);
                System.err.format("RESPONSES: %d of %d%n", responses.get(), BENCHMARK_CLIENTS);
                assertEquals(threads[t].isAlive(), false);
            }

            assertEquals(BENCHMARK_CLIENTS, requests.get());
            assertEquals(BENCHMARK_CLIENTS, responses.get());
            assertEquals(0, failures.get());

    	}
    	finally {
    		server.stop();
    		client.stop();
    	}
    }

    @Test
    public void testNoOverflow() throws Exception {

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

    		char[] chars = new char[65*1024];
    		String payload = new String(chars);

    		for(int i = 0; i < 100; i++) {
    			hello.hello(payload);
    		}
    	}
    	finally {
    		server.stop();
    		client.stop();
    	}
    }

    @Test
    public void testUnderLoadSyncObject() throws Exception {
        HashMap<String, SerializationStrategy> map = new HashMap<String, SerializationStrategy>();

        DispatchQueue queue = Dispatch.createQueue();
        ServerInvokerImpl server = new ServerInvokerImpl("tcp://localhost:0", queue, map);
        server.start();
        ClientInvokerImpl client = new ClientInvokerImpl(queue, map);
        client.start();

        try {
            final HelloImpl helloImpl = new HelloImpl();
            server.registerService("service-id", new ServerInvoker.ServiceFactory() {
                public Object get() {
                    return helloImpl;
                }
                public void unget() {
                }
            }, HelloImpl.class.getClassLoader());


            InvocationHandler handler = client.getProxy(server.getConnectAddress(), "service-id", HelloImpl.class.getClassLoader());

            final Hello hello  = (Hello) Proxy.newProxyInstance(HelloImpl.class.getClassLoader(), new Class[] { Hello.class }, handler);

            assertEquals("Hello World!", hello.helloworld());

            final AtomicInteger requests = new AtomicInteger(0);
            final AtomicInteger failures = new AtomicInteger(0);
            final long latencies[] = new long[BENCHMARK_CLIENTS * BENCHMARK_INVOCATIONS_PER_CLIENT];

            final long start = System.nanoTime();
            Thread[] threads = new Thread[BENCHMARK_CLIENTS];
            for (int t = 0; t < BENCHMARK_CLIENTS; t++) {
                final int thread_idx = t;
                threads[t] = new Thread() {
                    public void run() {
                        for (int i = 0; i < BENCHMARK_INVOCATIONS_PER_CLIENT; i++) {
                            try {
                                requests.incrementAndGet();
                                String response;

                                final long start = System.nanoTime();
                                response = hello.hello("Fabric");
                                final long end = System.nanoTime();
                                latencies[(thread_idx* BENCHMARK_INVOCATIONS_PER_CLIENT)+i] = end-start;

                                assertEquals("Hello Fabric!", response);
                            } catch (Throwable t) {
                                latencies[(thread_idx* BENCHMARK_INVOCATIONS_PER_CLIENT)+i] = -1;
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

            for (int t = 0; t < BENCHMARK_CLIENTS; t++) {
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


    class AsyncClient implements AsyncCallback<StringValue.Getter> {

        final int thread_idx;
        final int nbInvocationsPerThread;
        final long latencies[];
        final AtomicInteger requests;
        final AtomicInteger failures;
        final Hello hello;

        final DispatchQueue queue = createQueue();
        final StringValue.Buffer msg = stringValue("Fabric").freeze();
        final CountDownLatch done = new CountDownLatch(1);

        int i;
        long start;


        AsyncClient(int thread_idx, int nbInvocationsPerThread, Hello hello, AtomicInteger failures, AtomicInteger requests, long[] latencies) {
            this.failures = failures;
            this.requests = requests;
            this.nbInvocationsPerThread = nbInvocationsPerThread;
            this.latencies = latencies;
            this.hello = hello;
            this.thread_idx = thread_idx;
        }

        void start() {
            queue.execute(new Runnable() {
                public void run() {
                    sendNext();
                }
            });
        }

        void join() throws InterruptedException {
            done.await();
        }

        private void sendNext() {
            if( i < nbInvocationsPerThread ) {
                requests.incrementAndGet();
                start = System.nanoTime();
                hello.protobuf(msg, this);
            } else {
                done.countDown();
            }
        }

        public void onSuccess(StringValue.Getter result) {
            latencies[(thread_idx*nbInvocationsPerThread)+i] = System.nanoTime() -start;
            i++;
            sendNext();
        }

        public void onFailure(Throwable t) {
            failures.incrementAndGet();
            latencies[(thread_idx*nbInvocationsPerThread)+i] = -1;
            i++;
            if (t instanceof UndeclaredThrowableException) {
                t = ((UndeclaredThrowableException) t).getUndeclaredThrowable();
            }
            System.err.println("Error: " + t.getClass().getName() + (t.getMessage() != null ? " (" + t.getMessage() + ")" : ""));
            sendNext();
        }
    }

    @Test
    public void testUnderLoadAsyncProto() throws Exception {
        HashMap<String, SerializationStrategy> map = new HashMap<String, SerializationStrategy>();
        map.put("protobuf", new ProtobufSerializationStrategy());

        DispatchQueue queue = Dispatch.createQueue();
        ServerInvokerImpl server = new ServerInvokerImpl("tcp://localhost:0", queue, map);
        server.start();
        ClientInvokerImpl client = new ClientInvokerImpl(queue, map);
        client.start();

        try {

            final HelloImpl helloImpl = new HelloImpl();
            server.registerService("service-id", new ServerInvoker.ServiceFactory() {
                public Object get() {
                    return helloImpl;
                }
                public void unget() {
                }
            }, HelloImpl.class.getClassLoader());


            InvocationHandler handler = client.getProxy(server.getConnectAddress(), "service-id", HelloImpl.class.getClassLoader());

            final Hello hello  = (Hello) Proxy.newProxyInstance(HelloImpl.class.getClassLoader(), new Class[] { Hello.class }, handler);

            final AtomicInteger requests = new AtomicInteger(0);
            final AtomicInteger failures = new AtomicInteger(0);
            final long latencies[] = new long[BENCHMARK_CLIENTS * BENCHMARK_INVOCATIONS_PER_CLIENT];

            final long start = System.nanoTime();
            AsyncClient[] threads = new AsyncClient[BENCHMARK_CLIENTS];
            for (int t = 0; t < BENCHMARK_CLIENTS; t++) {
                threads[t] = new AsyncClient(t, BENCHMARK_INVOCATIONS_PER_CLIENT, hello, failures, requests, latencies);
                threads[t].start();
            }

            for (int t = 0; t < BENCHMARK_CLIENTS; t++) {
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

        String helloworld();

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

        public String helloworld() {
            return "Hello World!";
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
