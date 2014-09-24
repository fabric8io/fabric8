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
package io.fabric8.agent.mvn;

import java.net.MalformedURLException;

import io.fabric8.maven.util.MavenRepositoryURL;
import org.junit.Assert;
import org.junit.Test;

public class MavenRepositoryURLTest {

    @Test
    public void testSimpleSpec() throws MalformedURLException {
        String spec = "http://repo1.maven.org/maven2";
        MavenRepositoryURL repo = new MavenRepositoryURL(spec);
        Assert.assertTrue(repo.isReleasesEnabled());
        Assert.assertFalse(repo.isSnapshotsEnabled());
        Assert.assertEquals("repo_" + String.valueOf("http://repo1.maven.org/maven2/".hashCode()), repo.getId() );
    }

    @Test
    public void testSimpleSpecWithSnapshots() throws MalformedURLException {
        String spec = "http://repo1.maven.org/maven2@snapshots";
        MavenRepositoryURL repo = new MavenRepositoryURL(spec);
        Assert.assertTrue(repo.isReleasesEnabled());
        Assert.assertTrue(repo.isSnapshotsEnabled());
        Assert.assertEquals("repo_" + String.valueOf("http://repo1.maven.org/maven2/".hashCode()), repo.getId() );
    }

    @Test
    public void testSimpleSpecWithNoReleases() throws MalformedURLException {
        String spec = "http://repo1.maven.org/maven2@noreleases";
        MavenRepositoryURL repo = new MavenRepositoryURL(spec);
        Assert.assertFalse(repo.isReleasesEnabled());
        Assert.assertFalse(repo.isSnapshotsEnabled());
        Assert.assertEquals("repo_" + String.valueOf("http://repo1.maven.org/maven2/".hashCode()), repo.getId() );
    }

    @Test
    public void testSimpleSpecWithId() throws MalformedURLException {
        String spec = "http://repo1.maven.org/maven2@id=central";
        MavenRepositoryURL repo = new MavenRepositoryURL(spec);
        Assert.assertTrue(repo.isReleasesEnabled());
        Assert.assertFalse(repo.isSnapshotsEnabled());
        Assert.assertEquals("central", repo.getId() );
    }


    @Test
    public void testSpecWithSnapshotsAndId() throws MalformedURLException {
        String spec = "http://repo1.maven.org/maven2@snapshots@id=central";
        MavenRepositoryURL repo = new MavenRepositoryURL(spec);
        Assert.assertTrue(repo.isReleasesEnabled());
        Assert.assertTrue(repo.isSnapshotsEnabled());
        Assert.assertEquals("central", repo.getId() );

        spec = "http://repo1.maven.org/maven2@id=central@snapshots";
        repo = new MavenRepositoryURL(spec);
        Assert.assertTrue(repo.isReleasesEnabled());
        Assert.assertTrue(repo.isSnapshotsEnabled());
        Assert.assertEquals("central", repo.getId() );
    }


}
