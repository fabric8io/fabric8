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
package io.fabric8.maven.proxy.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServlet;

import io.fabric8.common.util.Files;
import io.fabric8.deployer.ProjectDeployer;
import io.fabric8.deployer.dto.DependencyDTO;
import io.fabric8.deployer.dto.DeployResults;
import io.fabric8.deployer.dto.ProjectRequirements;
import io.fabric8.maven.MavenResolver;
import io.fabric8.maven.proxy.MavenProxy;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;

public class MavenProxyServletSupport extends HttpServlet implements MavenProxy {

    protected static final Logger LOGGER = Logger.getLogger(MavenProxyServletSupport.class.getName());

    private static final String SNAPSHOT_TIMESTAMP_REGEX = "^([0-9]{8}.[0-9]{6}-[0-9]+).*";
    private static final Pattern SNAPSHOT_TIMESTAMP_PATTENR = Pattern.compile(SNAPSHOT_TIMESTAMP_REGEX);

    //The pattern below matches a path to the following:
    //1: groupId
    //2: artifactId
    //3: version
    //4: artifact filename
    public static final Pattern ARTIFACT_REQUEST_URL_REGEX = Pattern.compile("([^ ]+)/([^/ ]+)/([^/ ]+)/([^/ ]+)");

    //The pattern bellow matches the path to the following:
    //1: groupId
    //2: artifactId
    //3: version
    //4: maven-metadata xml filename
    //7: repository id.
    //9: type
    public static final Pattern ARTIFACT_METADATA_URL_REGEX = Pattern.compile("([^ ]+)/([^/ ]+)/([^/ ]+)/((maven-metadata([-]([^ .]+))?.xml))([.]([^ ]+))?");

    public static final Pattern REPOSITORY_ID_REGEX = Pattern.compile("[^ ]*(@id=([^@ ]+))+[^ ]*");

    public static final String DEFAULT_REPO_ID = "default";

    protected static final String LOCATION_HEADER = "X-Location";

    protected List<RemoteRepository> repositories;
    protected RepositorySystem system;
    protected RepositorySystemSession session;
    protected File tmpFolder = new File(System.getProperty("karaf.data") + File.separator + "maven" + File.separator + "proxy" + File.separator + "tmp");

    final ProjectDeployer projectDeployer;

    final MavenResolver resolver;

    public MavenProxyServletSupport(MavenResolver resolver, ProjectDeployer projectDeployer) {
        this.resolver = resolver;
        this.projectDeployer = projectDeployer;
    }

    public synchronized void start() throws IOException {
        if (!tmpFolder.exists() && !tmpFolder.mkdirs()) {
            throw new IOException("Failed to create temporary artifact folder");
        }
        if (system == null) {
            system = resolver.getRepositorySystem();
        }
        if (session == null) {
            session = resolver.createSession();
        }
        if (repositories == null) {
            repositories = resolver.getRepositories();
        }
    }

    public synchronized void stop() {
    }

    @Override
    public File download(String path) throws InvalidMavenArtifactRequest {
        if (path == null) {
            throw new InvalidMavenArtifactRequest();
        }

        Matcher artifactMatcher = ARTIFACT_REQUEST_URL_REGEX.matcher(path);
        Matcher metdataMatcher = ARTIFACT_METADATA_URL_REGEX.matcher(path);

        if (metdataMatcher.matches()) {
            LOGGER.log(Level.INFO, String.format("Received request for maven metadata : %s", path));
            Metadata metadata = null;
            try {
                metadata = convertPathToMetadata(path);
                List<MetadataRequest> requests = new ArrayList<MetadataRequest>();
                String id = metdataMatcher.group(7);
                for (RemoteRepository repository : repositories) {
                    if (repository.getId().equals(id)) {
                        MetadataRequest request = new MetadataRequest(metadata, repository, null);
                        request.setFavorLocalRepository(false);
                        requests.add(request);
                    }
                }
                if (requests.isEmpty()) {
                    for (RemoteRepository repository : repositories) {
                        MetadataRequest request = new MetadataRequest(metadata, repository, null);
                        request.setFavorLocalRepository(false);
                        requests.add(request);
                    }
                }
                List<MetadataResult> results = system.resolveMetadata(session, requests);
                for (MetadataResult result : results) {
                    if (result.getMetadata() != null && result.getMetadata().getFile() != null) {
                        return result.getMetadata().getFile();
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, String.format("Could not find metadata : %s due to %s", metadata, e));
                return null;
            }
            //If no matching metadata found return nothing
            return null;
        } else if (artifactMatcher.matches()) {
            LOGGER.log(Level.INFO, String.format("Received request for maven artifact : %s", path));
            Artifact artifact = convertPathToArtifact(path);
            try {
                return resolver.resolveFile(artifact);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, String.format("Could not find artifact : %s due to %s", artifact, e), e);
                return null;
            }
        }
        return null;
    }

    @Override
    public boolean upload(InputStream is, String path) throws InvalidMavenArtifactRequest {
        return doUpload(is, path).status();
    }

