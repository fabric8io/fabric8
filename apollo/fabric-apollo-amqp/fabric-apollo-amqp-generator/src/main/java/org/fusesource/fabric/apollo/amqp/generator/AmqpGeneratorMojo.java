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

package org.fusesource.fabric.apollo.amqp.generator;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.List;

/**
 * A Maven Mojo so that the AMQP compiler can be used with maven.
 *
 * @goal compile
 * @phase process-sources
 */
public class AmqpGeneratorMojo extends AbstractMojo {

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The directory containing the project source
     *
     * @parameter default=value="${basedir}/src/main/java"
     */
    private File sourceDirectory;

    /**
     * The directory where the amqp spec files (<code>*.xml</code>) are
     * located.
     *
     * @parameter default-value="${basedir}/src/main/resources/amqp"
     */
    private File mainSourceDirectory;

    /**
     * The name of the XML file containing the AMQP type system and
     * encodings
     *
     * @parameter default-value="types.xml"
     */
    private String typesDescriptor;

    /**
     * The directory where the output files will be located.
     *
     * @parameter default-value="${project.build.directory}/generated-sources/amqp"
     */
    private File mainOutputDirectory;

    /**
     * The directory where the amqp spec files (<code>*.xml</code>) are
     * located.
     *
     * @parameter default-value="${basedir}/src/test/resources/amqp"
     */
    private File testSourceDirectory;

    /**
     * The directory where the output files will be located.
     *
     * @parameter default-value="${project.build.directory}/test-generated-sources/amqp"
     */
    private File testOutputDirectory;

    /**
     * The package prefix to put the generated Java classes in
     *
     * @parameter default-value="org.fusesource.fabric.apollo.amqp.codec"
     */
    private String packagePrefix;

    public void execute() throws MojoExecutionException {
        Log.LOG = getLog();

        Log.info("\tmain source directory at %s", mainSourceDirectory);
        Log.info("\tmain output directory at %s", mainOutputDirectory);
        Log.info("\ttest source directory at %s", testSourceDirectory);
        Log.info("\ttest output directory at %s", testOutputDirectory);

        File[] mainFiles = null;
        if ( mainSourceDirectory.exists() ) {
            mainFiles = mainSourceDirectory.listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.getName().endsWith(".xml");
                }
            });
            if ( mainFiles == null || mainFiles.length == 0 ) {
                Log.warn("No AMQP XML definitions found in directory : %s", mainSourceDirectory.getPath());
            } else {
                processFiles(mainFiles, mainOutputDirectory);
                this.project.addCompileSourceRoot(mainOutputDirectory.getAbsolutePath());
            }
        } else {
            Log.warn("Configured main source directory at %s does not exist", mainSourceDirectory);
        }

        File[] testFiles = null;
        if ( testSourceDirectory.exists() ) {
            testFiles = testSourceDirectory.listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.getName().endsWith(".xml");
                }
            });
            if ( testFiles == null || testFiles.length == 0 ) {
                Log.warn("No AMQP XML definitions found in directory : %s", testSourceDirectory.getPath());
            } else {
                processFiles(testFiles, testOutputDirectory);
                this.project.addTestCompileSourceRoot(testOutputDirectory.getAbsolutePath());
            }
        } else {
            Log.warn("Configured test source directory at %s does not exist", testSourceDirectory);
        }
    }

    private void processFiles(File[] mainFiles, File outputDir) throws MojoExecutionException {

        Log.info("Processing files : ");

        List<File> recFiles = Arrays.asList(mainFiles);

        for ( File file : recFiles ) {
            Log.info("\t%s", file);
        }

        try {
            Generator gen = new Generator();
            gen.setInputFiles(mainFiles);
            gen.setOutputDirectory(outputDir);
            gen.setSourceDirectory(sourceDirectory);
            gen.setPackagePrefix(packagePrefix);
            gen.generate();
        } catch (Exception e) {
            Log.error("Error generating code : " + e + " - " + e.getMessage(), e);
            throw new MojoExecutionException(e.getMessage(), e);
        }

    }

}
