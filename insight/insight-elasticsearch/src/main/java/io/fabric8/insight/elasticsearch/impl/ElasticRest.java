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
package io.fabric8.insight.elasticsearch.impl;

import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.node.internal.InternalNode;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.support.RestUtils;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ElasticRest implements ElasticRestMBean {

    private InternalNode node;
    private RestController controller;

    public ElasticRest() {
    }

    public ElasticRest(InternalNode node) {
        setNode(node);
    }

    public void setNode(InternalNode node) {
        this.node = node;
        this.controller = node.injector().getInstance(RestController.class);
    }

    public String get(String uri) throws IOException {
        return exec("GET", uri, null);
    }

    public String post(String uri, String content) throws IOException {
        return exec("POST", uri, content);
    }

    public String put(String uri, String content) throws IOException {
        return exec("PUT", uri, content);
    }

    public String delete(String uri) throws IOException {
        return exec("DELETE", uri, null);
    }

    public String head(String uri) throws IOException {
        return exec("HEAD", uri, null);
    }

    @Override
    public String exec(String method, String resource, String content) throws IOException {
        Request request = new Request(method, resource, content);
        Channel channel = new Channel(request);
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (channel) {
            controller.dispatchRequest(new Request(method, resource, content), channel);
            if (channel.getResponse() == null) {
                try {
                    channel.wait();
                } catch (InterruptedException e) {
                    throw (IOException) new InterruptedIOException().initCause(e);
                }
            }
            String result = channel.getResponse().content().toUtf8();
            if (channel.getResponse().status().getStatus() >= 400) {
                throw new IOException(result);
            } else {
                return result;
            }
        }
    }

    static class Request extends RestRequest {

        private final String method;
        private final String uri;
        private final Map<String, String> params;
        private final String rawPath;
        private final byte[] rawContent;

        Request(String method, String uri, String content) {
            this.method = method;
            this.uri = uri == null ? "" : uri;

            this.params = new HashMap<String, String>();
            int pathEndPos = uri.indexOf('?');
            if (pathEndPos < 0) {
                this.rawPath = uri;
            } else {
                this.rawPath = uri.substring(0, pathEndPos);
                RestUtils.decodeQueryString(uri, pathEndPos + 1, params);
            }

            this.rawContent = content != null ? content.getBytes() : null;
        }

        @Override
        public Method method() {
            return Method.valueOf(method.toUpperCase());
        }

        @Override
        public String uri() {
            return uri;
        }

        @Override
        public String rawPath() {
            return rawPath;
        }

        @Override
        public boolean hasContent() {
            return rawContent != null;
        }

        @Override
        public boolean contentUnsafe() {
            return false;
        }

        @Override
        public BytesReference content() {
            return new BytesArray(rawContent);
        }

        @Override
        public Iterable<Map.Entry<String, String>> headers() {
            return Collections.<String, String>emptyMap().entrySet();
        }

        @Override
        public String header(String name) {
            return null;
        }

        @Override
        public boolean hasParam(String key) {
            return params.containsKey(key);
        }

        @Override
        public String param(String key) {
            return params.get(key);
        }

        @Override
        public Map<String, String> params() {
            return params;
        }

        @Override
        public String param(String key, String defaultValue) {
            if (params.containsKey(key)) {
                return params.get(key);
            } else {
                return defaultValue;
            }
        }

    }

    static class Channel extends RestChannel {

        private RestResponse response;

        Channel(RestRequest request) {
            super(request);
        }

        @Override
        public synchronized void sendResponse(RestResponse response) {
            if (response.status() != RestStatus.CONTINUE) {
                this.response = response;
                this.notifyAll();
            }
        }

        public RestResponse getResponse() {
            return response;
        }
    }
}
