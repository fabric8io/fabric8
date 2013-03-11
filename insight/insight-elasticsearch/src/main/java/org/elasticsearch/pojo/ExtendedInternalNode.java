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

package org.elasticsearch.pojo;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalNode;
import org.fusesource.insight.elasticsearch.ElasticRest;
import org.fusesource.insight.elasticsearch.impl.ElasticRestImpl;
import org.fusesource.insight.elasticsearch.impl.ElasticStorageImpl;
import org.fusesource.insight.storage.StorageService;

import java.io.IOException;

/**
 * Instead of registering 3 different services, we use a single wrapper which delegate to the
 * three services.  It helps management of service registration.
 */
public class ExtendedInternalNode implements Node, ElasticRest, StorageService {

    private final InternalNode node;
    private final ElasticRestImpl rest;
    private final ElasticStorageImpl storage;

    public ExtendedInternalNode(InternalNode node) {
        this.node = node;
        this.rest = new ElasticRestImpl(node);
        this.storage = new ElasticStorageImpl(node);
    }

    @Override
    public Node start() {
        Node n = this.node.start();
        this.storage.init();
        return n;
    }

    @Override
    public Node stop() {
        this.storage.destroy();
        return this.node.stop();
    }

    @Override
    public void close() {
        this.storage.destroy();
        this.node.close();
    }

    @Override
    public Settings settings() {
        return this.node.settings();
    }

    @Override
    public Client client() {
        return this.node.client();
    }

    @Override
    public boolean isClosed() {
        return this.node.isClosed();
    }

    @Override
    public String get(String uri) throws IOException {
        return this.rest.get(uri);
    }

    @Override
    public String post(String uri, String content) throws IOException {
        return this.rest.post(uri, content);
    }

    @Override
    public String put(String uri, String content) throws IOException {
        return this.rest.put(uri, content);
    }

    @Override
    public String delete(String uri) throws IOException {
        return this.rest.delete(uri);
    }

    @Override
    public String head(String uri) throws IOException {
        return this.rest.head(uri);
    }

    @Override
    public void store(String type, long timestamp, String jsonData) {
        this.storage.store(type, timestamp, jsonData);
    }
}
