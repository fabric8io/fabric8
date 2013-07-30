package org.fusesource.fabric.maven.impl;

import java.util.regex.Matcher;
import junit.framework.Assert;
import org.junit.Test;

public class MavenProxyServletSupportTest {

    private MavenProxyServletSupport servlet = new MavenDownloadProxyServlet(null, null, false, null,null,null,null,0,null, null, null);

    @Test
    public void testMetadataRegex() {
        Matcher m = MavenProxyServletSupport.ARTIFACT_METADATA_URL_REGEX.matcher("groupId/artifactId/version/maven-metadata.xml");
        Assert.assertTrue(m.matches());
        Assert.assertEquals("maven-metadata.xml", m.group(4));

        m = MavenProxyServletSupport.ARTIFACT_METADATA_URL_REGEX.matcher("groupId/artifactId/version/maven-metadata-local.xml");
        Assert.assertTrue(m.matches());
        Assert.assertEquals("maven-metadata-local.xml", m.group(4));
        Assert.assertEquals("local", m.group(7));

        m = MavenProxyServletSupport.ARTIFACT_METADATA_URL_REGEX.matcher("groupId/artifactId/version/maven-metadata-rep-1234.xml");
        Assert.assertTrue(m.matches());
        Assert.assertEquals("maven-metadata-rep-1234.xml", m.group(4));
        Assert.assertEquals("rep-1234", m.group(7));

        m = MavenProxyServletSupport.ARTIFACT_METADATA_URL_REGEX.matcher("groupId/artifactId/version/maven-metadata.xml.md5");
        Assert.assertTrue(m.matches());
        Assert.assertEquals("maven-metadata.xml", m.group(4));
    }

    @Test
    public void testRepoRegex() {
        Matcher m = MavenProxyServletSupport.REPOSITORY_ID_REGEX.matcher("repo1.maven.org/maven2@id=central");
        Assert.assertTrue(m.matches());
        Assert.assertEquals("central", m.group(2));

        m = MavenProxyServletSupport.REPOSITORY_ID_REGEX.matcher("http://repo.fusesource.com/nexus/content/repositories/releases@id=fusereleases");
        Assert.assertTrue(m.matches());
        Assert.assertEquals("fusereleases", m.group(2));

        m = MavenProxyServletSupport.REPOSITORY_ID_REGEX.matcher("repo1.maven.org/maven2@snapshots@id=central");
        Assert.assertTrue(m.matches());
        Assert.assertEquals("central", m.group(2));

        m = MavenProxyServletSupport.REPOSITORY_ID_REGEX.matcher("repo1.maven.org/maven2@id=central@snapshots");
        Assert.assertTrue(m.matches());
        Assert.assertEquals("central", m.group(2));

        m = MavenProxyServletSupport.REPOSITORY_ID_REGEX.matcher("repo1.maven.org/maven2@noreleases@id=central@snapshots");
        Assert.assertTrue(m.matches());
        Assert.assertEquals("central", m.group(2));
    }

    @Test(expected = InvalidMavenArtifactRequest.class)
    public void testConvertNullPath() throws InvalidMavenArtifactRequest {
        servlet.convertToMavenUrl(null);
    }

