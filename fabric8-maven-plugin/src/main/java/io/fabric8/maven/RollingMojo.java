/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Like the <code>apply</code> goal but forces a rolling upgrade of all the Replication Controllers
 */
@Mojo(name = "rolling", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.INSTALL)
public class RollingMojo extends ApplyMojo {
    /**
     * Should we preserve the current scale of the ReplicationController as we perform the rolling upgrade.
     *
     * If false then we will rolling upgrade to the replica count in the generated kubernetes.json
     */
    @Parameter(property = "fabric8.rolling.preserveScale", defaultValue = "true")
    private boolean rollingUpgradePreserveScale;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        setRollingUpgrades(true);
        super.execute();
    }

    @Override
    public boolean isRollingUpgradePreserveScale() {
        return rollingUpgradePreserveScale;
    }

    public void setRollingUpgradePreserveScale(boolean rollingUpgradePreserveScale) {
        this.rollingUpgradePreserveScale = rollingUpgradePreserveScale;
    }
}
