/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.dosgi.api;

import org.fusesource.fabric.dosgi.tcp.InvocationStrategy;
import org.fusesource.fabric.dosgi.tcp.ResponseFuture;
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
public class ObjectSerializationStrategy implements SerializationStrategy {
    public static final ObjectSerializationStrategy INSTANCE = new ObjectSerializationStrategy();

    public String name() {
        return "object";
    }

    public void encodeRequest(ClassLoader loader, Class<?>[] types, Object[] args, DataByteArrayOutputStream target) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(target);
        oos.writeObject(args);
        oos.flush();
    }

    public void decodeResponse(ClassLoader loader, Class<?> type, DataByteArrayInputStream source, AsyncCallback result) throws IOException, ClassNotFoundException {
        ClassLoaderObjectInputStream ois = new ClassLoaderObjectInputStream(source);
        ois.setClassLoader(loader);
        Throwable error = (Throwable) ois.readObject();
        Object value = ois.readObject();
        if (error != null) {
            result.onFailure(error);
        } else {
            result.onSuccess(value);
        }
    }

    public void decodeRequest(ClassLoader loader, Class<?>[] types, DataByteArrayInputStream source, Object[] target) throws IOException, ClassNotFoundException {
        final ClassLoaderObjectInputStream ois = new ClassLoaderObjectInputStream(source);
        ois.setClassLoader(loader);
        final Object[] args = (Object[]) ois.readObject();
        System.arraycopy(args, 0, target, 0, args.length);
    }


    public void encodeResponse(ClassLoader loader, Class<?> type, Object value, Throwable error, DataByteArrayOutputStream target) throws IOException, ClassNotFoundException {
        ObjectOutputStream oos = new ObjectOutputStream(target);
        oos.writeObject(error);
        oos.writeObject(value);
        oos.flush();
    }


}