    @Test
    public void testConvertNormalPath() throws InvalidMavenArtifactRequest {
        Assert.assertEquals("groupId:artifactId:extension:version",servlet.convertToMavenUrl("groupId/artifactId/version/artifactId-version.extension"));
        Assert.assertEquals("group.id:artifactId:extension:version",servlet.convertToMavenUrl("group/id/artifactId/version/artifactId-version.extension"));
        Assert.assertEquals("group.id:artifact.id:extension:version",servlet.convertToMavenUrl("group/id/artifact.id/version/artifact.id-version.extension"));

        Assert.assertEquals("group-id:artifactId:extension:version",servlet.convertToMavenUrl("group-id/artifactId/version/artifactId-version.extension"));
        Assert.assertEquals("group-id:artifact-id:extension:version",servlet.convertToMavenUrl("group-id/artifact-id/version/artifact-id-version.extension"));
        Assert.assertEquals("group-id:my-artifact-id:extension:version",servlet.convertToMavenUrl("group-id/my-artifact-id/version/my-artifact-id-version.extension"));

        //Some real cases
        Assert.assertEquals("org.apache.camel.karaf:apache-camel:jar:LATEST",servlet.convertToMavenUrl("org/apache/camel/karaf/apache-camel/LATEST/apache-camel-LATEST.jar"));
        Assert.assertEquals("org.apache.cxf.karaf:apache-cxf:jar:LATEST",servlet.convertToMavenUrl("org/apache/cxf/karaf/apache-cxf/LATEST/apache-cxf-LATEST.jar"));
        Assert.assertEquals("org.fusesource.fabric:fuse-fabric:jar:LATEST",servlet.convertToMavenUrl("org/fusesource/fabric/fuse-fabric/LATEST/fuse-fabric-LATEST.jar"));

        //Try extensions with a dot
        Assert.assertEquals("org.fusesource.fabric:fuse-fabric:zip:LATEST",servlet.convertToMavenUrl("org/fusesource/fabric/fuse-fabric/LATEST/fuse-fabric-LATEST.zip"));
    }

    @Test
    public void testConvertNormalPathWithClassifier() throws InvalidMavenArtifactRequest {
        Assert.assertEquals("groupId:artifactId:extension:classifier:version",servlet.convertToMavenUrl("groupId/artifactId/version/artifactId-version-classifier.extension"));
        Assert.assertEquals("group.id:artifactId:extension:classifier:version",servlet.convertToMavenUrl("group/id/artifactId/version/artifactId-version-classifier.extension"));
        Assert.assertEquals("group.id:artifact.id:extension:classifier:version",servlet.convertToMavenUrl("group/id/artifact.id/version/artifact.id-version-classifier.extension"));

        Assert.assertEquals("group.id:artifact.id:extension.sha1:classifier:version",servlet.convertToMavenUrl("group/id/artifact.id/version/artifact.id-version-classifier.extension.sha1"));
        Assert.assertEquals("group.id:artifact.id:extension.md5:classifier:version",servlet.convertToMavenUrl("group/id/artifact.id/version/artifact.id-version-classifier.extension.md5"));

        Assert.assertEquals("group-id:artifactId:extension:classifier:version",servlet.convertToMavenUrl("group-id/artifactId/version/artifactId-version-classifier.extension"));
        Assert.assertEquals("group-id:artifact-id:extension:classifier:version",servlet.convertToMavenUrl("group-id/artifact-id/version/artifact-id-version-classifier.extension"));
        Assert.assertEquals("group-id:my-artifact-id:extension:classifier:version",servlet.convertToMavenUrl("group-id/my-artifact-id/version/my-artifact-id-version-classifier.extension"));

        //Some real cases
        Assert.assertEquals("org.apache.camel.karaf:apache-camel:xml:features:LATEST",servlet.convertToMavenUrl("org/apache/camel/karaf/apache-camel/LATEST/apache-camel-LATEST-features.xml"));
        Assert.assertEquals("org.apache.cxf.karaf:apache-cxf:xml:features:LATEST",servlet.convertToMavenUrl("org/apache/cxf/karaf/apache-cxf/LATEST/apache-cxf-LATEST-features.xml"));
        Assert.assertEquals("org.fusesource.fabric:fuse-fabric:xml:features:LATEST",servlet.convertToMavenUrl("org/fusesource/fabric/fuse-fabric/LATEST/fuse-fabric-LATEST-features.xml"));
        Assert.assertEquals("org.fusesource.fabric:fuse-fabric:xml:features:7-1-x-fuse-01",servlet.convertToMavenUrl("org/fusesource/fabric/fuse-fabric/7-1-x-fuse-01/fuse-fabric-7-1-x-fuse-01-features.xml"));

        //Try extensions with a dot
        Assert.assertEquals("org.fusesource.fabric:fuse-fabric:zip:distro:LATEST",servlet.convertToMavenUrl("org/fusesource/fabric/fuse-fabric/LATEST/fuse-fabric-LATEST-distro.zip"));
    }

}
