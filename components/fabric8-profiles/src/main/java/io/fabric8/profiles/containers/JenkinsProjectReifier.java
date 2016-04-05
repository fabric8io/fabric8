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
package io.fabric8.profiles.containers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Reifies jenkins project resources, like jenkinsfile pipeline script.
 */
public class JenkinsProjectReifier extends ProjectReifier {

    public static final String CONTAINER_TYPE = "jenkins";

    private static final String JENKINSFILE = "Jenkinsfile";

    public JenkinsProjectReifier(Properties properties) {
        super(properties);
    }

    @Override
    public void reify(Path target, Properties config, Path profilesDir) throws IOException {
        // copy Jenkinsfile to root dir in target
        Path jenkinsFile = profilesDir.resolve(JENKINSFILE);
        if (Files.notExists(jenkinsFile)) {
            throw new IOException("Missing Jenkinsfile");
        }
        Files.copy(jenkinsFile, target.resolve(JENKINSFILE));
    }
}
