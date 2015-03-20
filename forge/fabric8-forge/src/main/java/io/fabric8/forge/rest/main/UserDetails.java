/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.forge.rest.main;

import io.fabric8.repo.git.GitRepoClient;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

/**
 */
public class UserDetails {
    private final String user;
    private final String password;
    private final String address;
    private final String email;
    private String branch = "master";

    public UserDetails(String address, String user, String password, String email) {
        this.user = user;
        this.password = password;
        this.address = address;
        this.email = email;
    }


    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getUser() {
        return user;
    }

    /**
     * Returns the address of the gogs REST API
     */
    public String getAddress() {
        return address;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public GitRepoClient createRepoClient() {
        return new GitRepoClient(address, user, password);
    }

    public CredentialsProvider createCredentialsProvider() {
        return  new UsernamePasswordCredentialsProvider(user, password);
    }
}
