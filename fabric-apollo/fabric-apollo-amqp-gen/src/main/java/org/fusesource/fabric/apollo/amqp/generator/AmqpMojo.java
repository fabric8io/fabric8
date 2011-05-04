/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
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
public class AmqpMojo extends AbstractMojo {

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
     *
     */
    private File sourceDirectory;

    /**
     * The directory where the amqp spec files (<code>*.xml</code>) are
     * located.
     *
     * @parameter default-value="${basedir}/src/main/amqp"
     */
    private File mainSourceDirectory;

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
     * @parameter default-value="${basedir}/src/test/amqp"
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

        File[] mainFiles = null;
        if ( mainSourceDirectory.exists() ) {
            mainFiles = mainSourceDirectory.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".xml");
            }
        });
            if (mainFiles==null || mainFiles.length==0) {
                getLog().warn("No amqp spec files found in directory: " + mainSourceDirectory.getPath());
            } else {
                processFiles(mainFiles, mainOutputDirectory);
                this.project.addCompileSourceRoot(mainOutputDirectory.getAbsolutePath());
            }
        }

        File[] testFiles = null;
        if ( testSourceDirectory.exists() ) {
            testFiles = testSourceDirectory.listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.getName().endsWith(".xml");
                }
            });
            if (testFiles==null || testFiles.length==0) {
                getLog().warn("No amqp spec files found in directory: " + testSourceDirectory.getPath());
            } else {
                processFiles(testFiles, testOutputDirectory);
                this.project.addTestCompileSourceRoot(testOutputDirectory.getAbsolutePath());
            }
        }
    }

    private void processFiles(File[] mainFiles, File outputDir) throws MojoExecutionException {
        List<File> recFiles = Arrays.asList(mainFiles);
        for (File file : recFiles) {
            try {
                getLog().info("Compiling: "+file.getPath());
                Utils.LOG = getLog();

                Generator gen = new Generator();
                gen.setInputFiles(mainFiles);
                gen.setOutputDirectory(outputDir);
                gen.setSourceDirectory(sourceDirectory);
                gen.setPackagePrefix(packagePrefix);
                gen.generate();
            } catch (Exception e) {
                getLog().error("Error generating code : " + e + " - " + e.getMessage(), e);
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
    }

}
