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
package io.fabric8.gateway.handlers.detecting;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Implements a Vertx handler which you can await() future results from.
 */
public class FutureHandler<T> implements Handler<T> {
    CountDownLatch done = new CountDownLatch(1);
    T event;

    @Override
    public void handle(T event) {
        this.event = event;
        done.countDown();
    }

    public T await() throws InterruptedException {
        done.await();
        return event;
    }

    public T await(long timeout, TimeUnit unit) throws InterruptedException {
        if( done.await(timeout, unit) ) {
            return event;
        } else {
            return null;
        }
    }


    public static <T> T result(FutureHandler<AsyncResult<T>> future) throws Exception {
        AsyncResult<T> asyncResult = future.await();
        if( asyncResult.failed() ) {
            throw new Exception(asyncResult.cause());
        } else {
            return asyncResult.result();
        }
    }
}
