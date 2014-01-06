/*
 * Copyright 2010 Red Hat, Inc.
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
package io.fabric8.partition.internal.repositories;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.git.internal.GitDataStore;
import io.fabric8.partition.internal.BaseWorkItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;

public class ProfileWorkItemRepository extends BaseWorkItemRepository implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZkWorkItemRepository.class);

    private final String name;
    private final FabricService fabricService;
    private final GitDataStore dataStore;
    private final String profile;
    private final String folderPath;

    private volatile String lastModified = "";

    public ProfileWorkItemRepository(String name, GitDataStore dataStore, String partitionsPath, FabricService fabricService) {
        this.name = name;
        this.dataStore = dataStore;
        this.fabricService = fabricService;
        int index = partitionsPath.indexOf("/");
        this.profile = partitionsPath.substring((ProfileWorkItemRepositoryFactory.SCHME + ":").length(), index);
        this.folderPath = partitionsPath.substring(index + 1);
    }


    @Override
    public void start() {
       dataStore.trackConfiguration(this);
       run();
    }

    @Override
    public void stop() {
       dataStore.untrackConfiguration(this);
    }

    @Override
    public void close() {
       stop();
    }

    @Override
    public List<String> listWorkItemLocations() {
        List<String> items = Lists.newArrayList();
        try {
            String version = dataStore.getContainerVersion(name);
            Version v = fabricService.getVersion(version);
            Profile p = v.getProfile(profile);
            for (String f : p.getFileConfigurations().keySet()) {
                if (f.startsWith(folderPath)) {
                    items.add(f);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting work items from profile repository. Returning empty.", e);
        }
        return items;
    }


    @Override
    public String readContent(String location) {
        try {
            return Resources.toString(new URL(ProfileWorkItemRepositoryFactory.SCHME + ":" + location), Charsets.UTF_8);
        } catch (Exception e) {
            throw FabricException.launderThrowable(e);
        }
    }

    @Override
    public void run() {
       String modifed = dataStore.getLastModified(dataStore.getContainerVersion(name), profile);
       if (!modifed.equals(lastModified)) {
           notifyListeners();
           lastModified = modifed;
       }
    }
}
