/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.repo.gitlab;

/**
 */
public abstract class GitlabClientSupport {
    protected final String address;
    protected final String username;
    protected final String password;
    protected String privateToken;
    private GitlabApi api;

    public GitlabClientSupport(String address, String username, String password) {
        this.address = address;
        this.password = password;
        this.username = username;
    }

    public GitlabClientSupport(String address, String username) {
        this.username = username;
        this.address = address;
        this.password = null;
    }

    protected GitlabApi getApi() {
        if (api == null) {
            api = createWebClient(GitlabApi.class);
        }
        return api;
    }

    public String getAddress() {
        return address;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public String getPrivateToken() {
        return privateToken;
    }

    public void setPrivateToken(String privateToken) {
        this.privateToken = privateToken;
    }

    protected abstract <T> T createWebClient(Class<T> clientType);
}
