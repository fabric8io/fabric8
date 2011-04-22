/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.dosgi.tcp;

import org.fusesource.fabric.dosgi.api.RequestCallback;
import org.fusesource.fabric.dosgi.api.RequestCodecStrategy;
import org.fusesource.fabric.dosgi.api.ResponseFuture;
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
public class AsyncObjectSerializationStrategy implements RequestCodecStrategy {

    static public boolean isAsyncMethod(Method method) {
        Class<?>[] types = method.getParameterTypes();
        return types.length != 0 && types[types.length - 1] == RequestCallback.class;
    }

    private static class ObjectResponseFuture extends FutureTask<Object> implements ResponseFuture {

        private static final Callable<Object> EMPTY_CALLABLE = new Callable<Object>() {
            public Object call() {
                return null;
            }
        };
        private final ClassLoader loader;
        private final RequestCallback callback;

        public ObjectResponseFuture(ClassLoader loader, RequestCallback callback) {
            super(EMPTY_CALLABLE);
            this.loader = loader;
            this.callback = callback;
        }

        public void set(DataByteArrayInputStream source) throws IOException, ClassNotFoundException {
            ClassLoaderObjectInputStream ois = new ClassLoaderObjectInputStream(source);
            ois.setClassLoader(loader);
            Throwable error = (Throwable) ois.readObject();
            Object value = ois.readObject();
            try {
                if (error != null) {
                    callback.onFailure(error);
                } else {
                    callback.onSuccess(value);
                }
            } catch (Throwable e) {
                e.printStackTrace(); // come on app.. avoid throwing us your exceptions will ya?
            }
        }

        @Override
        public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            // TODO: we could store the timeout so we can time out the async request...
            return null;
        }

        @Override
        public Object get() throws InterruptedException, ExecutionException {
            return null;
        }
    }

    public ResponseFuture request(ClassLoader loader, Method method, Object[] args, DataByteArrayOutputStream target) throws IOException {
        if(!isAsyncMethod(method)) {
            throw new IllegalArgumentException("Invalid async method declaration: last argument is not a RequestCallback");
        }

        ObjectOutputStream oos = new ObjectOutputStream(target);
        Object[] new_args = new Object[args.length-1];
        System.arraycopy(args, 0, new_args, 0, new_args.length);
        oos.writeObject(new_args);
        oos.flush();

        return new ObjectResponseFuture(loader, (RequestCallback) args[args.length-1]);
    }

    static class ServiceResponse {

        private final DataByteArrayOutputStream responseStream;
        private final Runnable onComplete;
        private final int pos;
        // Used to protect against sending multiple responses.
        final AtomicBoolean responded = new AtomicBoolean(false);

        public ServiceResponse(DataByteArrayOutputStream responseStream, Runnable onComplete) {
            this.responseStream = responseStream;
            this.onComplete = onComplete;
            pos = responseStream.position();
        }

        public void send(Throwable error, Object value) {
            if( responded.compareAndSet(false, true) ) {
                try {
                    ObjectOutputStream oos = new ObjectOutputStream(responseStream);
                    oos.writeObject(error);
                    oos.writeObject(value);
                    oos.flush();
                } catch (IOException e) {
                    // we failed to encode the response.. reposition and write that error.
                    try {
                        responseStream.position(pos);
                        ObjectOutputStream oos = new ObjectOutputStream(responseStream);
                        oos.writeObject(new RemoteException(e.toString()));
                        oos.writeObject(null);
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

        final ServiceResponse helper = new ServiceResponse(responseStream, onComplete);
        try {
            final ClassLoaderObjectInputStream ois = new ClassLoaderObjectInputStream(requestStream);
            ois.setClassLoader(loader);
            final Object[] args = (Object[]) ois.readObject();
            Object[] new_args = new Object[args.length+1];
            System.arraycopy(args, 0, new_args, 0, args.length);
            new_args[new_args.length-1] = new RequestCallback<Object>() {
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
