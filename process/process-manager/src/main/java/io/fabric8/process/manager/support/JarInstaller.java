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
package io.fabric8.process.manager.support;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.jar.Attributes;

import aQute.bnd.osgi.Jar;
import com.google.common.base.Throwables;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import io.fabric8.common.util.ChecksumUtils;
import io.fabric8.common.util.FileChangeInfo;
import io.fabric8.common.util.Filter;
import io.fabric8.common.util.Pair;
import io.fabric8.common.util.Strings;
import io.fabric8.fab.DependencyFilters;
import io.fabric8.fab.DependencyTreeResult;
import io.fabric8.fab.MavenResolverImpl;
import io.fabric8.process.manager.InstallContext;
import io.fabric8.process.manager.InstallOptions;
import io.fabric8.process.manager.InstallTask;
import io.fabric8.process.manager.config.ProcessConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.ArtifactResolutionException;

import static io.fabric8.common.util.Strings.join;

/**
 */
public class JarInstaller implements InstallTask {

    private static final Logger LOG = LoggerFactory.getLogger(JarInstaller.class);

    MavenResolverImpl mavenResolver = new MavenResolverImpl();
    private final InstallOptions parameters;
    private final Executor executor;

    public JarInstaller(InstallOptions parameters, Executor executor) {
        this.parameters = parameters;
        this.executor = executor;
    }

    @Override
    public void install(InstallContext installContext, ProcessConfig config, String id, File installDir) throws Exception {
        // lets unpack the launcher
        URL artifactUrl = parameters.getUrl();
        File libDir = new File(installDir, "lib");
        libDir.mkdirs();
        Map<String, Pair<File, File>> copyFiles = new HashMap<String, Pair<File, File>>();

        if (artifactUrl != null) {
            copyArtifactAndDependencies(config, id, installDir, parameters, libDir, copyFiles);
        }
        copyJarFiles(id, installDir, parameters, libDir, copyFiles);

        Set<Map.Entry<String, Pair<File, File>>> entries = copyFiles.entrySet();
        Map<File, Long> checksums = ChecksumUtils.loadInstalledChecksumCache(libDir);

        // lets delete all the files we've not got a checksum for which
        // we are not about to write
        Set<File> filesToRemove = new HashSet<File>();
        filesToRemove.addAll(checksums.keySet());

        for (Map.Entry<String, Pair<File, File>> entry : entries) {
            String location = entry.getKey();
            Pair<File, File> pair = entry.getValue();
            File source = pair.getFirst();
            File dest = pair.getSecond();
            // lets use the source for the checksum so we can update
            // the checksum cache before we change any files
            long checksum = ChecksumUtils.checksumFile(source);
            filesToRemove.remove(dest);
            checksums.put(source, checksum);
        }

        for (File fileToRemove : filesToRemove) {
            LOG.info("Removing: " + fileToRemove);
            checksums.remove(fileToRemove);
            installContext.addRestartReason(fileToRemove);
            fileToRemove.delete();
        }

        // now lets update the checksums on disk before we start writing any new files
        // so that if we fail after this point we can properly clean up any new files we've added
        ChecksumUtils.saveInstalledChecksumCache(libDir, checksums);

        for (Map.Entry<String, Pair<File, File>> entry : entries) {
            String location = entry.getKey();
            Pair<File, File> pair = entry.getValue();
            File sourceFile = pair.getFirst();
            File destFile = pair.getSecond();
            FileChangeInfo oldChangeInfo = installContext.createChangeInfo(destFile);
            Files.copy(sourceFile, destFile);
            installContext.onDeploymentFileWrite(location, destFile, oldChangeInfo, true);
        }
    }

    protected void copyJarFiles(String id, File installDir, InstallOptions parameters, File libDir, Map<String, Pair<File, File>> copyFiles) throws IOException {
        Set<Map.Entry<String, File>> entries = parameters.getJarFiles().entrySet();
        for (Map.Entry<String, File> entry : entries) {
            String location = entry.getKey();
            File file = entry.getValue();
            copyFiles.put(location, new Pair<File, File>(file, new File(libDir, file.getName())));
        }
    }