    protected UploadContext doUpload(InputStream is, String path) throws InvalidMavenArtifactRequest {
        if (path == null) {
            throw new InvalidMavenArtifactRequest();
        }

        int p = path.lastIndexOf('/');
        final String filename = path.substring(p + 1);

        String uuid = UUID.randomUUID().toString(); // TODO -- user uuid?
        File tmp = new File(tmpFolder, uuid);
        //noinspection ResultOfMethodCallIgnored
        tmp.mkdir();
        final File file;
        try {
            file = readFile(is, tmp, filename);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        }

        UploadContext result = new UploadContext(file);

        // root path, try reading mvn coords
        if (p <= 0) {
            try {
                String mvnCoordsPath = readMvnCoordsPath(file);
                if (mvnCoordsPath != null) {
                    return move(file, mvnCoordsPath);
                } else {
                    result.addHeader(LOCATION_HEADER, file.getPath()); // we need manual mvn coords input
                    return result;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, String.format("Failed to deploy artifact : %s due to %s", filename, e));
                return UploadContext.ERROR;
            }
        }

        Matcher artifactMatcher = ARTIFACT_REQUEST_URL_REGEX.matcher(path);
        Matcher metadataMatcher = ARTIFACT_METADATA_URL_REGEX.matcher(path);

        if (metadataMatcher.matches()) {
            LOGGER.log(Level.INFO, String.format("Received upload request for maven metadata : %s", path));
            try {
                Metadata metadata = convertPathToMetadata(path);
                metadata = metadata.setFile(file);
                InstallRequest request = new InstallRequest();
                request.addMetadata(metadata);
                system.install(session, request);
                LOGGER.log(Level.INFO, "Maven metadata installed: " + request.toString());
            } catch (Exception e) {
                result = UploadContext.ERROR;
                LOGGER.log(Level.WARNING, String.format("Failed to upload metadata: %s due to %s", path, e), e);
            }
            //If no matching metadata found return nothing
        } else if (artifactMatcher.matches()) {
            LOGGER.log(Level.INFO, String.format("Received upload request for maven artifact : %s", path));
            Artifact artifact = null;
            try {
                artifact = convertPathToArtifact(path);
                artifact = artifact.setFile(file);
                InstallRequest request = new InstallRequest();
                request.addArtifact(artifact);
                system.install(session, request);

                result.setGroupId(artifact.getGroupId());
                result.setArtifactId(artifact.getArtifactId());
                result.setVersion(artifact.getVersion());
                result.setType(artifact.getExtension());

                LOGGER.log(Level.INFO, "Artifact installed: " + artifact.toString());
            } catch (Exception e) {
                result = UploadContext.ERROR;
                LOGGER.log(Level.WARNING, String.format("Failed to upload artifact : %s due to %s", artifact, e), e);
            }
        }
        return result;

    }

    protected UploadContext move(String currentFile, String newPath) throws Exception {
        File file = new File(currentFile);
        if (!file.exists()) {
            throw new IllegalArgumentException("No such file: " + currentFile);
        }

        return move(file, newPath);
    }

