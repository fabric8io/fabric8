/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fabric8.fab;

import java.io.File;

/**
 * Simple class to display the dependencies of a given pom.xml file
 */
public class Main {
    private String pomFile;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: pomFile");
            return;
        }
        Main main = new Main(args[0]);
        try {
            main.run();
        } catch (Exception e) {
            System.out.println("Failed: " + e);
            e.printStackTrace();
        }
    }

    public Main(String pomFile) {
        this.pomFile = pomFile;
    }

    public void run() throws Exception {
        MavenResolverImpl manager = new MavenResolverImpl();

        DependencyTreeResult results;
        File file = new File(pomFile);
        if (file.exists()) {
            System.out.println("Parsing pom: " + file);
            results = manager.collectDependencies(file, true);
        } else {
            VersionedDependencyId dependencyId = VersionedDependencyId.fromString(pomFile);
            results = manager.collectDependencies(dependencyId, true, DependencyFilters.testScopeOrOptionalFilter);
        }
        DependencyTree tree = results.getTree();
        System.out.println(tree.getDescription());
    }
}
