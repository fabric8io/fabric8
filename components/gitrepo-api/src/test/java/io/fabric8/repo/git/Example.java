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
package io.fabric8.repo.git;

import java.util.List;

import static io.fabric8.utils.jaxrs.JsonHelper.toJson;

/**
 */
public class Example {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: address userName password");
            return;
        }
        String address = args[0];
        String userName = args[1];
        String password = args[2];

        try {
            GitRepoClient client = new GitRepoClient(address, userName, password);

            List<RepositoryDTO> repositoryDTOs = client.listRepositories();

            System.out.println("Got repositories: " + toJson(repositoryDTOs));
        } catch (Exception e) {
            System.out.println("Caught: " + e);
            e.printStackTrace();
        }
    }
}
