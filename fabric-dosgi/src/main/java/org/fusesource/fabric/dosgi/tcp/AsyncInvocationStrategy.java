/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.dosgi.tcp;

import org.fusesource.fabric.dosgi.api.AsyncCallback;
import org.fusesource.fabric.dosgi.api.SerializationStrategy;
import org.fusesource.fabric.dosgi.util.ClassLoaderObjectInputStream;
import org.fusesource.hawtbuf.DataByteArrayInputStream;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;
import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.DispatchQueue;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.rmi.RemoteException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class AsyncInvocationStrategy implements InvocationStrategy {

    public static final AsyncInvocationStrategy INSTANCE = new AsyncInvocationStrategy();

    static public boolean isAsyncMethod(Method method) {
        Class<?>[] types = method.getParameterTypes();
        return types.length != 0 && types[types.length - 1] == AsyncCallback.class;
    }


    private class AsyncResponseFuture implements ResponseFuture {

        private final ClassLoader loader;
        private final Method method;
        private final AsyncCallback callback;
        private final SerializationStrategy serializationStrategy;
        private final DispatchQueue queue;

        public AsyncResponseFuture(ClassLoader loader, Method method, AsyncCallback callback, SerializationStrategy serializationStrategy, DispatchQueue queue) {
            this.loader = loader;
            this.method = method;
            this.callback = callback;
            this.serializationStrategy = serializationStrategy;
            this.queue = queue;
        }

        public void set(final DataByteArrayInputStream source) {
            if( queue!=null ) {
                queue.execute(new Runnable() {
                    public void run() {
                        decodeIt(source);
                    }
                });
            } else {
                decodeIt(source);
            }
        }

        private void decodeIt(DataByteArrayInputStream source) {
            try {
                serializationStrategy.decodeResponse(loader, getResultType(method), source, callback);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            // TODO: we could store the timeout so we can time out the async request...
            return null;
        }

    }

    public ResponseFuture request(SerializationStrategy serializationStrategy, ClassLoader loader, Method method, Object[] args, DataByteArrayOutputStream target) throws Exception {
        if(!isAsyncMethod(method)) {
            throw new IllegalArgumentException("Invalid async method declaration: last argument is not a RequestCallback");
        }

        Class[] new_types = payloadTypes(method);
        Object[] new_args = new Object[args.length-1];
        System.arraycopy(args, 0, new_args, 0, new_args.length);

        serializationStrategy.encodeRequest(loader, new_types, new_args, target);

        return new AsyncResponseFuture(loader, method, (AsyncCallback) args[args.length-1], serializationStrategy, Dispatch.getCurrentQueue());
    }

    static private Class<?>[] payloadTypes(Method method) {
        Class<?>[] types = method.getParameterTypes();
        Class<?>[] new_types = new Class<?>[types.length-1];
        System.arraycopy(types, 0, new_types, 0, new_types.length);
        return new_types;
    }

    static private Class getResultType(Method method) {
        Type[] types = method.getGenericParameterTypes();
        ParameterizedType t = (ParameterizedType) types[types.length-1];
        return (Class) t.getActualTypeArguments()[0];
    }


    class ServiceResponse {

        private final ClassLoader loader;
        private final Method method;
        private final DataByteArrayOutputStream responseStream;
        private final Runnable onComplete;
        private final SerializationStrategy serializationStrategy;
        private final int pos;
        // Used to protect against sending multiple responses.
        final AtomicBoolean responded = new AtomicBoolean(false);

        public ServiceResponse(ClassLoader loader, Method method, DataByteArrayOutputStream responseStream, Runnable onComplete, SerializationStrategy serializationStrategy) {
            this.loader = loader;
            this.method = method;
            this.responseStream = responseStream;
            this.onComplete = onComplete;
            this.serializationStrategy = serializationStrategy;
            pos = responseStream.position();
        }

        public void send(Throwable error, Object value) {
            if( responded.compareAndSet(false, true) ) {
                Class resultType = getResultType(method);
                try {
                    serializationStrategy.encodeResponse(loader, resultType, value, error, responseStream);
                } catch (Exception e) {
                    // we failed to encode the response.. reposition and write that error.
                    try {
                        responseStream.position(pos);
                        serializationStrategy.encodeResponse(loader, resultType, value, new RemoteException(e.toString()), responseStream);
                    } catch (Exception unexpected) {
                        unexpected.printStackTrace();
                    }
                } finally {
                    onComplete.run();
                }
            }
        }


    }
    public void service(SerializationStrategy serializationStrategy, ClassLoader loader, Method method, Object target, DataByteArrayInputStream requestStream, final DataByteArrayOutputStream responseStream, final Runnable onComplete) {

        final ServiceResponse helper = new ServiceResponse(loader, method, responseStream, onComplete, serializationStrategy);
        try {

            Object[] new_args = new Object[method.getParameterTypes().length];
            serializationStrategy.decodeRequest(loader, payloadTypes(method), requestStream, new_args);
            new_args[new_args.length-1] = new AsyncCallback<Object>() {
                public void onSuccess(Object result) {
                    helper.send(null, result);
                }
                public void onFailure(Throwable failure) {
                    helper.send(failure, null);
                }
            };
            method.invoke(target, new_args);

        } catch (Throwable t) {
            helper.send(t, null);
        }

    }

}
