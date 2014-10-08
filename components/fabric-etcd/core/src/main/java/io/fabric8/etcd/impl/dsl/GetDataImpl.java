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

package io.fabric8.etcd.impl.dsl;

import io.fabric8.etcd.api.Keys;
import io.fabric8.etcd.api.Response;
import io.fabric8.etcd.core.AsynchronousExecution;
import io.fabric8.etcd.core.Operation;
import io.fabric8.etcd.core.OperationContext;
import io.fabric8.etcd.core.SynchronousExecution;
import io.fabric8.etcd.dsl.AsyncGetDataBuilder;
import io.fabric8.etcd.dsl.GetDataBuilder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;

import java.util.concurrent.Future;

public class GetDataImpl implements Operation {

    public static class Builder implements GetDataBuilder {

        private final SynchronousExecution execution = new SynchronousExecution();
        private final OperationContext context;

        private boolean recursive;
        private boolean sorted;
        private int waitIndex;

        public Builder(OperationContext context) {
            this.context = context;
        }

        public Response forKey(final String key) {
            return execution.execute(context, new GetDataImpl(key, recursive, sorted, false, 0));
        }

        public Builder recursive() {
            this.recursive = true;
            return this;
        }

        public Builder sorted() {
            this.sorted = true;
            return this;
        }

        public AsyncGetDataBuilder waitIndex(final int waitIndex) {
            this.waitIndex = waitIndex;
            return new AsyncBuilder(context, this);
        }

        @Override
        public AsyncGetDataBuilder watch() {
            return new AsyncBuilder(context, this);
        }
    }

    private static class AsyncBuilder implements AsyncGetDataBuilder {

        private final OperationContext context;
        private final AsynchronousExecution execution = new AsynchronousExecution();

        private boolean recursive;
        private boolean sorted;
        private int waitIndex;

        public AsyncBuilder(OperationContext context, Builder builder) {
            this.context = context;
            this.recursive = builder.recursive;
            this.sorted = builder.sorted;
            this.waitIndex = builder.waitIndex;
        }

        public Future<Response> forKey(final String key) {
            return execution.execute(context, new GetDataImpl(key, recursive, sorted, true, waitIndex));
        }

        public AsyncBuilder recursive() {
            this.recursive = true;
            return this;
        }

        public AsyncBuilder sorted() {
            this.sorted = true;
            return this;
        }
    }

    private final String key;
    private final boolean recursive;
    private final boolean sorted;
    private final boolean shouldWait;
    private final int waitIndex;


    public GetDataImpl(String key, boolean recursive, boolean sorted, boolean shouldWait, int waitIndex) {
        this.key = key;
        this.recursive = recursive;
        this.sorted = sorted;
        this.shouldWait = shouldWait;
        this.waitIndex = waitIndex;
    }

    @Override
    public HttpUriRequest createRequest(OperationContext context) {
        try {
            URIBuilder builder = new URIBuilder(context.getBaseUri()).setPath(Keys.makeKey(key))
                    .addParameter("recursive", String.valueOf(recursive))
                    .addParameter("sorted", String.valueOf(sorted));

            if (shouldWait) {
                builder = builder.addParameter("wait", "true");
                if (waitIndex > 0) {
                    builder = builder.addParameter("waitIndex", String.valueOf(waitIndex));
                }
            }
            return new HttpGet(builder.build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