    protected void copyArtifactAndDependencies(ProcessConfig config, String id, File installDir, InstallOptions parameters, File libDir, Map<String, Pair<File, File>> copyFiles) throws Exception {
        URL artifactUrl = parameters.getUrl();
        // now lets download the executable jar as main.jar and all its dependencies...
        Filter<Dependency> optionalFilter = DependencyFilters.parseExcludeOptionalFilter(join(Arrays.asList(parameters.getOptionalDependencyPatterns()), " "));
        Filter<Dependency> excludeFilter = DependencyFilters.parseExcludeFilter(join(Arrays.asList(parameters.getExcludeDependencyFilterPatterns()), " "), optionalFilter);
        DependencyTreeResult result = mavenResolver.collectDependenciesForJar(getArtifactFile(artifactUrl),
                parameters.isOffline(),
                excludeFilter);

        DependencyNode mainJarDependency = result.getRootNode();

        Artifact mainPomArtifact = mainJarDependency.getDependency().getArtifact();
        File mainJar = mavenResolver.resolveArtifact(parameters.isOffline(),
                mainPomArtifact.getGroupId(), mainPomArtifact.getArtifactId(),
                mainPomArtifact.getVersion(), mainPomArtifact.getClassifier(), "jar").
                getFile();
        if (mainJar == null) {
            System.out.println("Cannot find file for main jar " + mainJarDependency);
        } else {
            File newMain = new File(libDir, "main.jar");
            Files.copy(mainJar, newMain);
            String mainClass = parameters.getMainClass();
            if (mainClass != null) {
                setMainClass(config, installDir, newMain, id, mainClass);
            }
        }

        copyDependencies(mainJarDependency, libDir, copyFiles);
    }

    private File getArtifactFile(URL url) throws IOException {
        File tmpFile = File.createTempFile("artifact", ".jar");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(tmpFile);
            Resources.copy(url, fos);
        } catch (Exception ex) {
            LOG.warn("Could not copy URL: " + url + ". Reason: " + ex, ex);
            Throwables.propagate(ex);
        } finally {
            Closeables.closeQuietly(fos);
        }
        return tmpFile;
    }

    /**
     * Sets the executable class name in the given jar
     */
    protected void setMainClass(ProcessConfig config, File installDir, File jarFile, String id, String mainClass) throws Exception {
        File tmpFile = File.createTempFile("fuse-process-" + id, ".jar");
        Files.copy(jarFile, tmpFile);
        Jar jar = new Jar(tmpFile);
        Attributes attributes = jar.getManifest().getMainAttributes();
        attributes.putValue("Main-Class", mainClass);
        jar.write(jarFile);
    }

    protected void copyDependencies(DependencyNode dependency, File libDir, Map<String, Pair<File, File>> copyFiles) throws IOException, ArtifactResolutionException {
        List<DependencyNode> children = dependency.getChildren();
        if (children != null) {
            for (DependencyNode child : children) {
                String location = toLocation(child.getDependency());
                if (child.getDependency().getScope().equals("provided")) {
                    LOG.debug("Dependency {} has scope provided. Not copying.", child.getDependency());
                    continue;
                }
                File file = getFile(child);
                if (file == null) {
                    System.out.println("Cannot find file for dependent jar " + child);
                } else {
                    copyFiles.put(location, new Pair<File, File>(file, new File(libDir, file.getName())));
                }
                copyDependencies(child, libDir, copyFiles);
            }
        }
    }

    private String toLocation(Dependency dependency) {
        Artifact artifact = dependency.getArtifact();
        String prefix = "";
        String postfix = "";
        String classifier = artifact.getClassifier();
        String extension = artifact.getExtension();
        if (Strings.isNotBlank(extension) && (!extension.equals("jar") || Strings.isNotBlank(classifier))) {
            postfix = "/" + extension;
        }
        if (Strings.isNotBlank(classifier)) {
            postfix += "/" + classifier;
        }
        return prefix + "mvn:" + artifact.getGroupId() + "/" + artifact.getArtifactId() + "/" + artifact.getVersion() + postfix;
    }

    protected File getFile(DependencyNode node) throws ArtifactResolutionException {
        if (node != null) {
            Dependency dependency = node.getDependency();
            if (dependency != null) {
                Artifact artifact = dependency.getArtifact();
                if (artifact != null) {
                    File file = artifact.getFile();
                    if (file == null) {
                        return mavenResolver.resolveFile(artifact);
                    }
                    return file;
                }
            }
        }
        return null;
    }

}
