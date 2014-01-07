/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.service;

import java.io.IOException;
import java.util.Properties;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class PatchServiceImplTest {

    @Test
    public void testPatchDescriptorWithoutDirectives() throws IOException {
        Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream("test1.patch"));
        PatchServiceImpl.PatchDescriptor descriptor = new PatchServiceImpl.PatchDescriptor(properties);
        assertEquals(2, descriptor.getBundles().size());
        assertTrue(descriptor.getBundles().contains("mvn:org.fusesource.test/test1/1.2.0"));
        assertTrue(descriptor.getBundles().contains("mvn:org.fusesource.test/test2/1.2.0"));
    }

    @Test
    public void testPatchDescriptorWithDirectives() throws IOException {
        Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream("test2.patch"));
        PatchServiceImpl.PatchDescriptor descriptor = new PatchServiceImpl.PatchDescriptor(properties);
        assertEquals(2, descriptor.getBundles().size());
        assertTrue(descriptor.getBundles().contains("mvn:org.fusesource.test/test1/1.2.0;range=[1.0.0,2.0.0)"));
        assertTrue(descriptor.getBundles().contains("mvn:org.fusesource.test/test2/1.2.0"));
    }
}
