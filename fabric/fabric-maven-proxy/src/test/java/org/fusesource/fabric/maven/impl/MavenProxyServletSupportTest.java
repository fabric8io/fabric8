package org.fusesource.fabric.maven.impl;

import junit.framework.Assert;
import org.junit.Test;

public class MavenProxyServletSupportTest {

    private MavenProxyServletSupport servlet = new MavenDownloadProxyServlet();

    @Test(expected = InvalidMavenArtifactRequest.class)
    public void testConvertNullPath() throws InvalidMavenArtifactRequest {
        servlet.convertToMavenUrl(null);
    }

    @Test
    public void testConvertNormalPath() throws InvalidMavenArtifactRequest {
        Assert.assertEquals("org.fusesource.fabric:fuse-fabric:jar:LATEST",servlet.convertToMavenUrl("org/fusesource/fabric/fuse-fabric/LATEST/fuse-fabric-LATEST.jar"));
    }

    @Test
    public void testConvertWithPlainGroupId() throws InvalidMavenArtifactRequest {
        Assert.assertEquals("fusesource:fuse-fabric:jar:LATEST",servlet.convertToMavenUrl("fusesource/fuse-fabric/LATEST/fuse-fabric-LATEST.jar"));
    }

}
