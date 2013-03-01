package org.elasticsearch.restjmx;

import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.jmx.MBean;
import org.elasticsearch.jmx.ManagedOperation;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.support.AbstractRestRequest;
import org.elasticsearch.rest.support.RestUtils;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.Map;

@MBean(objectName = "service=restjmx", description = "RestJmx")
public class RestJmxManagement {

    private final RestController controller;

    @Inject
    public RestJmxManagement(RestController controller) {
        this.controller = controller;
    }

    @ManagedOperation
    public String exec(String method, String resource, String content) throws IOException {
        Channel channel = new Channel();
        synchronized (channel) {
            controller.dispatchRequest(new Request(method, resource, content), channel);
            if (channel.getResponse() == null) {
                try {
                    channel.wait();
                } catch (InterruptedException e) {
                    throw (IOException) new InterruptedIOException().initCause(e);
                }
            }
            String result = new String(channel.getResponse().content(), 0, channel.getResponse().contentLength());
            if (channel.getResponse().status().getStatus() >= 400) {
                throw new IOException(result);
            } else {
                return result;
            }
        }
    }

    static class Request extends AbstractRestRequest implements RestRequest {

        private final String method;
        private final String uri;
        private final String content;
        private final Map<String, String> params;
        private final String rawPath;
        private final byte[] rawContent;

        Request(String method, String uri, String content) {
            this.method = method;
            this.uri = uri;
            this.content = content;

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

    static class Channel implements RestChannel {

        private RestResponse response;

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
