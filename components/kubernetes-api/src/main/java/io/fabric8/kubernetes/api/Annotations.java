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
package io.fabric8.kubernetes.api;

/**
 * Constants for standard annotations used in fabric8
 */
public class Annotations {
    public static class Secrets {
        public static final String SSH_KEY = "fabric8.io/secret-ssh-key";
        public static final String SSH_PUBLIC_KEY = "fabric8.io/secret-ssh-public-key";
        public static final String GPG_KEY = "fabric8.io/secret-gpg-key";
    }

    public static class Builds {
        public static final String BUILD_ID = "fabric8.io/build-id";
        public static final String BUILD_URL = "fabric8.io/build-url";

        public static final String GIT_COMMIT = "fabric8.io/git-commit";
        public static final String GIT_URL = "fabric8.io/git-url";
        public static final String GIT_BRANCH = "fabric8.io/git-branch";
    }

    public static class Project {
        public static final String PREFIX = "fabric8.io/project-";
    }

    public static class Tests {
        public static final String SESSION_ID = "fabric8.io/test-session-id";
        public static final String TEST_CASE_STATUS = "fabric8.io/test-status-";
        public static final String TEST_SESSION_STATUS = "fabric8.io/test-session-status";
    }
}