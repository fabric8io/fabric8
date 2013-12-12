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

/**
 */
public class ClassLoaderIntegrationTestSupport {
    protected MavenResolverImpl mavenResolver = new MavenResolverImpl();
    protected boolean offline;
    protected String groupId = "io.fabric8.fab.tests";
    protected String version = "1.1-SNAPSHOT";
    protected String extension = "jar";
    protected String classifier = null;


    public static void main(String[] args) {
        ClassLoaderIntegrationTestSupport test = new ClassLoaderIntegrationTestSupport();
        String artifactId = "fab-sample-camel-velocity-share";
        if (args.length == 1) {
            artifactId = args[0];
        } else if (args.length > 1) {
            test.groupId = args[0];
            artifactId = args[1];
            if (args.length > 2) {
                test.version = args[2];
            }
        }
        try {
            test.testFabricBundle(artifactId);
        } catch (Exception e) {
            System.out.println("Caught: " + e);
            e.printStackTrace();
            Throwable cause = e.getCause();
            if (cause != e && cause != null) {
                System.out.println("Caused by: " + cause);
                cause.printStackTrace();
            }
        }
    }

    protected void testFabricBundle(String artifactId) throws Exception {
        DependencyTreeResult node = mavenResolver.collectDependencies(groupId, artifactId, version, extension, classifier, offline, DependencyFilters.testScopeOrOptionalFilter);
        DependencyTree tree = node.getTree();
        System.out.println(tree.getDescription());
    }


}
