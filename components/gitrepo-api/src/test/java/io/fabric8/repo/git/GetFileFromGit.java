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

import io.fabric8.utils.IOHelpers;
import io.fabric8.utils.Systems;

import java.io.InputStream;

/**
 */
public class GetFileFromGit {
    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: userName password repo file [branch]");
            return;
        }
        String address = "http://" + Systems.getEnvVarOrSystemProperty("GOGS_SERVICE_HOST", "gogs.vagrant.f8");
        String userName = args[0];
        String password = args[1];
        String repo = args[2];
        String path = args[3];
        String branch = "master";
        if (args.length > 4) {
            branch = args[4];
        }

        try {
            System.out.println("Logging into git repo at " + address + " with user " + userName + " to find file: " + path);

            GitRepoClient client = new GitRepoClient(address, userName, password);

            InputStream input = client.getRawFile(userName, repo, branch, path);
            if (input == null) {
                System.out.println("No such file: " + path + " in branch " + branch + " for user: " + userName);
            } else {
                String text = IOHelpers.readFully(input);
                System.out.println("File found!");
                System.out.println();
                System.out.println(text);
                System.out.println();
            }

            path = "DoesNotExist.garbage";
            System.out.println("Now trying file that does not exist: " + path);
            input = client.getRawFile(userName, repo, branch, path);
            if (input == null) {
                System.out.println("Correctly found no file!");
            } else {
                System.out.println("WHOAH found content: " + IOHelpers.readFully(input));
            }

        } catch (Exception e) {
            System.out.println("Caught: " + e);
            e.printStackTrace();
        }
    }
}
