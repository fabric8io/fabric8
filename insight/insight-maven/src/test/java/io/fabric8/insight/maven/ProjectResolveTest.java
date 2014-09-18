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

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.fabric8.insight.maven.aether.AetherJarOrPom;
import io.fabric8.insight.maven.aether.AetherPomResult;
import io.fabric8.insight.maven.aether.AetherResult;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ProjectResolveTest extends LocalBuildTestSupport {

    private Map<String, ArtifactVersions> idNoVersionMap = new TreeMap<String, ArtifactVersions>();

    @Test
    public void aetherTest() throws Exception {
        String postfix = "insight-maven/pom.xml";
        // lets find the fuse project dir
        File file = new File("../" + postfix);
        if (!file.exists()) {
            file = new File("../../" + postfix);
        }

        assertTrue(file.exists());
        AetherPomResult result = aether.resolveLocalProject(file);

        addResultDependencies(result);

        for (ArtifactVersions av: idNoVersionMap.values()) {
            Map<String, HashSet<DependencyNode>> versions = av.versionsToUseSet;
            if (versions.size() > 1) {
                System.err.println("ERROR: Duplciate versions for " + av.artifact + " uses: " + versions);
            }
        }

        for (ArtifactVersions av: idNoVersionMap.values()) {
            Artifact k = av.artifact;
            List<String> versions = new LinkedList<String>(av.versionsToUseSet.keySet());
            if (versions.isEmpty()) {
                System.out.println("Warning no versions for " + k + " in " + av.versionsToUseSet);
            } else {
                String version = versions.get(versions.size() - 1);
                System.out.println("<dependency>");
                System.out.println("  <groupId>" + k.getGroupId() + "</groupId>");
                System.out.println("  <artifactId>" + k.getArtifactId() + "</artifactId>");
                System.out.println("  <version>" + version + "</version>");

                String classifier = k.getClassifier();
                if (classifier != null && classifier.length() > 0) {
                    System.out.println("  <classifier>" + classifier + "</classifier>");
                }
                String extension = k.getExtension();
                if (extension != null && extension.length() > 0 && !extension.equals("jar")) {
                    System.out.println("  <type>" + extension + "</type>");
                }
                System.out.println("</dependency>");
            }
        }
    }

    private static String idLessVersion(Artifact a) {
        return a.getGroupId() + ":" + a.getArtifactId() + ": " + a.getExtension() + ":" + a.getClassifier();
    }

//    private static String idWithVersion(Artifact a) {
//        return a.getGroupId() + ":" + a.getArtifactId() + ": " + a.getVersion() + ":" + a.getExtension() + ":" + a.getClassifier();
//    }

    public void addDependencies(DependencyNode root) {
        addChildDependencies(root, root);
    }

    public void addChildDependencies(DependencyNode node, DependencyNode owner) {
        Artifact artifact = node.getDependency().getArtifact();
        String idNoVersion = idLessVersion(artifact);
        String version = artifact.getVersion();

        ArtifactVersions artifactVersions = idNoVersionMap.get(idNoVersion);
        if (artifactVersions == null) {
            artifactVersions = new ArtifactVersions(artifact);
            idNoVersionMap.put(idNoVersion, artifactVersions);
        }

        Map<String, HashSet<DependencyNode>> versionMap = artifactVersions.versionsToUseSet;
        HashSet<DependencyNode> useSet = versionMap.get(version);
        if (useSet == null) {
            useSet = new HashSet<DependencyNode>();
            versionMap.put(version, useSet);
        }

        useSet.add(owner);
        versionMap.put(version, useSet);

        for (DependencyNode child: node.getChildren()) {
            addChildDependencies(child, owner);
        }
    }

    private void addResultDependencies(AetherJarOrPom r) {
        if (r instanceof AetherPomResult) {
            addDependencies(r.root());
            for (AetherJarOrPom m : ((AetherPomResult) r).getModules()) {
                addResultDependencies(m);
            }
        } else if (r instanceof AetherResult) {
            addDependencies(r.root());
        }
    }

//    private <K, V> V getOrElseUpdate(Map<K, V> map, K key, V value) {
//        V answer = map.get(key);
//        if (answer == null) {
//            answer = value;
//            map.put(key, value);
//        }
//        return answer;
//    }

    private static class ArtifactVersions {
        private Artifact artifact;
        private Map<String, HashSet<DependencyNode>> versionsToUseSet = new TreeMap<String, HashSet<DependencyNode>>();

        private ArtifactVersions(Artifact artifact) {
            this.artifact = artifact;
        }

        @Override
        public String toString() {
            return "ArtifactVersions{" + idLessVersion(artifact) + " versions: " + versionsToUseSet + ")";
        }
    }

}
