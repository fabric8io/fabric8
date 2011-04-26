/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.dosgi.api;

import org.fusesource.fabric.dosgi.util.ClassLoaderObjectInputStream;
import org.fusesource.hawtbuf.DataByteArrayInputStream;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;
import org.fusesource.hawtbuf.proto.MessageBuffer;
import org.fusesource.hawtbuf.proto.PBMessage;
import org.fusesource.hawtbuf.proto.PBMessageFactory;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ProtobufSerializationStrategy implements SerializationStrategy {

    public static final ProtobufSerializationStrategy INSTANCE = new ProtobufSerializationStrategy();

    public String name() {
        return "protobuf";
    }

    private void encodeProtobuf(Class<?> type, Object arg, DataByteArrayOutputStream target) throws IOException {
        if( !PBMessage.class.isAssignableFrom(type) ) {
            throw new IllegalArgumentException("Invalid "+name()+" serialization method: method argument not a "+PBMessage.class.getName());
        }
        PBMessage msg = (PBMessage) arg;
        if( msg==null ) {
            return;
        }
        msg.freeze().writeUnframed(target);
    }

    private Object decodeProtobuf(Class<?> type, DataByteArrayInputStream source) throws IllegalAccessException, NoSuchFieldException, IOException {
        if( !PBMessage.class.isAssignableFrom(type) ) {
            throw new IllegalArgumentException("Invalid "+name()+" serialization method: method argument not a "+PBMessage.class.getName());
        }

        // Get the factory instance...
        PBMessageFactory factory = (PBMessageFactory) type.getEnclosingClass().getField("FACTORY").get(null);
        PBMessage msg = factory.parseUnframed(source);
        String name = type.getName();
        Object rc;
        if( name.endsWith("$Getter") || name.endsWith("$Buffer") ) {
            // Interface is ok we us giving them a read only impl.
            rc = msg;
        } else {
            // They want a read/write impl.
            rc = msg.copy();
        }
        return rc;
    }

    public void encodeRequest(ClassLoader loader, Class<?>[] types, Object[] args, DataByteArrayOutputStream target) throws IOException {
        if( types.length == 0 ) {
            return;
        } else if( types.length == 1 ) {
            encodeProtobuf(types[0], args[0], target);
        } else {
            throw new IllegalArgumentException("Invalid "+name()+" serialization method: methods must have zero or one argument.");
        }
    }

    public void decodeRequest(ClassLoader loader, Class<?>[] types, DataByteArrayInputStream source, Object[] target) throws IOException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        if( types.length == 0 ) {
            return;
        } else if( types.length == 1 ) {
            target[0] = decodeProtobuf(types[0], source);
        } else {
            throw new IllegalArgumentException("Invalid "+name()+" serialization method: methods must have zero or one argument.");
        }
    }

    public void encodeResponse(ClassLoader loader, Class<?> type, Object value, Throwable error, DataByteArrayOutputStream target) throws IOException, ClassNotFoundException {
        if( error!=null ) {
            target.writeBoolean(true);
            target.writeUTF(error.getClass().getName());
            target.writeUTF(error.getMessage());
        } else {
            target.writeBoolean(false);
            encodeProtobuf(type, value, target);
        }
    }

    public void decodeResponse(ClassLoader loader, Class<?> type, DataByteArrayInputStream source, AsyncCallback result) throws IOException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        if( source.readBoolean() ) {
            String className = source.readUTF();
            String message = source.readUTF();

            Throwable error;
            try {
                // try to build the exception...
                Constructor<?> ctr = loader.loadClass(className).getConstructor(new Class[]{String.class});
                error = (Throwable) ctr.newInstance(message);
            } catch (Throwable e) {
                // fallback to something simple..
                error = new RuntimeException(className+": "+message);
            }
            result.onFailure(error);

        } else {
            result.onSuccess(decodeProtobuf(type, source));
        }

    }




}
