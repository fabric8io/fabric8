package org.fusesource.fabric.maven.impl;

import junit.framework.Assert;
import org.junit.Test;

public class MavenProxyServletSupportTest {

    private MavenProxyServletSupport servlet = new MavenDownloadProxyServlet();

    @Test(expected = InvalidMavenArtifactRequest.class)
    public void testConvertNullPath() throws InvalidMavenArtifactRequest {
        servlet.convertToMavenUrl(null);
    }

    @Test(expected = InvalidMavenArtifactRequest.class)
         public void testConvertEmptyPath() throws InvalidMavenArtifactRequest {
        servlet.convertToMavenUrl("");
    }

    @Test(expected = InvalidMavenArtifactRequest.class)
    public void testConvertIncompletePath() throws InvalidMavenArtifactRequest {
        servlet.convertToMavenUrl("org/fusesource/fabric/LATEST");
    }

    @Test
    public void testConvertNormalPath() throws InvalidMavenArtifactRequest {
        Assert.assertEquals("org.fusesource.fabric:fuse-fabric:jar:LATEST",servlet.convertToMavenUrl("org/fusesource/fabric/fuse-fabric/LATEST/fuse-fabric-LATEST.jar"));
    }

}
