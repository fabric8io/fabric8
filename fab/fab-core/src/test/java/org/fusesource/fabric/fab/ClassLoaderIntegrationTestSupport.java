/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab;

/**
 */
public class ClassLoaderIntegrationTestSupport {
    protected MavenResolver mavenResolver = new MavenResolver();
    protected boolean offline;
    protected String groupId = "org.fusesource.fabric.fab.tests";
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
