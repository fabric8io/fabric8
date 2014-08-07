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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.aether.repository.Authentication;

public class Authentications {

    public static Logger log = LoggerFactory.getLogger(Authentications.class);

    public static final String HOME = System.getProperty("user.home", ".");
    public static final File REPO_FILE = new File(HOME, ".repo.fusesource.com.properties");

    public static Authentication getFuseRepoAuthentication() {
        if (!REPO_FILE.exists()) {
            throw new IllegalArgumentException("No file available at " + REPO_FILE + " to contain the username and password to connect to the fusesource repo!");
        }

        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(REPO_FILE));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
        String username = properties.getProperty("username");
        String password = properties.getProperty("password");

        if (username == null) {
            throw new IllegalArgumentException("Missing 'username' in file: " + REPO_FILE);
        }
        if (password == null) {
            throw new IllegalArgumentException("Missing 'password' in file: " + REPO_FILE);
        }
        log.debug("Using user {} to access repo.fusesource.com", username);

        return new AuthenticationBuilder().addUsername(username).addPassword(password).build();
    }

}
