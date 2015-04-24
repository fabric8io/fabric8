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
package io.fabric8.etcd.impl.dsl;

import io.fabric8.etcd.api.Keys;
import io.fabric8.etcd.api.Response;
import io.fabric8.etcd.core.Operation;
import io.fabric8.etcd.core.OperationContext;
import io.fabric8.etcd.core.SynchronousExecution;
import io.fabric8.etcd.dsl.SetDataBuilder;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;

public class SetDataImpl implements Operation {

    public static class Builder implements SetDataBuilder {

        private final SynchronousExecution execution = new SynchronousExecution();
        private final OperationContext context;

        private String value;
        private boolean dir;
        private String prevValue;
        private int prevIndex;
        private boolean prevExists;
        private int ttl=-1;
        private boolean ordered;

        public Builder(OperationContext context) {
            this.context = context;
        }

        public Response forKey(final String key) {
            return execution.execute(context, new SetDataImpl(key, value, dir, prevValue, prevIndex, prevExists, ttl, ordered));
        }

        @Override
        public SetDataBuilder value(String value) {
            this.value = value;
            return this;
        }

        @Override
        public SetDataBuilder prevValue(String prevValue) {
            this.prevValue = prevValue;
            return this;
        }

        @Override
        public SetDataBuilder prevIndex(int prevIndex) {
            this.prevIndex = prevIndex;
            return this;
        }

        @Override
        public SetDataBuilder prevExists() {
            this.prevExists = true;
            return this;
        }

        @Override
        public SetDataBuilder dir() {
            this.dir = true;
            return this;
        }

        @Override
        public SetDataBuilder ttl(int ttl) {
            this.ttl = ttl;
            return this;
        }


        @Override
        public SetDataBuilder ordered() {
            this.ordered = true;
            return this;
        }
    }

    private final String key;
    private final String value;
    private final boolean dir;
    private final String prevValue;
    private final int prevIndex;
    private final boolean prevExists;
    private final int ttl;
    private final boolean ordered;


    public SetDataImpl(String key, String value, boolean dir, String prevValue, int prevIndex, boolean prevExists, int ttl, boolean ordered) {
        this.key = key;
        this.value = value;
        this.dir = dir;
        this.prevValue = prevValue;
        this.prevIndex = prevIndex;
        this.prevExists = prevExists;
        this.ttl = ttl;
        this.ordered = ordered;
    }

    @Override
    public HttpUriRequest createRequest(OperationContext context) {
        try {
            URIBuilder builder = new URIBuilder(context.getBaseUri())
                    .setPath(Keys.makeKey(key))
                    .addParameter("value", value)
                    .addParameter("dir", String.valueOf(dir))
                    .addParameter("ordered", String.valueOf(ordered))
                    .addParameter("prevExists", String.valueOf(prevExists));


            if (ttl >= 0) {
                builder = builder.addParameter("ttl", String.valueOf(ttl));
            }

            if (prevValue != null)  {
                builder = builder.addParameter("prevValue", prevValue)
                        .addParameter("prevIndex", String.valueOf(prevIndex));

            }

            return new HttpPut(builder.build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
