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


import com.google.common.base.Function;
import io.fabric8.etcd.api.Response;
import org.apache.http.HttpResponse;
import org.apache.http.nio.client.HttpAsyncClient;

import java.net.URI;

public class OperationContextImpl  implements OperationContext{

    private final URI baseUri;
    private final HttpAsyncClient httpClient;
    private final Function<HttpResponse, Response> converter;

    public OperationContextImpl(URI baseUri, HttpAsyncClient httpClient, Function<HttpResponse, Response> converter) {
        this.baseUri = baseUri;
        this.httpClient = httpClient;
        this.converter = converter;
    }

    public URI getBaseUri() {
        return baseUri;
    }

    public HttpAsyncClient getHttpClient() {
        return httpClient;
    }

    public Function<HttpResponse, Response> getConverter() {
        return converter;
    }
}
