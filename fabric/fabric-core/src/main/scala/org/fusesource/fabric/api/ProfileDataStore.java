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
package org.fusesource.fabric.api;

import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Stan Lewis
 */
public interface ProfileDataStore {

    //
    // Import
    //

    void importFromFileSystem(String from);

    //
    // Default version
    //

    String getDefaultVersion();

    void setDefaultVersion(String versionId);

    //
    // Version management
    //

    List<String> getVersions();

    boolean hasVersion(String name);

    void createVersion(String version);

    void createVersion(String parentVersionId, String toVersion);

    void deleteVersion(String version);

    Map<String, String> getVersionAttributes(String version);

    void setVersionAttribute(String version, String key, String value);

    //
    // Profile management
    //

    List<String> getProfiles(String version);

    boolean hasProfile(String version, String profile);

    void createProfile(String version, String profile);

    String getProfile(String version, String profile, boolean create);

    void deleteProfile(String version, String profile);

    Map<String, String> getProfileAttributes(String version, String profile);

    void setProfileAttribute(String version, String profile, String key, String value);

    // File configurations, including Map based configurations

    Map<String, byte[]> getFileConfigurations(String version, String profile);

    byte[] getFileConfiguration(String version, String profile, String name) throws InterruptedException, KeeperException;

    void setFileConfigurations(String version, String profile, Map<String, byte[]> configurations);

    void setFileConfiguration(String version, String profile, String name, byte[] configuration);

    // Map based configurations

    Map<String, Map<String, String>> getConfigurations(String version, String profile);

    Map<String, String> getConfiguration(String version, String profile, String pid) throws InterruptedException, KeeperException, IOException;

    void setConfigurations(String version, String profile, Map<String, Map<String, String>> configurations);

    void setConfiguration(String version, String profile, String pid, Map<String, String> configuration);
}
