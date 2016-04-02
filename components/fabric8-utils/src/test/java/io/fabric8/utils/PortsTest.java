/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.utils;

import org.junit.Assert;
import org.junit.Test;

public class PortsTest {

    @Test
    public void testPortMapWithNoRange() {
        Assert.assertEquals(8080, Ports.mapPortToRange(8080, 0, 0));
    }

    @Test
    public void testPortMapWithLowerMinimum() {
        Assert.assertEquals(8080, Ports.mapPortToRange(8080, 8000, 0));
    }

    @Test
    public void testPortMapWithGreaterMaximum() {
        Assert.assertEquals(8080, Ports.mapPortToRange(8080, 0, 9000));
    }


    @Test
    public void testPortInRange() {
        Assert.assertEquals(8080, Ports.mapPortToRange(8080, 8000, 9000));
    }

    @Test
    public void testPortOutOfRange() {
        Assert.assertEquals(18080, Ports.mapPortToRange(8080, 10000, 19000));
    }

    @Test
    public void testPortOutOfNarrowRange() {
        Assert.assertEquals(13080, Ports.mapPortToRange(8080, 10000, 15000));
    }

    @Test
    public void testPortOutOfRangeWithNoUpperLimit() {
        Assert.assertEquals(18080, Ports.mapPortToRange(8080, 10000, 0));
    }

    @Test
    public void testExtractSshUrl()  {
        Assert.assertEquals(2181, Ports.extractPort("istation:2181"));
    }
}
