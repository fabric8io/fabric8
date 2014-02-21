/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.fabric8.maven.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServlet;

import com.google.common.base.Strings;
import org.apache.maven.repository.internal.DefaultServiceLocator;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.file.FileWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpsWagon;
import io.fabric8.maven.MavenProxy;
import io.fabric8.maven.util.MavenUtils;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.installation.InstallRequest;
import org.sonatype.aether.metadata.Metadata;
import org.sonatype.aether.repository.Authentication;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.repository.RepositoryPolicy;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.resolution.MetadataRequest;
import org.sonatype.aether.resolution.MetadataResult;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.util.DefaultRepositoryCache;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.metadata.DefaultMetadata;

public class MavenProxyServletSupport extends HttpServlet implements MavenProxy {

    protected static final Logger LOGGER = Logger.getLogger(MavenProxyServletSupport.class.getName());

    private static final String SNAPSHOT_TIMESTAMP_REGEX = "^([0-9]{8}.[0-9]{6}-[0-9]+).*";
    private static final Pattern SNAPSHOT_TIMESTAMP_PATTENR = Pattern.compile(SNAPSHOT_TIMESTAMP_REGEX);

    //The pattern below matches a path to the following:
    //1: groupId
    //2: artifactId
    //3: version
    //4: atifact filename
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

    protected Map<String, RemoteRepository> repositories;
    protected RepositorySystem system;
    protected RepositorySystemSession session;
    protected File tmpFolder = new File(System.getProperty("karaf.data") + File.separator + "maven" + File.separator + "proxy" + File.separator + "tmp");

    final String localRepository;
    final List<String> remoteRepositories;
    final boolean appendSystemRepos;

    final String updatePolicy;
    final String checksumPolicy;

    final String proxyProtocol;
    final String proxyHost;
    final int proxyPort;
    final String proxyUsername;
    final String proxyPassword;
    final String proxyNonProxyHosts;

    public MavenProxyServletSupport(String localRepository, List<String> remoteRepositories, boolean appendSystemRepos, String updatePolicy, String checksumPolicy, String proxyProtocol, String proxyHost, int proxyPort, String proxyUsername, String proxyPassword, String proxyNonProxyHosts) {
        this.localRepository = localRepository;
        this.remoteRepositories = remoteRepositories;
        this.appendSystemRepos = appendSystemRepos;
        this.updatePolicy = updatePolicy;
        this.checksumPolicy = checksumPolicy;
        this.proxyProtocol = proxyProtocol;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUsername = proxyUsername;
        this.proxyPassword = proxyPassword;
        this.proxyNonProxyHosts = proxyNonProxyHosts;
    }

