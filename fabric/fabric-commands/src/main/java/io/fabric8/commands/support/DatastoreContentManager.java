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
package io.fabric8.commands.support;


import io.fabric8.api.DataStore;
import org.jledit.ContentManager;

import java.io.IOException;
import java.nio.charset.Charset;

public class DatastoreContentManager implements ContentManager {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private final DataStore dataStore;

    public DatastoreContentManager(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * Loads content from the specified location.
     *
     * @param location
     * @return
     */
    @Override
    public String load(String location) throws IOException {
        try {
            String[] parts = location.trim().split(" ");
            if (parts.length < 3) {
                throw new IllegalArgumentException("Invalid location:" + location);
            }
            String profile = parts[0];
            String version = parts[1];
            String resource = parts[2];
            String data = new String(dataStore.getFileConfiguration(version, profile, resource));
            return data != null ? data : "";
        } catch (Exception e) {
            throw new IOException("Failed to read data from zookeeper.", e);
        }
    }

    /**
     * Saves content to the specified location.
     *
     * @param content
     * @param location
     * @return
     */
    @Override
    public boolean save(String content, String location) {
        try {
            String[] parts = location.trim().split(" ");
            if (parts.length < 3) {
                throw new IllegalArgumentException("Invalid location:" + location);
            }
            String profile = parts[0];
            String version = parts[1];
            String resource = parts[2];
            dataStore.setFileConfiguration(version, profile, resource, content.getBytes());
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Saves the {@link String} content to the specified location using the specified {@link java.nio.charset.Charset}.
     *
     * @param content
     * @param charset
     * @param location
     * @return
     */
    @Override
    public boolean save(String content, Charset charset, String location) {
        return save(content, location);
    }

    /**
     * Detect the Charset of the content in the specified location.
     *
     * @param location
     * @return
     */
    @Override
    public Charset detectCharset(String location) {
        return UTF_8;
    }
}
