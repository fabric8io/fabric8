/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.agent.model;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URI;

public class Repository {

    private final URI uri;
    private Features features;

    public Repository(URI uri) {
        this.uri = uri;
    }

    public URI getURI() {
        return uri;
    }

    public String getName() throws IOException {
        load();
        return features.getName();
    }

    public URI[] getRepositories() throws IOException {
        load();
        URI[] result = new URI[features.getRepository().size()];
        for (int i = 0; i < features.getRepository().size(); i++) {
            String uri = features.getRepository().get(i);
            uri = uri.trim();
            result[i] = URI.create(uri);
        }
        return result;
    }

    public Feature[] getFeatures() throws IOException {
        load();
        return features.getFeature().toArray(new Feature[features.getFeature().size()]);
    }


    public void load() throws IOException {
        load(false);
    }

    public void load(boolean validate) throws IOException {
        if (features == null) {
            try (
                    InputStream inputStream = new InterruptibleInputStream(uri.toURL().openStream())
            ) {
                load(inputStream, validate);
            }
        }
    }

    public void load(InputStream inputStream, boolean validate) throws IOException {
        try {
            features = JaxbUtil.unmarshal(uri.toASCIIString(), inputStream, validate);
        } catch (Exception e) {
            throw (IOException) new IOException(e.getMessage() + " : " + uri).initCause(e);
        }
    }

    static class InterruptibleInputStream extends FilterInputStream {
        InterruptibleInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedIOException();
            }
            return super.read(b, off, len);
        }
    }

}

