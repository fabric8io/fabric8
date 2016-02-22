/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.maven.hawt.app;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeVisitor;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import static org.codehaus.plexus.archiver.util.DefaultFileSet.fileSet;

/**
 * Builds a hawt app assembly.
 *
 */
@Mojo(name = "build", requiresDependencyResolution = ResolutionScope.TEST,
        defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class BuildMojo extends AbstractMojo {

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The dependency tree builder to use.
     */
    @Component(hint = "default")
    private DependencyGraphBuilder dependencyGraphBuilder;

    /**
     * The scope to filter by when resolving the classpath of the application.
     *
     */
    @Parameter(property = "scope", defaultValue = "runtime")
    private String scope;

    /**
     * Directory of resources use to augment the files in the hawt-app archive.
     *
     */
    @Parameter(property = "hawt-app.source",
            defaultValue = "${basedir}/src/main/hawt-app")
    protected File source;

    /**
     * Directory used to create the assembly.
     *
     */
    @Parameter(property = "hawt-app.assembly",
            defaultValue = "${project.build.directory}/hawt-app")
    protected File assembly;

    @Parameter(property = "hawt-app.archive",
            defaultValue = "${project.build.directory}/${project.artifactId}-${project.version}-app.tar.gz")
    protected File archive;

    @Parameter(property = "hawt-app.archiveClassifier",
            defaultValue = "app")
    protected String archiveClassifier;

    @Parameter(property = "hawt-app.archivePrefix",
            defaultValue = "${project.artifactId}-${project.version}-app/")
    protected String archivePrefix;

    /**
     * The main class to execute for the assembly.
     */
    @Parameter(property = "hawt-app.javaMainClass")
    protected String javaMainClass;

    @Component(role = Archiver.class, hint = "tar")
    protected Archiver archiver;

    // Used for attaching the archive to this artefact
    @Component
    private MavenProjectHelper projectHelper;

    public void execute()
            throws MojoExecutionException, MojoFailureException {

        File libDir = new File(assembly, "lib");
        libDir.mkdirs();

        File binDir = new File(assembly, "bin");
        binDir.mkdirs();

        ArrayList<String> classpath = new ArrayList<String>();

        // get sets of dependencies
        ArrayList<Artifact> artifacts = null;
        try {
            artifacts = collectClassPath();
        } catch (DependencyGraphBuilderException e) {
            throw new MojoExecutionException("Could not get classpath", e);
        }
        System.out.println(artifacts);

        // Lets first copy this project's artifact.
        if (project.getArtifact().getFile() != null) {
            File target = new File(libDir, project.getArtifact().getFile().getName());
            classpath.add(target.getName());
            try {
                FileUtils.copyFile(project.getArtifact().getFile(), target);
            } catch (IOException e) {
                throw new MojoExecutionException("Could not copy artifact to lib dir", e);
            }
        }

        // Artifacts in this map point to resolved files.
        Map artifactMap = project.getArtifactMap();

        // Lets then copy the it's dependencies.
        for (Artifact x : artifacts) {

            // x is not resolved, so lets look it up in the map.
            Artifact artifact = (Artifact) artifactMap.get(ArtifactUtils.versionlessKey(x));
            if( artifact==null || artifact.getFile() == null ) {
                continue;
            }
            File file = artifact.getFile().getAbsoluteFile();
            try {

                File target = new File(libDir, file.getName());

                // just in case we run into an lib name collision, lets
                // find a non-colliding target name
                int dupCounter = 1;
                while (classpath.contains(target.getName())) {
                    target = new File(libDir, "dup" + dupCounter + "-" + file.getName());
                    dupCounter++;
                }

                classpath.add(target.getName());
                FileUtils.copyFile(artifact.getFile(), target);

            } catch (IOException e) {
                throw new MojoExecutionException("Could not copy artifact to lib dir", e);
            }
        }

        // Finally lets write the classpath.
        try {
            String classpathTxt = StringUtils.join(classpath.iterator(), "\n") + "\n";
            FileUtils.fileWrite(new File(libDir, "classpath"), classpathTxt);
        } catch (IOException e) {
            throw new MojoExecutionException("Could create the classpath file", e);
        }

        HashMap<String, String> interpolations = new HashMap<String, String>();
        // Be sure that an empty string is replaced when no main class is given
        interpolations.put("hawtapp.mvn.main.property", javaMainClass != null ? javaMainClass : "");

        File targetRun = new File(binDir, "run.sh");
        copyResource("bin/run.sh", targetRun, interpolations);
        chmodExecutable(targetRun);

        if (source != null && source.exists()) {
            try {
                FileUtils.copyDirectoryStructure(source, assembly);
            } catch (IOException e) {
                throw new MojoExecutionException("Could copy the hawt-app resources", e);
            }
        }

        ((TarArchiver) archiver).setCompression(TarArchiver.TarCompressionMethod.gzip);
        archiver.setDestFile(archive);
        archiver.addFileSet(fileSet(assembly).prefixed(archivePrefix).includeExclude(null, new String[]{"bin/*"}).includeEmptyDirs(true));
        archiver.setFileMode(0755);
        archiver.addFileSet(fileSet(assembly).prefixed(archivePrefix).includeExclude(new String[]{"bin/*"}, null).includeEmptyDirs(true));
        try {
            archiver.createArchive();
        } catch (IOException e) {
            throw new MojoExecutionException("Could not create the " + archive + " file", e);
        }
        projectHelper.attachArtifact(project, "tar.gz", archiveClassifier, archive);
    }

    private void chmodExecutable(File file) {
        try {
            Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rwxr-xr-x"));
        } catch (Throwable ignore) {
            // we tried our best, perhaps the OS does not support posix file perms.
        }
    }

    private void copyResource(String source, File target, HashMap<String, String> interpolations) throws MojoExecutionException {

        try {
            String content = loadTextResource(getClass().getResource(source));
            if (interpolations != null) {
                content = StringUtils.interpolate(content, interpolations);
            }
            // Safety check
            content = content.replaceAll("\\r?\\n", Matcher.quoteReplacement("\n"));
            FileUtils.fileWrite(target, content);
        } catch (IOException e) {
            throw new MojoExecutionException("Could create the " + target + " file", e);
        }
    }

    private String loadTextResource(URL resource) throws IOException {
        InputStream is = resource.openStream();
        try {
            return IOUtil.toString(is, "UTF-8");
        } finally {
            IOUtil.close(is);
        }
    }

    private ArrayList<Artifact> collectClassPath() throws DependencyGraphBuilderException {
        ArtifactFilter filter = new ScopeArtifactFilter(scope);
        DependencyNode rootNode = dependencyGraphBuilder.buildDependencyGraph(project, filter);
        final ArrayList<Artifact> artifacts = new ArrayList<>();
        rootNode.accept(new DependencyNodeVisitor() {
            @Override
            public boolean visit(DependencyNode dependencyNode) {
                artifacts.add(dependencyNode.getArtifact());
                return true;
            }

            @Override
            public boolean endVisit(DependencyNode dependencyNode) {
                return true;
            }
        });
        return artifacts;
    }

}
