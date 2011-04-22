/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.dosgi.tcp;

import org.fusesource.fabric.dosgi.util.ClassLoaderObjectInputStream;
import org.fusesource.hawtbuf.DataByteArrayInputStream;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class BlockingInvocationStrategy implements InvocationStrategy {

    private static class ObjectResponseFuture extends FutureTask<Object> implements ResponseFuture {

        private static final Callable<Object> EMPTY_CALLABLE = new Callable<Object>() {
            public Object call() {
                return null;
            }
        };
        private final ClassLoader loader;

        public ObjectResponseFuture(ClassLoader loader) {
            super(EMPTY_CALLABLE);
            this.loader = loader;
        }

        public void set(DataByteArrayInputStream source) throws IOException, ClassNotFoundException {
            ClassLoaderObjectInputStream ois = new ClassLoaderObjectInputStream(source);
            ois.setClassLoader(loader);
            Throwable error = (Throwable) ois.readObject();
            Object value = ois.readObject();
            if (error != null) {
                super.setException(error);
            } else {
                super.set(value);
            }
        }

    }

    public ResponseFuture request(ClassLoader loader, Method method, Object[] args, DataByteArrayOutputStream target) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(target);
        oos.writeObject(args);
        oos.flush();
        return new ObjectResponseFuture(loader);
    }

    public void service(ClassLoader loader, Method method, Object target, DataByteArrayInputStream requestStream, DataByteArrayOutputStream responseStream, Runnable onComplete) {

        int pos = responseStream.position();
        try {

            Object value = null;
            Throwable error = null;

            try {
                final ClassLoaderObjectInputStream ois = new ClassLoaderObjectInputStream(requestStream);
                ois.setClassLoader(loader);
                final Object[] args = (Object[]) ois.readObject();
                value = method.invoke(target, args);
            } catch (Throwable t) {
                if (t instanceof InvocationTargetException) {
                    error = t.getCause();
                } else {
                    error = t;
                }
            }

            ObjectOutputStream oos = new ObjectOutputStream(responseStream);
            oos.writeObject(error);
            oos.writeObject(value);
            oos.flush();

        } catch(Exception e) {

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