    private UploadContext move(File file, String newPath) throws Exception {
        try {
            try (FileInputStream fis = new FileInputStream(file)) {
                return doUpload(fis, newPath);
            }
        } finally {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    protected static String readMvnCoordsPath(File file) throws Exception {
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(file);

            String previous = null;
            String match = null;

            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("META-INF/maven/") && name.endsWith("pom.properties")) {
                    if (previous != null) {
                        throw new IllegalStateException(String.format("Duplicate pom.properties found: %s != %s", name, previous));
                    }

                    previous = name; // check for dups

                    Properties props = new Properties();
                    try (InputStream stream = jarFile.getInputStream(entry)) {
                        props.load(stream);
                    }
                    String groupId = props.getProperty("groupId");
                    String artifactId = props.getProperty("artifactId");
                    String version = props.getProperty("version");
                    String packaging = Files.getFileExtension(file.getPath());
                    match = String.format("%s/%s/%s/%s-%s.%s", groupId, artifactId, version, artifactId, version, packaging != null ? packaging : "jar");
                }
            }

            return match;
        } finally {
            if (jarFile != null) {
                jarFile.close();
            }
        }
    }

    protected ProjectRequirements toProjectRequirements(UploadContext context) {
        ProjectRequirements requirements = new ProjectRequirements();

        requirements.setParentProfiles(Collections.<String>emptyList());

        DependencyDTO rootDependency = new DependencyDTO();
        rootDependency.setGroupId(context.getGroupId());
        rootDependency.setArtifactId(context.getArtifactId());
        rootDependency.setVersion(context.getVersion());
        rootDependency.setType(context.getType());
        requirements.setRootDependency(rootDependency);

        return requirements;
    }

    protected DeployResults addToProfile(ProjectRequirements requirements) throws Exception {
        return projectDeployer.deployProject(requirements);
    }

    /**
     * Converts the path of the request to maven coords.
     * The format is the same as the one used in {@link DefaultArtifact}.
     *
     * @param path The request path, following the format: {@code <groupId>/<artifactId>/<version>/<artifactId>-<version>-[<classifier>].extension}
     * @return A {@link String} in the following format: {@code <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>}
     * @throws InvalidMavenArtifactRequest
     */
    protected String convertToMavenUrl(String path) throws InvalidMavenArtifactRequest {
        String url = null;
        StringBuilder sb = new StringBuilder();

        if (path == null) {
            throw new InvalidMavenArtifactRequest("Cannot match request path to maven url, request path is empty.");
        }
        Matcher pathMatcher = ARTIFACT_REQUEST_URL_REGEX.matcher(path);
        if (pathMatcher.matches()) {
            String groupId = pathMatcher.group(1).replaceAll("/", ".");
            String artifactId = pathMatcher.group(2);
            String version = pathMatcher.group(3);
            String filename = pathMatcher.group(4);
            String extension = "jar";
            String classifier = "";
            String filePerfix = artifactId + "-" + version;
            String stripedFileName = null;

            if (version.endsWith("SNAPSHOT")) {
                String baseVersion = version.replaceAll("-SNAPSHOT", "");
                String timestampedFileName = filename.substring(artifactId.length() + baseVersion.length() + 2);
                //Check if snapshot is timestamped and override the version. @{link Artifact} will still treat it as a SNAPSHOT.
                //and also in case of artifact installation the proper filename will be used.
                Matcher ts = SNAPSHOT_TIMESTAMP_PATTENR.matcher(timestampedFileName);
                if (ts.matches()) {
                    version = baseVersion + "-" + ts.group(1);
                    filePerfix = artifactId + "-" + version;
                }
                stripedFileName = filename.replaceAll(SNAPSHOT_TIMESTAMP_REGEX, "SNAPSHOT");
                stripedFileName = stripedFileName.substring(filePerfix.length());
            } else {
                stripedFileName = filename.substring(filePerfix.length());
            }

            if (stripedFileName != null && stripedFileName.startsWith("-") && stripedFileName.contains(".")) {
                classifier = stripedFileName.substring(1, stripedFileName.indexOf('.'));
            }
            extension = stripedFileName.substring(stripedFileName.indexOf('.') + 1);
            sb.append(groupId).append(":").append(artifactId).append(":").append(extension).append(":");
            if (classifier != null && !classifier.isEmpty()) {
                sb.append(classifier).append(":");
            }
            sb.append(version);
            url = sb.toString();
        }
        return url;
    }

    /**
     * Converts the path of the request to an {@link Artifact}.
     *
     * @param path The request path, following the format: {@code <groupId>/<artifactId>/<version>/<artifactId>-<version>-[<classifier>].extension}
     * @return A {@link DefaultArtifact} that matches the request path.
     * @throws InvalidMavenArtifactRequest
     */
    protected Artifact convertPathToArtifact(String path) throws InvalidMavenArtifactRequest {
        return new DefaultArtifact(convertToMavenUrl(path), null);
    }

    /**
     * Converts the path of the request to {@link Metadata}.
     *
     * @param path The request path, following the format: {@code <groupId>/<artifactId>/<version>/<artifactId>-<version>-[<classifier>].extension}
     * @return
     * @throws InvalidMavenArtifactRequest
     */
    protected Metadata convertPathToMetadata(String path) throws InvalidMavenArtifactRequest {
        DefaultMetadata metadata = null;
        if (path == null) {
            throw new InvalidMavenArtifactRequest("Cannot match request path to maven url, request path is empty.");
        }
        Matcher pathMatcher = ARTIFACT_METADATA_URL_REGEX.matcher(path);
        if (pathMatcher.matches()) {
            String groupId = pathMatcher.group(1).replaceAll("/", ".");
            String artifactId = pathMatcher.group(2);
            String version = pathMatcher.group(3);
            String type = pathMatcher.group(9);
            if (type == null) {
                type = "maven-metadata.xml";
            } else {
                type = "maven-metadata.xml." + type;
            }
            metadata = new DefaultMetadata(groupId, artifactId, version, type, Metadata.Nature.RELEASE_OR_SNAPSHOT);

        }
        return metadata;
    }

    /**
     * Reads a {@link File} from the {@link InputStream} then saves it under a temp location and returns the file.
     *
     * @param is           The source input stream.
     * @param tempLocation The temporary location to save the content of the stream.
     * @param name         The name of the file.
     * @return
     * @throws FileNotFoundException
     */
    protected File readFile(InputStream is, File tempLocation, String name) throws FileNotFoundException {
        File tmpFile = null;
        FileOutputStream fos = null;
        try {
            tmpFile = new File(tempLocation, name);
            if (tmpFile.exists() && !tmpFile.delete()) {
                throw new IOException("Failed to delete file");
            }
            fos = new FileOutputStream(tmpFile);

            int length;
            byte buffer[] = new byte[8192];

            while ((length = is.read(buffer)) != -1) {
                fos.write(buffer, 0, length);
            }
        } catch (Exception ex) {
        } finally {
            try {
                fos.flush();
            } catch (Exception ex) {
            }
            try {
                fos.close();
            } catch (Exception ex) {
            }
        }
        return tmpFile;
    }

    public ProjectDeployer getProjectDeployer() {
        return projectDeployer;
    }

}
