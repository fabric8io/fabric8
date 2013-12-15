package io.fabric8.internal;

import junit.framework.Assert;
import org.junit.Test;

import java.util.Set;

public class PlaceholderResolverHelpersTest {

    @Test
    public void testExtractSchemes() {
        //Simple key.
        String key = "${zk:container/ip}";
        Set<String> schemes = PlaceholderResolverHelpers.getSchemeForValue(key);
        Assert.assertEquals(schemes.size(), 1);
        Assert.assertTrue(schemes.contains("zk"));

        //Nested key
        key = "${zk:container/${zk:container/resolver}}";
        schemes = PlaceholderResolverHelpers.getSchemeForValue(key);
        Assert.assertEquals(schemes.size(), 1);
        Assert.assertTrue(schemes.contains("zk"));

        //Nested key with multiple schemes
        key = "${profile:${zk:container/foo}";
        schemes = PlaceholderResolverHelpers.getSchemeForValue(key);
        Assert.assertEquals(schemes.size(), 2);
        Assert.assertTrue(schemes.contains("zk"));
        Assert.assertTrue(schemes.contains("profile"));

        key = "file:${karaf.home}/${karaf.default.repository}@snapshots@id=karaf-default,file:${karaf.home}/local-repo@snapshots@id=karaf-local";
        schemes = PlaceholderResolverHelpers.getSchemeForValue(key);
        Assert.assertEquals(schemes.size(), 0);
    }
}
