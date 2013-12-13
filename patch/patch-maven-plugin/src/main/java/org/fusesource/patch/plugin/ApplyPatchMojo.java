/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.patch.plugin;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.fusesource.patch.impl.Offline;

/**
 * Apply patches to an unpacked distribution
 *
 * @goal apply-patch
 * @phase process-resources
 * @execute phase="process-resources"
 * @requiresDependencyResolution runtime
 * @inheritByDefault true
 * @description Apply patch to an unpacked distribution
 */
public class ApplyPatchMojo extends AbstractMojo {

    private static final String OVERRIDE_RANGE = ";range=";

    /**
     * The output directory containing the karaf distribution
     *
     * @parameter
     */
    protected File outputDirectory;

    /**
     * The list of patch files to apply
     *
     * @parameter
     */
    private List<File> patches;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("Applyings patches to " + outputDirectory);

        Offline offline = new Offline(outputDirectory, new Offline.Logger() {
            @Override
            public void log(int level, String message) {
                switch (level) {
                    case Offline.DEBUG: getLog().debug(message); break;
                    case Offline.INFO:  getLog().info(message); break;
                    case Offline.WARN:  getLog().warn(message); break;
                    case Offline.ERROR: getLog().error(message); break;
                }
            }
        });

        try {
            for (File patch : patches) {
                getLog().info("Applying patch: " + patch);
                offline.apply(patch);
            }
        } catch (Exception e) {
            throw new MojoFailureException("Error processing patches", e);
        }
    }

}
