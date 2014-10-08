/*
 * Copyright 2005-2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.etcd.core;

import com.google.common.util.concurrent.Futures;
import io.fabric8.etcd.api.Response;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;

import java.util.concurrent.Future;

public class AsynchronousExecution implements Execution<Future<Response>> {

    private static final FutureCallback<HttpResponse> FUTURE_CALLBACK = new FutureCallback<HttpResponse>() {
        @Override
        public void completed(HttpResponse result) {
        }

        @Override
        public void failed(Exception ex) {
        }

        @Override
        public void cancelled() {
        }
    };

    @Override
    public Future<Response> execute(final OperationContext context, Operation operation) {
        Future<HttpResponse> httpResponseFuture = context.getHttpClient().execute(operation.createRequest(context), FUTURE_CALLBACK);
        return Futures.lazyTransform(httpResponseFuture, context.getConverter());
    }
}
