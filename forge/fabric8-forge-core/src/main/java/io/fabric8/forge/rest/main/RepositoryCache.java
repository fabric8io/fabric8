/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.forge.rest.main;

import io.fabric8.repo.git.GitRepoClient;
import io.fabric8.repo.git.RepositoryDTO;

import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 */
@Singleton
public class RepositoryCache {
    private Map<String,RepositoryDTO> userCache = new ConcurrentHashMap<>();

    /**
     * Updates the cache of all user repositories
     */
    public void updateUserRepositories(List<RepositoryDTO> repositoryDTOs) {
        for (RepositoryDTO repositoryDTO : repositoryDTOs) {
            String fullName = repositoryDTO.getFullName();
            userCache.put(fullName, repositoryDTO);
        }
    }

    public RepositoryDTO getUserRepository(String user, String repository) {
        return getUserRepository(user + "/" + repository);
    }

    public RepositoryDTO getUserRepository(String fullName) {
        return userCache.get(fullName);
    }

    /**
     * Attempts to use the cache or performs a query for all the users repositories if its not present
     */
    public RepositoryDTO getOrFindUserRepository(String user, String repositoryName, GitRepoClient repoClient) {
        RepositoryDTO repository = getUserRepository(user, repositoryName);
        if (repository == null) {
            List<RepositoryDTO> repositoryDTOs = repoClient.listRepositories();
            updateUserRepositories(repositoryDTOs);
            repository = getUserRepository(user, repositoryName);
        }
        return repository;
    }
}
