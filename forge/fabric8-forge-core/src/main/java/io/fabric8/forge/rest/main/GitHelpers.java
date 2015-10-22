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
package io.fabric8.forge.rest.main;

import io.fabric8.utils.Files;
import io.fabric8.utils.ssl.TrustEverythingSSLTrustManager;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.jboss.forge.furnace.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

/**
 */
public class GitHelpers {
    private static final transient Logger LOG = LoggerFactory.getLogger(GitHelpers.class);

    public static File getRootGitDirectory(Git git) {
        return git.getRepository().getDirectory().getParentFile();
    }

    public static String toString(Collection<RemoteRefUpdate> updates) {
        StringBuilder builder = new StringBuilder();
        for (RemoteRefUpdate update : updates) {
            if (builder.length() > 0) {
                builder.append(" ");
            }
            builder.append(update.getMessage() + " " + update.getRemoteName() + " " + update.getNewObjectId());
        }
        return builder.toString();
    }

    public static void disableSslCertificateChecks() {
        LOG.info("Trusting all SSL certificates");

        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[]{new TrustEverythingSSLTrustManager()}, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
            // bypass host name check, too.
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });
        } catch (NoSuchAlgorithmException e) {
            LOG.warn("Failed to bypass certificate check", e);
        } catch (KeyManagementException e) {
            LOG.warn("Failed to bypass certificate check", e);
        }
    }

    /**
     * Returns the remote git URL for the given branch
     */
    public static String getRemoteURL(Git git, String branch) {
        StoredConfig config = git.getRepository().getConfig();
        return config.getString("branch", branch, "remote");
    }

    public static void configureBranch(Git git, String branch, String origin, String remoteRepository) {
        // lets update the merge config
        if (!Strings.isNullOrEmpty(branch)) {
            StoredConfig config = git.getRepository().getConfig();
            config.setString("branch", branch, "remote", origin);
            config.setString("branch", branch, "merge", "refs/heads/" + branch);

            config.setString("remote", origin, "url", remoteRepository);
            config.setString("remote", origin, "fetch", "+refs/heads/*:refs/remotes/" + origin + "/*");
            try {
                config.save();
            } catch (IOException e) {
                LOG.error("Failed to save the git configuration to " + git.getRepository().getDirectory()
                        + " with branch " + branch + " on " + origin + " remote repo: " + remoteRepository + " due: " + e.getMessage() + ". This exception is ignored.", e);
            }
        }
    }

    public static void addFiles(Git git, File... files) throws GitAPIException, IOException {
        File rootDir = getRootGitDirectory(git);
        for (File file : files) {
            String relativePath = getFilePattern(rootDir, file);
            git.add().addFilepattern(relativePath).call();
        }
    }

    public static String getFilePattern(File rootDir, File file) throws IOException {
        String relativePath = Files.getRelativePath(rootDir, file);
        if (relativePath.startsWith(File.separator)) {
            relativePath = relativePath.substring(1);
        }
        return relativePath.replace(File.separatorChar, '/');
    }

    public static RevCommit doCommitAndPush(Git git, String message, CredentialsProvider credentials, PersonIdent author, String branch, String origin, boolean pushOnCommit) throws GitAPIException {
        CommitCommand commit = git.commit().setAll(true).setMessage(message);
        if (author != null) {
            commit = commit.setAuthor(author);
        }

        RevCommit answer = commit.call();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Committed " + answer.getId() + " " + answer.getFullMessage());
        }

        if (pushOnCommit) {
            Iterable<PushResult> results = git.push().setCredentialsProvider(credentials).setRemote(origin).call();
            for (PushResult result : results) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Pushed " + result.getMessages() + " " + result.getURI() + " branch: " + branch + " updates: " + toString(result.getRemoteUpdates()));
                }
            }
        }
        return answer;
    }

    public static void doAddCommitAndPushFiles(Git git, CredentialsProvider credentials, PersonIdent personIdent, String branch, String origin, String message, boolean pushOnCommit) throws GitAPIException {
        git.add().addFilepattern(".").call();
        doCommitAndPush(git, message, credentials, personIdent, branch, origin, pushOnCommit);
    }
}
