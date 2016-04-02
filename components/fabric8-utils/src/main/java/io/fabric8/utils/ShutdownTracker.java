/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.utils;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A ShutdownTracker is used to track when a resource
 * is in use so that a shutdown action can occur
 * once all concurrent use of the resource has completed.
 *
 * Created by chirino on 6/23/14.
 */
public class ShutdownTracker {

    public static class ShutdownException extends IllegalStateException {}

    private AtomicInteger retained = new AtomicInteger(1);
    private AtomicBoolean stopping = new AtomicBoolean(false);
    private Runnable onStopCallback;

    /**
     * This method should be called before the resource is used.
     *
     * @throws ShutdownException once {@see shutdown} has been called.
     */
    public void retain() {
        if(!attemptRetain()) {
            throw new ShutdownException(); // fail the attempt at retaining.
        }
    }

    /**
     * Attempts to retain the the resources.
     *
     * @return false if the resource has already shutdown.
     */
    public boolean attemptRetain() {
        if( retained.getAndIncrement() == 0 || stopping.get() )  {
            retained.getAndDecrement(); // Undo the increment..
            return false;
        } else {
            return true;
        }
    }

    /**
     * This method should be called after a resource is no longer used.
     *
     * @throws IllegalStateException if an unbalanced number of release() calls are done in respect to retain() calls.
     */
    public void release() {
        if( retained.decrementAndGet()==0 ) {
            // not retained anymore? this should only happen when we are shutting down.
            if( !stopping.get() ) {
                throw new IllegalStateException("Unbalanced calls to release detected.");
            } else {
                if( onStopCallback!=null ) {
                    onStopCallback.run();
                    onStopCallback = null;
                }
            }
        }
    }

    /**
     * This is a helper method which calls {@see retain}/{@see release} before and after
     * a callable is called.
     *
     * @param callable
     * @param <T>
     * @return
     * @throws Exception
     */
    public <T> T use(Callable<T> callable) throws Exception {
        retain();
        try {
            return callable.call();
        } finally {
            retained.decrementAndGet();
        }
    }

    /**
     * Marks the resource as being in a shutdown state.  Once no more concurrent
     * users of the resource are active, the <code>onStopCallback</code> will
     * be run possibly after this method returns.
     *
     * @param onStopCallback if not null will be run once
     * @throws ShutdownException if {@see shutdown} has been previously called.
     */
    public void shutdown(Runnable onStopCallback) throws ShutdownException {
        if( stopping.compareAndSet(false, true) ) {
            this.onStopCallback = onStopCallback;
            release();
        } else {
            throw new ShutdownException();
        }
    }

    /**
     * Calls {@see shutdown} but waits for the shutdown to complete before
     * this method returns.
     *
     * @throws ShutdownException if preciously shutdown.
     * @throws InterruptedException if this thread is interrupted before the shutdown completes.
     */
    public void stop() throws ShutdownException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        shutdown(new Runnable(){
            @Override
            public void run() {
                latch.countDown();
            }
        });
        latch.await();
    }

    /**
     * Creates a dynamic proxy that implements all the interfaces of the target object
     * and which retains/releases this ShutdownTracker before/after method invocations
     * against the proxy.
     *
     * @param target
     * @return
     */
    public Object proxy(final Object target) {
        Class<?> targetClass = target.getClass();
        return Proxy.newProxyInstance(targetClass.getClassLoader(), targetClass.getInterfaces(), new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                retain();
                try {
                    return method.invoke(target, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                } finally {
                    release();
                }
            }
        });
    }

    public StandardMBean mbeanProxy(Object target) throws NotCompliantMBeanException {
        Class<?> targetClass = target.getClass();
        String targetClassName = targetClass.getSimpleName();
        for (Class clazz : targetClass.getInterfaces()) {
            if( clazz.getSimpleName().equals(targetClassName +"MBean")
                || clazz.getSimpleName().equals(targetClassName +"MXBean") ) {
                return new StandardMBean(proxy(target), clazz);
            }
        }
        throw new NotCompliantMBeanException();
    }

}
