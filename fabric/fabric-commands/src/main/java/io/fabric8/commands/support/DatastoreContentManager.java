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


import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileRegistry;

import java.io.IOException;
import java.nio.charset.Charset;

import org.jledit.ContentManager;

public class DatastoreContentManager implements ContentManager {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private final ProfileRegistry profileRegistry;

    public DatastoreContentManager(FabricService fabricService) {
        this.profileRegistry = fabricService.adapt(ProfileRegistry.class);
    }

    /**
     * Loads content from the specified location.
     */
    @Override
    public String load(String location) throws IOException {
        try {
            String[] parts = location.trim().split(" ");
            if (parts.length < 3) {
                throw new IllegalArgumentException("Invalid location:" + location);
            }
            String profileId = parts[0];
            String versionId = parts[1];
            String resource = parts[2];
            Profile profile = profileRegistry.getRequiredProfile(versionId, profileId);
            String data = new String(profile.getFileConfiguration(resource));
            return data != null ? data : "";
        } catch (Exception e) {
            throw new IOException("Failed to read data from zookeeper.", e);
        }
    }

    /**
     * Saves content to the specified location.
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
            profileRegistry.setFileConfiguration(version, profile, resource, content.getBytes());
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Saves the {@link String} content to the specified location using the specified {@link java.nio.charset.Charset}.
     */
    @Override
    public boolean save(String content, Charset charset, String location) {
        return save(content, location);
    }

    /**
     * Detect the Charset of the content in the specified location.
     */
    @Override
    public Charset detectCharset(String location) {
        return UTF_8;
    }
}
