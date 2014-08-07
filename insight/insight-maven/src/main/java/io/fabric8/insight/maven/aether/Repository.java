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
package io.fabric8.insight.maven.aether;

import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.RemoteRepository;

public class Repository {

    private String id;
    private String url;
    private String repoType;
    private Authentication authentication;

    public Repository(String id, String url) {
        this(id, url, "default", null);
    }

    public Repository(String id, String url, Authentication authentication) {
        this(id, url, "default", authentication);
    }

    public Repository(String id, String url, String repoType, Authentication authentication) {
        this.id = id;
        this.url = url;
        this.repoType = repoType;
        this.authentication = authentication;
    }

    public RemoteRepository toRemoteRepository() {
        RemoteRepository.Builder remoteRepository = new RemoteRepository.Builder(this.id, this.repoType, this.url);
        if (this.authentication != null) {
            remoteRepository.setAuthentication(this.authentication);
        }
        return remoteRepository.build();
    }

}
