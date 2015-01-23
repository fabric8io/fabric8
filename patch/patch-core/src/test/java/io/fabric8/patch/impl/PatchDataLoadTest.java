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
package io.fabric8.patch.impl;

import io.fabric8.patch.Service;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for {@link io.fabric8.patch.impl.PatchData}
 */
public class PatchDataLoadTest {

    private Service service;

    @Before
    public void createMockService() {
        service = createMock(Service.class);
        replay(service);
    }

    @Test
    public void testLoadWithFileOverrides() throws IOException {
        PatchData patch = PatchData.load(getClass().getClassLoader().getResourceAsStream("files/patch1.patch"));
        assertEquals(1, patch.getBundles().size());
        assertEquals(1, patch.getFiles().size());
        assertTrue(patch.getFiles().contains("bin/karaf"));
    }

}
