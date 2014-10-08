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
import io.fabric8.etcd.api.EtcdException;
import io.fabric8.etcd.api.Response;
import io.fabric8.etcd.api.ResponseReader;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import java.io.IOException;

public class ToResponse implements Function<HttpResponse, Response> {

    private final ResponseReader reader;

    public ToResponse(ResponseReader reader) {
        this.reader = reader;
    }

    public Response apply(HttpResponse response) {
        HttpEntity entity = response.getEntity();
        Response result = null;
        try {
            result = reader.read(entity.getContent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (result.getErrorCode() != 0) {
            throw new EtcdException(result.getMessage(), result.getErrorCode());
        }
        return result;
    }
}
