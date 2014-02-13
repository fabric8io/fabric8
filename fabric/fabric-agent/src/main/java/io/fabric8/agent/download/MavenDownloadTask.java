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
package io.fabric8.agent.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import javax.xml.parsers.ParserConfigurationException;

import io.fabric8.agent.mvn.DownloadableArtifact;
import io.fabric8.agent.mvn.MavenConfiguration;
import io.fabric8.agent.mvn.MavenRepositoryURL;
import io.fabric8.agent.mvn.Parser;
import io.fabric8.agent.mvn.Version;
import io.fabric8.agent.mvn.VersionRange;
import io.fabric8.agent.utils.URLUtils;
import io.fabric8.agent.utils.XmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class MavenDownloadTask extends AbstractDownloadTask implements Runnable {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDownloadTask.class);
    /**
     * 2 spaces indent;
     */
    private static final String Ix2 = "  ";
    /**
     * 4 spaces indent;
     */
    private static final String Ix4 = "    ";

    private final MavenRepositoryURL cache;
    private final MavenRepositoryURL system;
    private final MavenConfiguration configuration;

    public MavenDownloadTask(String url, MavenRepositoryURL cache, MavenRepositoryURL system, MavenConfiguration configuration, ExecutorService executor) {
        super(url, executor);
        this.cache = cache;
        this.system = system;
        this.configuration = configuration;
    }

    protected File download() throws Exception {
        Parser parser = new Parser(url.substring("mvn:".length()));
        Set<DownloadableArtifact> downloadables;
        if (!parser.getVersion().contains("SNAPSHOT")) {
            downloadables = doCollectPossibleDownloads(parser, Arrays.asList(cache, system, configuration.getLocalRepository()));
            for (DownloadableArtifact artifact : downloadables) {
                URL url = artifact.getArtifactURL();
                File file = new File(url.getFile());
                if (file.exists()) {
                    return file;
                }
            }
        }
        downloadables = collectPossibleDownloads(parser);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Possible download locations for [" + url + "]");
            for (DownloadableArtifact artifact : downloadables) {
                LOG.trace("  " + artifact);
            }
        }
        for (DownloadableArtifact artifact : downloadables) {
            LOG.trace("Downloading [" + artifact + "]");
            try {
                configuration.enableProxy(artifact.getArtifactURL());
                String repository = cache.getFile().getAbsolutePath();
                if (!repository.endsWith(File.separator)) {
                    repository = repository + File.separator;
                }
                InputStream is = artifact.getInputStream();
                File file = new File(repository + parser.getArtifactPath());
                file.getParentFile().mkdirs();
                if (!file.getParentFile().isDirectory()) {
                    throw new IOException("Unable to create directory " + file.getParentFile().toString());
                }
                File tmp = File.createTempFile("fabric-agent-", null, file.getParentFile());
                OutputStream os = new FileOutputStream(tmp);
                copy(is, os);
                is.close();
                os.close();
                if (file.exists() && !file.delete()) {
                    throw new IOException("Unable to delete file: " + file.toString());
                }
                if (!tmp.renameTo(file)) {
                    throw new IOException("Unable to rename file " + tmp.toString() + " to " + file.toString());
                }
                return file;
            } catch (IOException ignore) {
                // go on with next repository
                LOG.debug(Ix2 + "Could not download [" + artifact + "]");
                LOG.trace(Ix2 + "Reason [" + ignore.getClass().getName() + ": " + ignore.getMessage() + "]");
            }
        }
        // no artifact found
        throw new IOException("URL [" + url + "] could not be resolved.");
    }

    /**
     * Searches all available repositories for possible artifacts to download. The returned set of downloadable
     * artifacts (never null, but maybe empty) will be sorted descending by version of the artifact and by positon of
     * repository in the list of repositories to be searched.
     *
     * @return a non null sorted set of artifacts
     * @throws java.net.MalformedURLException re-thrown
     */
    private Set<DownloadableArtifact> collectPossibleDownloads(final Parser parser)
            throws MalformedURLException {
        final List<MavenRepositoryURL> repositories = new ArrayList<MavenRepositoryURL>();
        repositories.addAll(configuration.getRepositories());
        repositories.add(configuration.getLocalRepository());
        repositories.add(system);
        repositories.add(cache);
        // if the url contains a preferred repository add that repository as the first repository to be searched
        if (parser.getRepositoryURL() != null) {
            repositories.add(
                    repositories.size() == 0 ? 0 : 1,
                    parser.getRepositoryURL()
            );
        }
        return doCollectPossibleDownloads(parser, repositories);
    }

    /**
     * Search the default repositories for possible artifacts to download.
     */
    private Set<DownloadableArtifact> collectDefaultPossibleDownloads(final Parser parser)
            throws MalformedURLException {
        return doCollectPossibleDownloads(parser, configuration.getDefaultRepositories());
    }

    private Set<DownloadableArtifact> doCollectPossibleDownloads(final Parser parser,
                                                                 final List<MavenRepositoryURL> repositories)
            throws MalformedURLException {
        final Set<DownloadableArtifact> downloadables = new TreeSet<DownloadableArtifact>(new DownloadComparator());

        // find artifact type
        final boolean isLatest = parser.getVersion().contains("LATEST");
        final boolean isSnapshot = parser.getVersion().endsWith("SNAPSHOT");
        VersionRange versionRange = null;
        if (!isLatest && !isSnapshot) {
            try {
                versionRange = new VersionRange(parser.getVersion());
            } catch (Exception ignore) {
                // well, we do not have a range of versions
            }
        }
        final boolean isVersionRange = versionRange != null;
        final boolean isExactVersion = !(isLatest || isSnapshot || isVersionRange);

        int priority = 0;
        for (MavenRepositoryURL repositoryURL : repositories) {
            LOG.debug("Collecting versions from repository [" + repositoryURL + "]");
            priority++;
            try {
                if (isExactVersion) {
                    downloadables.add(resolveExactVersion(parser, repositoryURL, priority));
                } else if (isSnapshot) {
                    final DownloadableArtifact snapshot =
                            resolveSnapshotVersion(parser, repositoryURL, priority, parser.getVersion());
                    downloadables.add(snapshot);
                    // if we have a local built snapshot we skip the rest of repositories
                    if (snapshot.isLocalSnapshotBuild()) {
                        break;
                    }
                } else {
                    final Document metadata = getMetadata(repositoryURL.getURL(),
                            new String[]
                                    {
                                            parser.getArtifactLocalMetdataPath(),
                                            parser.getArtifactMetdataPath()
                                    }
                    );
                    if (isLatest) {
                        downloadables.add(resolveLatestVersion(parser, metadata, repositoryURL, priority));
                    } else {
                        downloadables.addAll(resolveRangeVersions(parser, metadata, repositoryURL, priority, versionRange));
                    }
                }
            } catch (IOException ignore) {
                // if metadata cannot be found we go on with the next repository. Maybe we have better luck.
                LOG.debug(Ix2 + "Skipping repository [" + repositoryURL + "], reason: " + ignore.getMessage());
            }
        }
        return downloadables;
    }

    /**
     * Returns maven metadata by looking first for a local metatdata xml file and then for a remote one.
     * If no metadata file is found or cannot be used an IOException is thrown.
     *
     * @param repositoryURL     url of the repository from where the metadata should be parsed
     * @param metadataLocations array of location paths to try as metadata
     * @return parsed xml document for the metadata file
     * @throws java.io.IOException if:
     *                             metadata file cannot be located
     */
    private Document getMetadata(final URL repositoryURL,
                                 final String[] metadataLocations)
            throws IOException {
        LOG.debug(Ix2 + "Resolving metadata");
        InputStream inputStream = null;
        String foundLocation = null;
        for (String location : metadataLocations) {
            try {
                // first try to get the artifact local metadata
                inputStream = prepareInputStream(repositoryURL, location);
                // get out at first found location
                foundLocation = location;
                LOG.trace(Ix4 + "Metadata found: [" + location + "]");
                break;
            } catch (IOException ignore) {
                LOG.trace(Ix4 + "Metadata not found: [" + location + "]");
            }
        }
        if (inputStream == null) {
            throw new IOException("Metadata not found in repository [" + repositoryURL + "]");
        }
        try {
            return XmlUtils.parseDoc(inputStream);
        } catch (ParserConfigurationException e) {
            throw initIOException("Metadata [" + foundLocation + "] could not be parsed.", e);
        } catch (SAXException e) {
            throw initIOException("Metadata [" + foundLocation + "] could not be parsed.", e);
        }
    }

    /**
     * Returns a downloadable artifact where the version is fully specified.
     *
     * @param repositoryURL the url of the repository to download from
     * @param priority      repository priority
     * @return a downloadable artifact
     * @throws IOException re-thrown
     */
    private DownloadableArtifact resolveExactVersion(final Parser parser,
                                                     final MavenRepositoryURL repositoryURL,
                                                     final int priority)
            throws IOException {
        if (!repositoryURL.isReleasesEnabled()) {
            throw new IOException("Releases not enabled");
        }
        LOG.debug(Ix2 + "Resolving exact version");
        return new DownloadableArtifact(
                parser.getVersion(),
                priority,
                repositoryURL.getURL(),
                parser.getArtifactPath(),
                false, // no local built snapshot
                configuration.getCertificateCheck()
        );
    }

    /**
     * Resolves the latest version of the artifact.
     *
     * @param metadata      parsed metadata xml
     * @param repositoryURL the url of the repository to download from
     * @param priority      repository priority
     * @return a downloadable artifact or throw an IOException if latest version cannot be determined.
     * @throws IOException if the artifact could not be resolved
     */
    private DownloadableArtifact resolveLatestVersion(final Parser parser,
                                                      final Document metadata,
                                                      final MavenRepositoryURL repositoryURL,
                                                      final int priority)
            throws IOException {
        LOG.debug(Ix2 + "Resolving latest version");
        final String version = XmlUtils.getTextContentOfElement(metadata, "versioning/versions/version[last]");
        if (version != null) {
            if (version.endsWith("SNAPSHOT")) {
                return resolveSnapshotVersion(parser, repositoryURL, priority, version);
            } else {
                return new DownloadableArtifact(
                        version,
                        priority,
                        repositoryURL.getURL(),
                        parser.getArtifactPath(version),
                        false, // no local built snapshot
                        configuration.getCertificateCheck()
                );
            }
        }
        throw new IOException("LATEST version could not be resolved.");
    }

    /**
     * Resolves snapshot version of the artifact.
     * Snapshot versions are resolved by parsing the metadata within the directory that contains the version as:
     * 1. if the metadata contains entries like "versioning/snapshot/timestamp (most likely on remote repos) it will
     * use the timestamp and buildnumber to point the real version
     * 2. if the metatdata does not contain the above (most likely a local repo) it will use as version the
     * versioning/lastUpdated
     *
     * @param repositoryURL the url of the repository to download from
     * @param priority      repository priority
     * @param version       snapshot version to resolve
     * @return an input stream to the artifact
     * @throws IOException if the artifact could not be resolved
     */
    private DownloadableArtifact resolveSnapshotVersion(final Parser parser,
                                                        final MavenRepositoryURL repositoryURL,
                                                        final int priority,
                                                        final String version)
            throws IOException {
        if (!repositoryURL.isSnapshotsEnabled()) {
            throw new IOException("Snapshots not enabled");
        }
        LOG.debug(Ix2 + "Resolving snapshot version [" + version + "]");
        try {
            final Document snapshotMetadata = getMetadata(repositoryURL.getURL(),
                    new String[]
                            {
                                    parser.getVersionLocalMetadataPath(version),
                                    parser.getVersionMetadataPath(version)
                            }
            );
            final String timestamp =
                    XmlUtils.getTextContentOfElement(snapshotMetadata, "versioning/snapshot/timestamp");
            final String buildNumber =
                    XmlUtils.getTextContentOfElement(snapshotMetadata, "versioning/snapshot/buildNumber");
            final String localSnapshot =
                    XmlUtils.getTextContentOfElement(snapshotMetadata, "versioning/snapshot/localCopy");
            if (timestamp != null && buildNumber != null) {
                return new DownloadableArtifact(
                        parser.getSnapshotVersion(version, timestamp, buildNumber),
                        priority,
                        repositoryURL.getURL(),
                        parser.getSnapshotPath(version, timestamp, buildNumber),
                        localSnapshot != null,
                        configuration.getCertificateCheck()
                );
            } else {
                String lastUpdated = XmlUtils.getTextContentOfElement(snapshotMetadata, "versioning/lastUpdated");
                if (lastUpdated != null) {
                    // last updated should contain in the first 8 chars the date and then the time,
                    // fact that is not compatible with timeStamp from remote repos which has a "." after date
                    if (lastUpdated.length() > 8) {
                        lastUpdated = lastUpdated.substring(0, 8) + "." + lastUpdated.substring(8);
                        return new DownloadableArtifact(
                                parser.getSnapshotVersion(version, lastUpdated, "0"),
                                priority,
                                repositoryURL.getURL(),
                                parser.getArtifactPath(version),
                                localSnapshot != null,
                                configuration.getCertificateCheck()
                        );
                    }
                }
            }
        } catch (IOException ignore) {
            // in this case we could not find any metadata so try to get the *-SNAPSHOT file directly
        }
        return new DownloadableArtifact(
                parser.getVersion(),
                priority,
                repositoryURL.getURL(),
                parser.getArtifactPath(),
                false, // no local built snapshot
                configuration.getCertificateCheck()
        );
    }

    /**
     * Resolves all versions that fits the provided range.
     *
     * @param metadata      parsed metadata xml
     * @param repositoryURL the url of the repository to download from
     * @param priority      repository priority
     * @param versionRange  version range to fulfill
     * @return list of downloadable artifacts that match the range
     * @throws IOException re-thrown
     */
    private List<DownloadableArtifact> resolveRangeVersions(final Parser parser,
                                                            final Document metadata,
                                                            final MavenRepositoryURL repositoryURL,
                                                            final int priority,
                                                            final VersionRange versionRange)
            throws IOException {
        LOG.debug(Ix2 + "Resolving versions in range [" + versionRange + "]");
        final List<DownloadableArtifact> downladables = new ArrayList<DownloadableArtifact>();
        final List<Element> elements = XmlUtils.getElements(metadata, "versioning/versions/version");
        if (elements != null && elements.size() > 0) {
            for (Element element : elements) {
                final String versionString = XmlUtils.getTextContent(element);
                if (versionString != null) {
                    final Version version = new Version(versionString);
                    if (versionRange.includes(version)) {
                        if (versionString.endsWith("SNAPSHOT")) {
                            downladables.add(
                                    resolveSnapshotVersion(parser, repositoryURL, priority, versionString)
                            );
                        } else {
                            downladables.add(
                                    new DownloadableArtifact(
                                            versionString,
                                            priority,
                                            repositoryURL.getURL(),
                                            parser.getArtifactPath(versionString),
                                            false, // no local built snapshot
                                            configuration.getCertificateCheck()
                                    )
                            );
                        }
                    }
                }
            }
        }
        return downladables;
    }

    /**
     * @param repositoryURL url to reporsitory
     * @param path          a path to the artifact jar file
     * @return prepared input stream
     * @throws IOException re-thrown
     * @see org.ops4j.net.URLUtils#prepareInputStream(java.net.URL, boolean)
     */
    private InputStream prepareInputStream(URL repositoryURL, final String path)
            throws IOException {
        String repository = repositoryURL.toExternalForm();
        if (!repository.endsWith(org.ops4j.pax.url.mvn.internal.Parser.FILE_SEPARATOR)) {
            repository = repository + org.ops4j.pax.url.mvn.internal.Parser.FILE_SEPARATOR;
        }
        configuration.enableProxy(repositoryURL);
        final URL url = new URL(repository + path);
        LOG.trace("Reading " + url.toExternalForm());
        return URLUtils.prepareInputStream(url, !configuration.getCertificateCheck());
    }

    /**
     * Sorting comparator for downladable artifacts.
     * The sorting is done by:
     * 1. descending version
     * 2. ascending priority.
     */
    private static class DownloadComparator
            implements Comparator<DownloadableArtifact> {

        public int compare(final DownloadableArtifact first,
                           final DownloadableArtifact second) {
            // first descending by version
            int result = -1 * first.getVersion().compareTo(second.getVersion());
            if (result == 0) {
                // then ascending by priority
                if (first.getPriority() < second.getPriority()) {
                    result = -1;
                } else if (first.getPriority() > second.getPriority()) {
                    result = 1;
                }
            }
            return result;
        }

    }
}
