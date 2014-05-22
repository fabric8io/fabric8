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

public class CompareResult {

    private AetherResult result1;
    private AetherResult result2;

    public CompareResult(AetherResult result1, AetherResult result2) {
        this.result1 = result1;
        this.result2 = result2;
    }

    public CompareDependencyNode getRoot() {
        return new CompareDependencyNode(result1.root(), result2.root());
    }

}
