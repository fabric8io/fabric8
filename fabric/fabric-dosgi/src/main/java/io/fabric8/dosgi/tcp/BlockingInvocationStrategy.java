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
package io.fabric8.dosgi.tcp;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import io.fabric8.dosgi.api.AsyncCallback;
import io.fabric8.dosgi.api.SerializationStrategy;
import org.fusesource.hawtbuf.DataByteArrayInputStream;
import org.fusesource.hawtbuf.DataByteArrayOutputStream;
import org.fusesource.hawtdispatch.Dispatch;

/**
 * <p>
 * </p>
 *
 */
public class BlockingInvocationStrategy implements InvocationStrategy {

    public static final BlockingInvocationStrategy INSTANCE = new BlockingInvocationStrategy();

    private static final Callable<Object> EMPTY_CALLABLE = new Callable<Object>() {
        public Object call() {
            return null;
        }
    };

    private class BlockingResponseFuture extends FutureTask<Object> implements ResponseFuture, AsyncCallback {

        private final ClassLoader loader;
        private final Method method;
        private final SerializationStrategy serializationStrategy;

        public BlockingResponseFuture(ClassLoader loader, Method method, SerializationStrategy serializationStrategy) {
            super(EMPTY_CALLABLE);
            this.loader = loader;
            this.method = method;
            this.serializationStrategy = serializationStrategy;
        }

        public void set(DataByteArrayInputStream source) throws IOException, ClassNotFoundException {
            try {
                serializationStrategy.decodeResponse(loader, method.getReturnType(), source, this);
            } catch (Throwable e) {
                super.setException(e);
            }
        }

        public void fail(Throwable failure) {
            super.setException(failure);
        }

        public void onSuccess(Object result) {
            super.set(result);
        }

        public void onFailure(Throwable failure) {
            super.setException(failure);
        }
    }

    public ResponseFuture request(SerializationStrategy serializationStrategy, ClassLoader loader, Method method, Object[] args, DataByteArrayOutputStream target) throws Exception {

        assert Dispatch.getCurrentQueue() == null : "You should not do blocking RPC class when executing on a dispatch queue";

        serializationStrategy.encodeRequest(loader, method.getParameterTypes(), args, target);
        return new BlockingResponseFuture(loader, method, serializationStrategy);
    }

    public void service(SerializationStrategy serializationStrategy, ClassLoader loader, Method method, Object target, DataByteArrayInputStream requestStream, DataByteArrayOutputStream responseStream, Runnable onComplete) {

        int pos = responseStream.position();
        try {

            Object value = null;
            Throwable error = null;

            try {
                Class<?>[] types = method.getParameterTypes();
                final Object[] args = new Object[types.length];
                serializationStrategy.decodeRequest(loader, types, requestStream, args);
                value = method.invoke(target, args);
            } catch (Throwable t) {
                if (t instanceof InvocationTargetException) {
                    error = t.getCause();
                } else {
                    error = t;
                }
            }

            serializationStrategy.encodeResponse(loader, method.getReturnType(), value, error, responseStream);

        } catch(Exception e) {

            // we failed to encode the response.. reposition and write that error.
            try {
                responseStream.position(pos);
                serializationStrategy.encodeResponse(loader, method.getReturnType(), null, new RemoteException(e.toString()), responseStream);
            } catch (Exception unexpected) {
                unexpected.printStackTrace();
            }

        } finally {
            onComplete.run();
        }
    }

}
