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
package io.fabric8.fab;

import java.util.Comparator;

import org.eclipse.aether.graph.DependencyNode;

public class DependencyNodeComparator implements Comparator<DependencyNode> {
    public static Comparator<DependencyNode> INSTANCE = new DependencyNodeComparator();

    @Override
    public int compare(DependencyNode o1, DependencyNode o2) {
        DependencyId id1 = DependencyId.newInstance(o1);
        DependencyId id2 = DependencyId.newInstance(o2);
        return id1.compareTo(id2);
    }
}
