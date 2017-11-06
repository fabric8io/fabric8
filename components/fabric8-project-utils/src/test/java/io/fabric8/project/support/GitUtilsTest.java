/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.project.support;

import org.eclipse.jgit.lib.Repository;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

/**
 */
public class GitUtilsTest {
    @Test
    public void testGetRepositoryURL() throws Exception {
        Repository repository = assertRepos();

        String url = GitUtils.getRemoteURL(repository);
        System.out.println("Found git repository URL: " + url);
        assertThat(url).isNotEmpty().contains(".git");
    }

    private Repository assertRepos() throws IOException {
        File basedir = new File(System.getProperty("basedir", "."));
        Repository repository = GitUtils.findRepository(basedir);
        assertNotNull("Should find a repository", repository);
        return repository;
    }

    @Test
    public void testGetRepositoryHttpsURL() throws Exception {
        Pattern GITHUB_HTTPS_URL_PATTERN = Pattern.compile("^https://github\\.com/(?<user>[a-z0-9](?:-?[a-z0-9]){0,38})/.*?$");

        Repository repository = assertRepos();

        String url = GitUtils.getRemoteAsHttpsURL(repository);
        System.out.println("Found git repository URL: " + url);
        assertThat(GITHUB_HTTPS_URL_PATTERN.matcher(url).find()).isTrue();
    }

    @Test
    public void testGetRepositoryHttpsAsURLWithRemoteName() throws Exception {
        Pattern GITHUB_HTTPS_URL_PATTERN = Pattern.compile("^https://github\\.com/(?<user>[a-z0-9](?:-?[a-z0-9]){0,38})/.*?$");

        Repository repository = assertRepos();

        String url = GitUtils.getRemoteAsHttpsURL(repository,"origin");
        System.out.println("Found git repository URL: " + url);
        assertThat(GITHUB_HTTPS_URL_PATTERN.matcher(url).find()).isTrue();
    }

    @Test
    public void testGitHostName() throws Exception {
        assertHostName("ssh://user@server/project.git", "server");
        assertHostName("user@cheese:project.git", "cheese");
        assertHostName("git@github.com:fabric8-quickstarts/spring-boot-webmvc.git", "github.com");
        assertHostName("https://github.com/fabric8-quickstarts/spring-boot-webmvc.git", "github.com");
        assertHostName("foo/bar", null);
    }

    @Test
    public void testGitProtocol() throws Exception {
        assertGitProtocol("ssh://user@server/project.git", "ssh");
        assertGitProtocol("user@cheese:project.git", "ssh");
        assertGitProtocol("git@github.com:fabric8-quickstarts/spring-boot-webmvc.git", "ssh");
        assertGitProtocol("https://github.com/fabric8-quickstarts/spring-boot-webmvc.git", "https");
        assertGitProtocol("foo/bar", null);
    }

    public static void assertHostName(String gitUrl, String expectedHostName) {
        String actual = GitUtils.getGitHostName(gitUrl);
        assertThat(actual).describedAs("getGitHostName: " + gitUrl).isEqualTo(expectedHostName);
    }

    public static void assertGitProtocol(String gitUrl, String expected) {
        String actual = GitUtils.getGitProtocol(gitUrl);
        assertThat(actual).describedAs("getGitProtocol: " + gitUrl).isEqualTo(expected);
    }


}
