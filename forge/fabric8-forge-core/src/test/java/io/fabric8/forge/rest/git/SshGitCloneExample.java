/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.forge.rest.git;

import io.fabric8.forge.rest.main.ProjectFileSystem;
import io.fabric8.utils.Files;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class SshGitCloneExample {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Arguments: gitUrl privateKeyFile publicKeyFile");
            return;
        }
        String cloneUrl = args[0];
        String privateKeyPath = args[1];
        String publicKeyPath = args[2];


        File projectFolder = new File("target/myproject");
        if (projectFolder.exists()) {
            Files.recursiveDelete(projectFolder);
        }
        File sshPrivateKey = new File(privateKeyPath);
        File sshPublicKey = new File(publicKeyPath);
        String remote = "origin";
        assertThat(sshPrivateKey).exists();
        assertThat(sshPublicKey).exists();

        ProjectFileSystem.cloneRepo(projectFolder, cloneUrl, null, sshPrivateKey, sshPublicKey, remote);
    }
}
