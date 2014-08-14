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

import io.fabric8.insight.maven.aether.CompareDependencyNode;
import io.fabric8.insight.maven.aether.CompareResult;
import org.junit.Test;

public class CompareTest extends TestSupport {

    @Test
    public void compare() throws Exception {
        CompareResult c = aether.compare("org.apache.mesos", "mesos", "0.19.1", "0.18.2");
        dump(c.getRoot(), 0);
    }

    private void dump(CompareDependencyNode node, int indentLevel) {
        for (int i = 0; i < indentLevel; i++) {
            System.out.print("  ");
        }
        System.out.println("" + node.groupId() + ":" + node.artifactId() + " " + node.change().getSummary());
        int nextIndent = indentLevel + 1;
        for (CompareDependencyNode c : node.createChildren()) {
            dump(c, nextIndent);
        }
    }

}
