/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.dosgi.api;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class RequestCallbackFuture<T> extends FutureTask<T> implements RequestCallback<T> {

    public RequestCallbackFuture() {
        super(new Callable<T>() {
            public T call() {
                return null;
            }
        });
    }

    public void onSuccess(T result) {
        super.set(result);
    }

    public void onFailure(Throwable failure) {
        super.setException(failure);
    }
}