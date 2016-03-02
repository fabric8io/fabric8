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
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
            defaultValue = "${project.build.directory}/${project.artifactId}-${project.version}-app.zip")
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
    protected Archiver tarArchiver;

    @Component(role = Archiver.class, hint = "zip")
    protected Archiver zipArchiver;

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
        getLog().debug("Classpath for " + scope + ":\n" + artifactsToString(artifacts));

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
        // project.getArtifactMap() doesn't include type or classifier in map key so we need to roll our own...
        Map artifactMap = getArtifactMap();

        // Lets then copy the it's dependencies.
        for (Artifact x : artifacts) {

            // x is not resolved, so lets look it up in the map.
            Artifact artifact = (Artifact) artifactMap.get(versionlessKey(x));

            // DefaultArtifact.equals(..) doesn't handle classifier with empty string & null
            // which should be treated as equivalent so we have to roll our own equals...
            if( artifact==null || artifact.getFile() == null || !artifactEquals(artifact, x)) {
                continue;
            }

            getLog().debug("Copying " + artifact.toString());
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
        copyResource("bin/run.sh", targetRun, interpolations, true);
        chmodExecutable(targetRun);

        File targetRunCmd = new File(binDir, "run.cmd");
        copyResource("bin/run.cmd", targetRunCmd, interpolations, false);

        if (source != null && source.exists()) {
            try {
                FileUtils.copyDirectoryStructure(source, assembly);
            } catch (IOException e) {
                throw new MojoExecutionException("Could copy the hawt-app resources", e);
            }
        }

        Archiver archiver;
        String archiveExtension;
        if( archive.getName().endsWith(".tar") ) {
            archiver = tarArchiver;
            archiveExtension = "tar";
        } else if( archive.getName().endsWith(".tar.gz") ) {
            ((TarArchiver) tarArchiver).setCompression(TarArchiver.TarCompressionMethod.gzip);
            archiver = tarArchiver;
            archiveExtension = "tar.gz";
        } else if(  archive.getName().endsWith(".zip") ) {
            archiver = zipArchiver;
            archiveExtension = "zip";
        } else {
            throw new MojoExecutionException("Invalid archive extension.  Should be zip | tar | tar.gz");
        }

        archiver.setDestFile(archive);
        archiver.addFileSet(fileSet(assembly).prefixed(archivePrefix).includeExclude(null, new String[]{"bin/*"}).includeEmptyDirs(true));
        archiver.setFileMode(0755);
        archiver.addFileSet(fileSet(assembly).prefixed(archivePrefix).includeExclude(new String[]{"bin/*"}, null).includeEmptyDirs(true));
        try {
            archiver.createArchive();
        } catch (IOException e) {
            throw new MojoExecutionException("Could not create the " + archive + " file", e);
        }
        projectHelper.attachArtifact(project, archiveExtension, archiveClassifier, archive);

    }

    private void chmodExecutable(File file) {
        try {
            Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rwxr-xr-x"));
        } catch (Throwable ignore) {
            // we tried our best, perhaps the OS does not support posix file perms.
        }
    }

    private void copyResource(String source, File target, HashMap<String, String> interpolations, boolean unixLinedEndings) throws MojoExecutionException {

        try {
            String content = loadTextResource(getClass().getResource(source));
            if (interpolations != null) {
                content = StringUtils.interpolate(content, interpolations);
            }
            // Safety check
            if( unixLinedEndings ) {
                content = content.replaceAll("\\r?\\n", Matcher.quoteReplacement("\n"));
            } else {
                content = content.replaceAll("\\r?\\n", Matcher.quoteReplacement("\r\n"));
            }

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

    private String artifactsToString(List<Artifact> artifacts) {
        StringBuilder sb = new StringBuilder();
        for (Artifact art : artifacts) {
            sb.append("    ").append(art.toString()).append(System.getProperty("line.separator"));
        }
        return sb.toString();
    }

    private String versionlessKey(Artifact artifact) {
        String groupId = artifact.getGroupId();
        String artifactId = artifact.getArtifactId();
        String type = artifact.getType();
        String classifier = artifact.getClassifier();
        if (groupId == null) {
            throw new NullPointerException("groupId is null");
        } else if (artifactId == null) {
            throw new NullPointerException("artifactId is null");
        } else if (type == null) {
            throw new NullPointerException("type is null");
        }

        if (classifier == null || classifier.isEmpty()) {
            return groupId + ":" + artifactId + ":" + type;
        }

        return groupId + ":" + artifactId + ":" + type + ":" + classifier;
    }

    private Map getArtifactMap() {
        Set<Artifact> artifacts = project.getArtifacts();
        LinkedHashMap artifactMap = new LinkedHashMap();
        if (project.getArtifacts() != null) {
            Iterator i$ = artifacts.iterator();

            while (i$.hasNext()) {
                Artifact artifact = (Artifact) i$.next();
                artifactMap.put(versionlessKey(artifact), artifact);
            }
        }

        return artifactMap;
    }

    private boolean artifactEquals(Artifact a1, Artifact a2) {
        if (a1 == a2) {
            return true;
        }

        if (!a1.getGroupId().equals(a2.getGroupId())) {
            return false;
        } else if (!a1.getArtifactId().equals(a2.getArtifactId())) {
            return false;
        } else if (!a1.getVersion().equals(a2.getVersion())) {
            return false;
        } else if (!a1.getType().equals(a2.getType())) {
            return false;
        }

        if (a1.getClassifier() == null || a1.getClassifier().isEmpty()) {
            return a2.getClassifier() == null || a2.getClassifier().isEmpty();
        }
        return a1.getClassifier().equals(a2.getClassifier());
    }

}