    public synchronized void start() throws IOException {
        if (!tmpFolder.exists() && !tmpFolder.mkdirs()) {
            throw new IOException("Failed to create temporary artifact folder");
        }
        if (system == null) {
            system = newRepositorySystem();
        }
        if (session == null) {
            session = newSession(system, localRepository);
        }

        repositories = new HashMap<String, RemoteRepository>();

        if (remoteRepositories != null) {
            for (String rep : remoteRepositories) {
                RemoteRepository remoteRepository = createRemoteRepository(rep);
                remoteRepository.setPolicy(true, new RepositoryPolicy(true, updatePolicy, checksumPolicy));
                remoteRepository.setProxy(session.getProxySelector().getProxy(remoteRepository));
                repositories.put(remoteRepository.getId(), remoteRepository);
            }
        }

        RemoteRepository local = new RemoteRepository("local", DEFAULT_REPO_ID, "file://" + localRepository);
        local.setPolicy(true, new RepositoryPolicy(true, updatePolicy, checksumPolicy));
        repositories.put("local", local);

        RemoteRepository karaf = new RemoteRepository("karaf", DEFAULT_REPO_ID, "file://" + System.getProperty("karaf.home") + File.separator + System.getProperty("karaf.default.repository"));
        karaf.setPolicy(true, new RepositoryPolicy(true, updatePolicy, checksumPolicy));
        repositories.put("karaf", karaf);

        RemoteRepository user = new RemoteRepository("user", DEFAULT_REPO_ID, "file://" + System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository");
        user.setPolicy(true, new RepositoryPolicy(true, updatePolicy, checksumPolicy));

        repositories.put("user", user);
        if (appendSystemRepos) {
            for (RemoteRepository sysRepo : MavenUtils.getRemoteRepositories()) {
                sysRepo.setProxy(session.getProxySelector().getProxy(sysRepo));
                repositories.put(sysRepo.getId(), sysRepo);
            }
        }
    }

    public synchronized void stop() {
    }

    @Override
    public File download(String path) throws InvalidMavenArtifactRequest {
        Matcher artifactMatcher = ARTIFACT_REQUEST_URL_REGEX.matcher(path);
        Matcher metdataMatcher = ARTIFACT_METADATA_URL_REGEX.matcher(path);

        if (path == null) {
            throw new InvalidMavenArtifactRequest();
        } else if (metdataMatcher.matches()) {
            LOGGER.log(Level.INFO, String.format("Received request for maven metadata : %s", path));
            Metadata metadata = null;
            try {
                metadata = convertPathToMetadata(path);
                List<MetadataRequest> requests = new ArrayList<MetadataRequest>();
                String id = metdataMatcher.group(7);
                if (repositories.containsKey(id)) {
                    MetadataRequest request = new MetadataRequest(metadata, repositories.get(id), null);
                    request.setFavorLocalRepository(false);
                    requests.add(request);
                } else {
                    for (RemoteRepository repository : repositories.values()) {
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
            String id = artifact.getGroupId() + ":" + artifact.getArtifactId();
            try {
                ArtifactRequest request = new ArtifactRequest(artifact, new ArrayList<RemoteRepository>(repositories.values()), null);
                ArtifactResult result = system.resolveArtifact(session, request);
                return result.getArtifact().getFile();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, String.format("Could not find artifact : %s due to %s", artifact, e));
                return null;
            }
        }
        return null;
    }

    @Override
    public boolean upload(InputStream is, String path) throws InvalidMavenArtifactRequest {
        boolean success = true;
        Matcher artifactMatcher = ARTIFACT_REQUEST_URL_REGEX.matcher(path);
        Matcher metdataMatcher = ARTIFACT_METADATA_URL_REGEX.matcher(path);
        if (path == null) {
            throw new InvalidMavenArtifactRequest();
        } else if (metdataMatcher.matches()) {
            LOGGER.log(Level.INFO, String.format("Received upload request for maven metadata : %s", path));
            try {
                String filename = path.substring(path.lastIndexOf('/') + 1);
                Metadata metadata = convertPathToMetadata(path);
                metadata = metadata.setFile(readFile(is, tmpFolder, filename));
                InstallRequest request = new InstallRequest();
                request.addMetadata(metadata);
                system.install(session, request);
                success = true;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, String.format("Failed to upload metadata: %s due to %s", path, e));
                success = false;
            }
            //If no matching metadata found return nothing
        } else if (artifactMatcher.matches()) {
            LOGGER.log(Level.INFO, String.format("Received upload request for maven artifact : %s", path));
            Artifact artifact = null;
            try {
                String filename = path.substring(path.lastIndexOf('/') + 1);
                artifact = convertPathToArtifact(path);
                artifact = artifact.setFile(readFile(is, tmpFolder, filename));
                InstallRequest request = new InstallRequest();
                request.addArtifact(artifact);
                system.install(session, request);
                success = true;
                LOGGER.log(Level.INFO, "Artifact installed: " + artifact.toString());
            } catch (Exception e) {
                success = false;
                LOGGER.log(Level.WARNING, String.format("Failed to upload artifact : %s due to %s", artifact, e), e);
            }
        }
        return success;

    }

    protected RepositorySystemSession newSession(RepositorySystem system, String localRepository) {

        DefaultRepositorySystemSession session = new MavenRepositorySystemSession();
        session.setOffline(false);
        session.setProxySelector(MavenUtils.getProxySelector(proxyProtocol, proxyHost, proxyPort, proxyNonProxyHosts, proxyUsername, proxyPassword));
        session.setMirrorSelector(MavenUtils.getMirrorSelector());
        session.setAuthenticationSelector(MavenUtils.getAuthSelector());
        session.setCache(new DefaultRepositoryCache());
        session.setUpdatePolicy(updatePolicy);
        session.setChecksumPolicy(checksumPolicy);
        LocalRepository localRepo = new LocalRepository(localRepository);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(localRepo));
        return session;
    }

    protected RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = new DefaultServiceLocator();
        locator.setServices(WagonProvider.class, new ManualWagonProvider());
        locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);
        locator.setService(org.sonatype.aether.spi.log.Logger.class, LogAdapter.class);
        return locator.getService(RepositorySystem.class);
    }

    /**
     * Creates a {@link RemoteRepository} for the specified url.
     * @param repositoryUrl The repository URL.
     * @return
     */
    static RemoteRepository createRemoteRepository(String repositoryUrl) {
        String id;
        RemoteRepository remoteRepository = null;
        repositoryUrl = repositoryUrl.trim();
        Authentication authentication = getAuthentication(repositoryUrl);
        if (authentication != null) {
            repositoryUrl = repositoryUrl.replaceFirst(String.format("%s:%s@", authentication.getUsername(), authentication.getPassword()), "");
        }

        Matcher idMatcher = REPOSITORY_ID_REGEX.matcher(repositoryUrl);
        if (idMatcher.matches()) {
            id = idMatcher.group(2);
            repositoryUrl = cleanUpRepositorySpec(repositoryUrl);
            remoteRepository = new RemoteRepository(id + Math.abs(repositoryUrl.hashCode()), DEFAULT_REPO_ID, repositoryUrl);
        } else {
            id = "rep-" + Math.abs(repositoryUrl.hashCode());
            repositoryUrl = cleanUpRepositorySpec(repositoryUrl);
            remoteRepository = new RemoteRepository("repo-" + Math.abs(repositoryUrl.hashCode()), DEFAULT_REPO_ID, repositoryUrl);
        }
        remoteRepository.setId(id);
        if (authentication != null) {
            remoteRepository.setAuthentication(authentication);
        }
        return remoteRepository;
    }

    /**
     * Get the {@link Authentication} instance if the URL contains credentials, otherwise return null.
     * @param repositoryUrl
     * @return
     */
     static Authentication getAuthentication(String repositoryUrl) {
        Authentication authentication = null;
        try {
            URL url = new URL(repositoryUrl);
            String authority = url.getUserInfo();
            if (!Strings.isNullOrEmpty(authority)) {
                String[] parts = authority.split(":");
                if (parts.length == 2) {
                    authentication = new Authentication(parts[0], parts[1]);
                }
            }
        } catch (MalformedURLException e) {
            LOGGER.warning("{} does not look like a valid repository URL");
        }
        return authentication;
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

    /**
     * Removes all options from the repository spec.
     *
     * @param spec
     * @return
     */
    static String cleanUpRepositorySpec(String spec) {
        if (spec == null || spec.isEmpty()) {
            return spec;
        } else if (!spec.contains("@")) {
            return spec;
        } else {
            return spec.substring(0, spec.indexOf('@'));
        }
    }

    public String getLocalRepository() {
        return localRepository;
    }

    public List<String> getRemoteRepositories() {
        return remoteRepositories;
    }

    public boolean isAppendSystemRepos() {
        return appendSystemRepos;
    }

    public String getUpdatePolicy() {
        return updatePolicy;
    }

    public String getChecksumPolicy() {
        return checksumPolicy;
    }

    public String getProxyProtocol() {
        return proxyProtocol;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public String getProxyUsername() {
        return proxyUsername;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public String getProxyNonProxyHosts() {
        return proxyNonProxyHosts;
    }

    public static class LogAdapter implements org.sonatype.aether.spi.log.Logger {

        public boolean isDebugEnabled() {
            return LOGGER.isLoggable(Level.FINE);
        }

        public void debug(String msg) {
            LOGGER.log(Level.FINE, msg);
        }

        public void debug(String msg, Throwable error) {
            LOGGER.log(Level.FINE, msg, error);
        }

        public boolean isWarnEnabled() {
            return LOGGER.isLoggable(Level.WARNING);
        }

        public void warn(String msg) {
            LOGGER.log(Level.WARNING, msg);
        }

        public void warn(String msg, Throwable error) {
            LOGGER.log(Level.WARNING, msg, error);
        }
    }

    public static class ManualWagonProvider implements WagonProvider {

        public Wagon lookup(String roleHint)
                throws Exception {
            if ("file".equals(roleHint)) {
                return new FileWagon();
            } else if ("http".equals(roleHint)) {
                return new LightweightHttpWagon();
            } else if ("https".equals(roleHint)) {
                return new LightweightHttpsWagon();
            }
            return null;
        }

        public void release(Wagon wagon) {

        }
    }
}
