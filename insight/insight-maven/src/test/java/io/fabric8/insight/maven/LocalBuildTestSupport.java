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
package io.fabric8.insight.maven;

import java.util.LinkedList;
import java.util.List;

import io.fabric8.insight.maven.aether.Aether;
import io.fabric8.insight.maven.aether.Repository;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Base class for tests which work on local builds; resolving using the local mvn repo first
 */
public class LocalBuildTestSupport {

    protected Aether aether;

    public LocalBuildTestSupport() {
        List<Repository> repositories = new LinkedList<Repository>();
        repositories.add(new Repository("local-repo", Aether.USER_REPOSITORY));
        repositories.addAll(Aether.defaultRepositories());

        aether = new Aether(Aether.USER_REPOSITORY, repositories);
    }

}
