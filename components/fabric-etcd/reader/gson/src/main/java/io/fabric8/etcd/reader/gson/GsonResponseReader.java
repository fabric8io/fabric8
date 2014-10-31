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
package io.fabric8.etcd.reader.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import io.fabric8.etcd.api.Response;
import io.fabric8.etcd.core.ImmutableResponse;
import io.fabric8.etcd.api.ResponseReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class GsonResponseReader implements ResponseReader {

    private static final Gson GSON = new GsonBuilder().create();

    @Override
    public Response read(InputStream is) throws IOException {
        Response response = null;
        try (InputStreamReader isr = new InputStreamReader(is); JsonReader reader = new JsonReader(isr)) {
            response = GSON.fromJson(reader, ImmutableResponse.class);
        }
        return response;
    }
}
