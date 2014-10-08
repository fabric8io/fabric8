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

import com.google.common.io.Closeables;
import io.fabric8.etcd.api.EtcdClient;
import io.fabric8.etcd.api.ResponseReader;
import io.fabric8.etcd.dsl.DeleteDataBuilder;
import io.fabric8.etcd.dsl.GetDataBuilder;
import io.fabric8.etcd.dsl.SetDataBuilder;
import io.fabric8.etcd.impl.dsl.DeleteDataImpl;
import io.fabric8.etcd.impl.dsl.GetDataImpl;
import io.fabric8.etcd.impl.dsl.SetDataImpl;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

import java.io.IOException;
import java.net.URI;

public class EtcdClientImpl implements EtcdClient {

    public static class Builder implements io.fabric8.etcd.api.Builder<EtcdClientImpl> {
        private URI baseUri;
        private ResponseReader responseReader;

        public Builder baseUri(final URI baseUri) {
            this.baseUri = baseUri;
            return this;
        }

        public Builder responseReader(final ResponseReader responseReader) {
            this.responseReader = responseReader;
            return this;
        }


        @Override
        public EtcdClientImpl build() {
            return new EtcdClientImpl(baseUri, responseReader);
        }
    }

    private final OperationContext context;
    private final CloseableHttpAsyncClient client;

    public EtcdClientImpl(URI baseUri, ResponseReader responseReader) {
        this.client = HttpAsyncClients.custom().build();
        this.context = new OperationContextImpl(baseUri, client, new ToResponse(responseReader));
    }


    @Override
    public void start() {
        client.start();
    }

    @Override
    public void stop() {
        try {
            Closeables.close(client, true);
        } catch (IOException e) {
            //ignore
        }
    }

    @Override
    public GetDataBuilder getData() {
        return new GetDataImpl.Builder(context);
    }

    @Override
    public SetDataBuilder setData() {
        return new SetDataImpl.Builder(context);
    }

    @Override
    public DeleteDataBuilder delete() {
        return new DeleteDataImpl.Builder(context);
    }
}
