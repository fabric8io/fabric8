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

package org.fusesource.fabric.maven.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;

import org.apache.maven.repository.internal.DefaultServiceLocator;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.fusesource.fabric.maven.MavenProxy;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.installation.InstallRequest;
import org.sonatype.aether.installation.InstallResult;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.repository.RepositoryPolicy;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.util.artifact.DefaultArtifact;

public class MavenProxyServletSupport extends HttpServlet implements MavenProxy {

    protected static final Logger LOGGER = Logger.getLogger(MavenProxyServletSupport.class.getName());

    protected String localRepository;
    protected String remoteRepositories = "repo1.maven.org/maven2,repo.fusesource.com/nexus/content/groups/public,repo.fusesource.com/nexus/content/groups/releases,repo.fusesource.com/nexus/content/groups/public-snapshots,repo.fusesource.com/nexus/content/groups/ea";
    protected String updatePolicy;
    protected String checksumPolicy;

    protected List<RemoteRepository> repositories;
    protected RepositorySystem system;
    protected RepositorySystemSession session;

    protected ConcurrentMap<String, Object> artifactLocks = new ConcurrentHashMap<String, Object>();

    protected File tmpFolder = new File(System.getProperty("karaf.home") + File.separator + "data" + File.separator + "maven" + File.separator);

    public synchronized void start() throws IOException {
        if (!tmpFolder.exists()) {
            tmpFolder.mkdirs();
        }

        if (localRepository.equals("")) {
            //It doesn't work when using the file:// protocol prefix.
            localRepository = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";
        }
        if (system == null) {
            system = newRepositorySystem();
        }
        if (session == null) {
            session = newSession(system, localRepository);
        }
        repositories = new ArrayList<RemoteRepository>();
        repositories.add(new RemoteRepository("local", "default", "file://" + localRepository));
        repositories.add(new RemoteRepository("karaf.default.repo", "default", "file://" + System.getProperty("karaf.home") + File.separator + System.getProperty("karaf.default.repository")));

        int i = 0;
        for (String rep : remoteRepositories.split(",")) {
            RemoteRepository remoteRepository = new RemoteRepository("repo-" + i++, "default", rep);
            remoteRepository.setPolicy(true, new RepositoryPolicy(true, updatePolicy, checksumPolicy));
            repositories.add(remoteRepository);
        }
    }

    public synchronized void stop() {
    }

    @Override
    public File download(String path) throws InvalidMavenArtifactRequest {
        String mvn = convertToMavenUrl(path);
        if (mvn == null) {
            LOGGER.log(Level.WARNING, String.format("Received non maven request : %s", path));
            return null;
        } else {
            LOGGER.log(Level.INFO, String.format("Received request for file : %s", mvn));
        }

        Artifact artifact = new DefaultArtifact(mvn, null);
        String id = artifact.getGroupId() + ":" + artifact.getArtifactId();
        artifactLocks.putIfAbsent(id, new Object());
        final Object lock = artifactLocks.get(id);
        synchronized (lock) {
            try {
                ArtifactRequest request = new ArtifactRequest(artifact, repositories, null);
                ArtifactResult result = system.resolveArtifact(session, request);
                return result.getArtifact().getFile();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, String.format("Could not find file : %s due to %s", mvn, e));
                return null;
            }
        }
    }

    @Override
    public boolean upload(InputStream is, String path) throws InvalidMavenArtifactRequest {
        boolean success = true;
        FileOutputStream fos = null;
        String filename = path.substring(path.lastIndexOf("/") + 1);

        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        String mvn = convertToMavenUrl(path);
        if (mvn != null) {

            try {
                File tmpFile = new File(tmpFolder, filename);
                if (tmpFile.exists()) {
                    tmpFile.delete();
                }
                fos = new FileOutputStream(tmpFile);

                int length = 0;
                byte buffer[] = new byte[4096];

                while ((length = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, length);
                }

                fos.flush();
                fos.close();


                Artifact artifact = new DefaultArtifact(mvn, null);
                artifact = artifact.setFile(tmpFile);
                InstallRequest request = new InstallRequest();
                request.addArtifact(artifact);
                InstallResult result = system.install(session, request);
                LOGGER.log(Level.INFO, "Artifact installed: " + mvn);
            } catch (Exception e) {
                success = false;
                LOGGER.log(Level.WARNING, String.format("Could not find artifact : %s due to %s", mvn, e), e);
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (Exception ex) {
                    }
                }
            }
        }
        return success;
    }


    protected RepositorySystemSession newSession(RepositorySystem system, String localRepository) {
        MavenRepositorySystemSession session = new MavenRepositorySystemSession();
        LocalRepository localRepo = new LocalRepository(localRepository);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(localRepo));
        return session;
    }

    protected RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = new DefaultServiceLocator();
        locator.setServices(WagonProvider.class, new MavenProxyImpl.ManualWagonProvider());
        locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);
        locator.setService(org.sonatype.aether.spi.log.Logger.class, LogAdapter.class);
        return locator.getService(RepositorySystem.class);
    }

    public String convertToMavenUrl(String location) throws InvalidMavenArtifactRequest {
        if (location == null) {
            throw new InvalidMavenArtifactRequest("Cannot match request path to maven url, request path is empty.");
        }
        String[] p = location.split("/");
        if (p.length >= 4) {
            String filename = p[p.length - 1];
            String version = p[p.length - 2];
            String artifactId = p[p.length - 3];
            String artifactBase = artifactId + "-" + version;

            if (version.contains("SNAPSHOT")) {
                filename = filename.replaceAll("\\d{8}.\\d+-\\d+", "SNAPSHOT");
            }

            if (filename.startsWith(artifactBase)) {
                String classifier;
                String type;
                String artifactIdVersion = artifactId + "-" + version;
                StringBuffer sb = new StringBuffer();
                if (p[p.length - 1].charAt(artifactIdVersion.length()) == '-') {
                    classifier = p[p.length - 1].substring(artifactIdVersion.length() + 1, p[p.length - 1].lastIndexOf('.'));
                    artifactIdVersion += "-" + classifier;
                } else {
                    classifier = "";
                }
                type = filename.substring(artifactIdVersion.length() + 1);
                for (int j = 0; j < p.length - 3; j++) {
                    if (j > 0) {
                        sb.append('.');
                    }
                    sb.append(p[j]);
                }
                sb.append(':').append(artifactId).append(':').append(type);
                if (classifier.length() > 0) {
                    sb.append(":").append(classifier);
                }
                sb.append(":").append(version);
                return sb.toString();
            } else {
                //We don't want to throw an exception here as it may break the upload.
                return null;
            }
        } else {
            return null;
        }
    }

    public void setLocalRepository(String localRepository) {
        this.localRepository = localRepository;
    }

    public String getRemoteRepositories() {
        return remoteRepositories;
    }

    public void setRemoteRepositories(String remoteRepositories) {
        this.remoteRepositories = remoteRepositories;
    }

    public List<RemoteRepository> getRepositories() {
        return repositories;
    }

    public String getUpdatePolicy() {
        return updatePolicy;
    }

    public void setUpdatePolicy(String updatePolicy) {
        this.updatePolicy = updatePolicy;
    }

    public String getChecksumPolicy() {
        return checksumPolicy;
    }

    public void setChecksumPolicy(String checksumPolicy) {
        this.checksumPolicy = checksumPolicy;
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
}
