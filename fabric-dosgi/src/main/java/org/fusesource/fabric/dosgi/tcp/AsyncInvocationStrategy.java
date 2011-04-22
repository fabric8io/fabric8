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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
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

    static public boolean isAsyncMethod(Method method) {
        Class<?>[] types = method.getParameterTypes();
        return types.length != 0 && types[types.length - 1] == AsyncCallback.class;
    }


    final SerializationStrategy serializationStrategy;

    public AsyncInvocationStrategy(SerializationStrategy serializationStrategy) {
        this.serializationStrategy = serializationStrategy;
    }

    private class AsyncResponseFuture implements ResponseFuture {

        private final ClassLoader loader;
        private final Method method;
        private final AsyncCallback callback;

        public AsyncResponseFuture(ClassLoader loader, Method method, AsyncCallback callback) {
            this.loader = loader;
            this.method = method;
            this.callback = callback;
        }

        public void set(DataByteArrayInputStream source) throws IOException, ClassNotFoundException {
            try {
                serializationStrategy.decodeResponse(loader, method.getReturnType(), source, callback);
            } catch (Throwable e) {
                e.printStackTrace(); // come on app.. avoid throwing us your exceptions will ya?
            }
        }

        public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            // TODO: we could store the timeout so we can time out the async request...
            return null;
        }

    }

    public ResponseFuture request(ClassLoader loader, Method method, Object[] args, DataByteArrayOutputStream target) throws Exception {
        if(!isAsyncMethod(method)) {
            throw new IllegalArgumentException("Invalid async method declaration: last argument is not a RequestCallback");
        }

        Class[] new_types = payloadTypes(method);
        Object[] new_args = new Object[args.length-1];
        System.arraycopy(args, 0, new_args, 0, new_args.length);

        serializationStrategy.encodeRequest(loader, new_types, new_args, target);

        return new AsyncResponseFuture(loader, method, (AsyncCallback) args[args.length-1]);
    }

    static private Class<?>[] payloadTypes(Method method) {
        Class<?>[] types = method.getParameterTypes();
        Class<?>[] new_types = new Class<?>[types.length-1];
        System.arraycopy(types, 0, new_types, 0, new_types.length);
        return new_types;
    }

    class ServiceResponse {

        private final ClassLoader loader;
        private final Method method;
        private final DataByteArrayOutputStream responseStream;
        private final Runnable onComplete;
        private final int pos;
        // Used to protect against sending multiple responses.
        final AtomicBoolean responded = new AtomicBoolean(false);

        public ServiceResponse(ClassLoader loader, Method method, DataByteArrayOutputStream responseStream, Runnable onComplete) {
            this.loader = loader;
            this.method = method;
            this.responseStream = responseStream;
            this.onComplete = onComplete;
            pos = responseStream.position();
        }

        public void send(Throwable error, Object value) {
            if( responded.compareAndSet(false, true) ) {
                try {
                    serializationStrategy.encodeResponse(loader, method.getReturnType(), value, error, responseStream);
                } catch (Exception e) {
                    // we failed to encode the response.. reposition and write that error.
                    try {
                        responseStream.position(pos);
                        serializationStrategy.encodeResponse(loader, method.getReturnType(), value, new RemoteException(e.toString()), responseStream);
                    } catch (Exception unexpected) {
                        unexpected.printStackTrace();
                    }
                } finally {
                    onComplete.run();
                }
            }
        }

    }
    public void service(ClassLoader loader, Method method, Object target, DataByteArrayInputStream requestStream, final DataByteArrayOutputStream responseStream, final Runnable onComplete) {

        final ServiceResponse helper = new ServiceResponse(loader, method, responseStream, onComplete);
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
