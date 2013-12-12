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

import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.graph.Dependency;

/**
 * An exception thrown if a dependency cannot resolve its dependents
 */
public class FailedToResolveDependency extends RepositoryException {
    private final Dependency dependency;

    public FailedToResolveDependency(Dependency dependency, Exception cause) {
        super("Failed to resolve dependency of " + dependency.getArtifact() + ". " + cause.getMessage(), cause);
        this.dependency = dependency;
    }

    public Dependency getDependency() {
        return dependency;
    }
}
