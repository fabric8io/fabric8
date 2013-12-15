package io.fabric8.agent.mvn;

import java.net.MalformedURLException;
import org.junit.Assert;
import org.junit.Test;

public class MavenRepositoryURLTest {

    @Test
    public void testSimpleSpec() throws MalformedURLException {
        String spec = "http://repo1.maven.org/maven2";
        MavenRepositoryURL repo = new MavenRepositoryURL(spec);
        Assert.assertTrue(repo.isReleasesEnabled());
        Assert.assertFalse(repo.isSnapshotsEnabled());
        Assert.assertEquals(String.valueOf("http://repo1.maven.org/maven2/".hashCode()), repo.getId() );
    }

    @Test
    public void testSimpleSpecWithSnapshots() throws MalformedURLException {
        String spec = "http://repo1.maven.org/maven2@snapshots";
        MavenRepositoryURL repo = new MavenRepositoryURL(spec);
        Assert.assertTrue(repo.isReleasesEnabled());
        Assert.assertTrue(repo.isSnapshotsEnabled());
        Assert.assertEquals(String.valueOf("http://repo1.maven.org/maven2/".hashCode()), repo.getId() );
    }

    @Test
    public void testSimpleSpecWithNoReleases() throws MalformedURLException {
        String spec = "http://repo1.maven.org/maven2@noreleases";
        MavenRepositoryURL repo = new MavenRepositoryURL(spec);
        Assert.assertFalse(repo.isReleasesEnabled());
        Assert.assertFalse(repo.isSnapshotsEnabled());
        Assert.assertEquals(String.valueOf("http://repo1.maven.org/maven2/".hashCode()), repo.getId() );
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
