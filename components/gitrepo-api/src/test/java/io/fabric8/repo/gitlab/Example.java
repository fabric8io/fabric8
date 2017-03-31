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

import java.util.List;

/**
 */
public class Example {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: address userName privateToken");
            return;
        }
        String address = args[0];
        String userName = args[1];
        String privateToken = args[2];

        try {
            GitlabClient client = new GitlabClient(address, userName);
            client.setPrivateToken(privateToken);

            List<GroupDTO> groups    = client.getGroups();
            System.out.println("Found " + groups.size() + " group(s)");
            for (GroupDTO group : groups) {
                System.out.println("" + group);
            }

            List<ProjectDTO> projects    = client.getProjects();
            System.out.println("Found " + groups.size() + " group(s)");
            for (ProjectDTO project : projects) {
                System.out.println("" + project);
            }

            if (projects.size() > 0) {
                ProjectDTO project = projects.get(0);
                Long id = project.getId();
                if (id != null) {

                }
            }
        } catch (Exception e) {
            System.out.println("Caught: " + e);
            e.printStackTrace();
        }
    }
}
