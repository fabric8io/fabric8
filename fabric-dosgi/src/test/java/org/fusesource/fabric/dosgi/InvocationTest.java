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

import org.fusesource.fabric.dosgi.io.ServerInvoker;
import org.fusesource.fabric.dosgi.tcp.ClientInvokerImpl;
import org.fusesource.fabric.dosgi.tcp.ServerInvokerImpl;
import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InvocationTest {

    @Test
    public void testInvoke() throws Exception {

        DispatchQueue queue = Dispatch.createQueue();
        ServerInvokerImpl server = new ServerInvokerImpl("tcp://localhost:3252", queue);
        server.start();
        ClientInvokerImpl client = new ClientInvokerImpl(queue);
        client.start();

        server.registerService("service-id", new ServerInvoker.ServiceFactory() {
            public Object get() {
                return new HelloImpl();
            }
            public void unget() {
            }
        }, HelloImpl.class.getClassLoader());


        InvocationHandler handler = client.getProxy("tcp://localhost:3252", "service-id", HelloImpl.class.getClassLoader());
        Hello hello  = (Hello) Proxy.newProxyInstance(HelloImpl.class.getClassLoader(), new Class[] { Hello.class }, handler);

        assertEquals("Hello Fabric!", hello.hello("Fabric"));
    }


    public static interface Hello {
        String hello(String name);
    }

    public static class HelloImpl implements Hello {
        public String hello(String name) {
            return "Hello " + name + "!";
        }
    }
}
