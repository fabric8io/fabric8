/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.gateway.handlers.http.rule;

import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerFileUpload;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.http.HttpVersion;
import org.vertx.java.core.net.NetSocket;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.X509Certificate;
import java.net.InetSocketAddress;
import java.net.URI;

/**
 * A dummy implementation of {@link HttpServerRequest} for testing
 */
public class StubHttpServletRequest implements HttpServerRequest {
    private final String uri;
    private String method = "GET";
    private String path;
    private HttpVersion version;
    private HttpServerResponse response;
    private MultiMap headers;
    private MultiMap params;
    private InetSocketAddress remoteAddress;
    private NetSocket netSocket;
    private MultiMap formAttributes;
    private URI absoluteURI;

    public StubHttpServletRequest(String uri) {
        this.uri = uri;
    }

    @Override
    public HttpServerRequest exceptionHandler(Handler<Throwable> handler) {
        return this;
    }

    @Override
    public HttpVersion version() {
        return version;
    }

    @Override
    public String method() {
        return method;
    }

    @Override
    public String uri() {
        return uri;
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public String query() {
        // TODO
        return null;
    }

    @Override
    public HttpServerResponse response() {
        return response;
    }

    @Override
    public MultiMap headers() {
        return headers;
    }

    @Override
    public MultiMap params() {
        return params;
    }

    @Override
    public InetSocketAddress remoteAddress() {
        // TODO
        return remoteAddress;
    }

    @Override
    public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
        return new X509Certificate[0];
    }

    @Override
    public URI absoluteURI() {
        return absoluteURI;
    }

    @Override
    public HttpServerRequest bodyHandler(Handler<Buffer> bodyHandler) {
        // TODO
        return this;
    }

    @Override
    public NetSocket netSocket() {
        return netSocket;
    }

    @Override
    public HttpServerRequest expectMultiPart(boolean expect) {
        // TODO
        return this;
    }

    @Override
    public HttpServerRequest uploadHandler(Handler<HttpServerFileUpload> uploadHandler) {
        // TODO
        return this;
    }

    @Override
    public MultiMap formAttributes() {
        return formAttributes;
    }

    @Override
    public HttpServerRequest dataHandler(Handler<Buffer> handler) {
        // TODO
        return null;
    }

    @Override
    public HttpServerRequest pause() {
        // TODO
        return this;
    }

    @Override
    public HttpServerRequest resume() {
        // TODO
        return this;
    }

    @Override
    public HttpServerRequest endHandler(Handler<Void> endHandler) {
        // TODO
        return this;
    }

    // Properties
    //-------------------------------------------------------------------------

    public String getUri() {
        return uri;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public HttpVersion getVersion() {
        return version;
    }

    public void setVersion(HttpVersion version) {
        this.version = version;
    }

    public HttpServerResponse getResponse() {
        return response;
    }

    public void setResponse(HttpServerResponse response) {
        this.response = response;
    }

    public MultiMap getHeaders() {
        return headers;
    }

    public void setHeaders(MultiMap headers) {
        this.headers = headers;
    }

    public MultiMap getParams() {
        return params;
    }

    public void setParams(MultiMap params) {
        this.params = params;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public NetSocket getNetSocket() {
        return netSocket;
    }

    public void setNetSocket(NetSocket netSocket) {
        this.netSocket = netSocket;
    }

    public MultiMap getFormAttributes() {
        return formAttributes;
    }

    public void setFormAttributes(MultiMap formAttributes) {
        this.formAttributes = formAttributes;
    }

    public URI getAbsoluteURI() {
        return absoluteURI;
    }

    public void setAbsoluteURI(URI absoluteURI) {
        this.absoluteURI = absoluteURI;
    }
}
