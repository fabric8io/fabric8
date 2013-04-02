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

    Properties getProfileAttributes(String version, String profile);

    void setProfileAttribute(String version, String profile, String key, String value);

    Map<String, byte[]> getFileConfigurations(String version, String profile);

    byte[] getFileConfiguration(String version, String profile, String pid) throws InterruptedException, KeeperException;

    void setFileConfigurations(String version, String profile, Map<String, byte[]> configurations);

    Map<String, Map<String, String>> getConfigurations(String version, String profile);

    Map<String, String> getConfiguration(String version, String profile, String pid) throws InterruptedException, KeeperException, IOException;

    void setConfigurations(String version, String profile, Map<String, Map<String, String>> configurations);

    Properties getVersionAttributes(String version);

    void setVersionAttribute(String version, String key, String value);

    void createVersion(String version);

    void createVersion(String parentVersionId, String toVersion);

    void deleteVersion(String version);

    List<String> getVersions();

    String getVersion(String name);

    List<String> getProfiles(String version);

    String getProfile(String version, String profile);

    String createProfile(String version, String profile);

    void deleteProfile(String version, String profile);

    void importFromFileSystem(String from);

    String getDefaultVersion();

    void setDefaultVersion(String versionId);

    void setFileConfiguration(String version, String id, String pid, byte[] configuration);

    String getProfile(String version, String profile, boolean create);

    void setConfiguration(String version, String profile, String pid, Map<String, String> configuration);
}
