package org.fusesource.fabric.utils;

import java.net.MalformedURLException;
import junit.framework.Assert;
import org.junit.Test;

public class PortUtilsTest {

    @Test
    public void testPortMapWithNoRange() {
        Assert.assertEquals(8080, PortUtils.mapPortToRange(8080, 0, 0));
    }

    @Test
    public void testPortMapWithLowerMinimum() {
        Assert.assertEquals(8080, PortUtils.mapPortToRange(8080, 8000, 0));
    }

    @Test
    public void testPortMapWithGreaterMaximum() {
        Assert.assertEquals(8080, PortUtils.mapPortToRange(8080, 0, 9000));
    }


    @Test
    public void testPortInRange() {
        Assert.assertEquals(8080, PortUtils.mapPortToRange(8080, 8000, 9000));
    }

    @Test
    public void testPortOutOfRange() {
        Assert.assertEquals(18080, PortUtils.mapPortToRange(8080, 10000, 19000));
    }

    @Test
    public void testPortOutOfNarrowRange() {
        Assert.assertEquals(13080, PortUtils.mapPortToRange(8080, 10000, 15000));
    }

    @Test
    public void testPortOutOfRangeWithNoUpperLimit() {
        Assert.assertEquals(18080, PortUtils.mapPortToRange(8080, 10000, 0));
    }

    @Test
    public void testExtractSshUrl()  {
        Assert.assertEquals(2181,PortUtils.extractPort("istation:2181"));
    }
}
