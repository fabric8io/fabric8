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
import io.fabric8.etcd.dsl.DeleteDataBuilder;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;

public class DeleteDataImpl implements Operation {

    public static class Builder implements DeleteDataBuilder {

        private final SynchronousExecution execution = new SynchronousExecution();
        private final OperationContext context;

        private String key;
        private boolean dir;
        private String prevValue;
        private int prevIndex;
        private boolean prevExists;
        private boolean recursive;

        public Builder(OperationContext context) {
            this.context = context;
        }

        public Response forKey(final String key) {
            return execution.execute(context, new DeleteDataImpl(key, dir, prevValue, prevIndex, prevExists, recursive));
        }

        public Builder dir() {
            this.dir = true;
            return this;
        }

        public Builder prevValue(final String prevValue) {
            this.prevValue = prevValue;
            return this;
        }

        public Builder prevIndex(final int prevIndex) {
            this.prevIndex = prevIndex;
            return this;
        }

        public Builder prevExists() {
            this.prevExists = prevExists;
            return this;
        }

        public Builder recursive() {
            this.recursive = true;
            return this;
        }
    }

    private final String key;
    private final boolean dir;
    private final String prevValue;
    private final int prevIndex;
    private final boolean prevExists;
    private final boolean recursive;

    public DeleteDataImpl(String key, boolean dir, String prevValue, int prevIndex, boolean prevExists, boolean recursive) {
        this.key = key;
        this.dir = dir;
        this.prevValue = prevValue;
        this.prevIndex = prevIndex;
        this.prevExists = prevExists;
        this.recursive = recursive;
    }

    @Override
    public HttpUriRequest createRequest(OperationContext context) {
        try {
            URIBuilder builder = new URIBuilder(context.getBaseUri())
                    .setPath(Keys.makeKey(key))
                    .addParameter("dir", String.valueOf(dir))
                    .addParameter("recursive", String.valueOf(recursive))
                    .addParameter("prevExists", String.valueOf(prevExists));

            if (prevValue != null)  {
                builder = builder.addParameter("prevValue", prevValue)
                        .addParameter("prevIndex", String.valueOf(prevIndex));
            }

            return new HttpDelete(builder.build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
