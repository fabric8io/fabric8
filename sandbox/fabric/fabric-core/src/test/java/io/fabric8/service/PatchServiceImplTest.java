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
package io.fabric8.service;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class PatchServiceImplTest {

    @Test
    public void testPatchDescriptorWithoutDirectives() throws IOException {
        PatchServiceImpl.PatchDescriptor descriptor = getPatchDescriptor("test1.patch");
        assertEquals(2, descriptor.getBundles().size());
        assertTrue(descriptor.getBundles().contains("mvn:io.fabric8.test/test1/1.2.0"));
        assertTrue(descriptor.getBundles().contains("mvn:io.fabric8.test/test2/1.2.0"));
        assertTrue(descriptor.getRequirements().isEmpty());
    }

    @Test
    public void testPatchDescriptorWithDirectives() throws IOException {
        PatchServiceImpl.PatchDescriptor descriptor = getPatchDescriptor("test2.patch");
        assertEquals(2, descriptor.getBundles().size());
        assertTrue(descriptor.getBundles().contains("mvn:io.fabric8.test/test1/1.2.0;range=[1.0.0,2.0.0)"));
        assertTrue(descriptor.getBundles().contains("mvn:io.fabric8.test/test2/1.2.0"));
        assertTrue(descriptor.getRequirements().isEmpty());
    }

    @Test
    public void testPatchDescriptorWithRequirements() throws IOException {
        PatchServiceImpl.PatchDescriptor descriptor = getPatchDescriptor("test3.patch");
        assertEquals(2, descriptor.getBundles().size());
        assertTrue(descriptor.getBundles().contains("mvn:io.fabric8.test/test1/1.2.0"));
        assertTrue(descriptor.getBundles().contains("mvn:io.fabric8.test/test2/1.2.0"));
        assertEquals(1, descriptor.getRequirements().size());
        assertTrue(descriptor.getRequirements().contains("prereq3"));
    }

    @Test
    public void testCheckRequirementsSatisfied() throws IOException {
        PatchServiceImpl.PatchDescriptor descriptor = getPatchDescriptor("test3.patch");
        Version version =
                buildVersion("1.1").withProfiles("karaf", "default", "patch-prereq3", "patch-somethingelse").done();

        // this should not throw a RuntimeException
        PatchServiceImpl.checkRequirements(version, descriptor);
    }

    @Test
    public void testCheckRequirementsMissingPatches() throws IOException {
        PatchServiceImpl.PatchDescriptor descriptor = getPatchDescriptor("test4.patch");
        Version version =
                buildVersion("1.1").withProfiles("karaf", "default", "patch-prereq4a", "patch-somethingelse").done();

        try {
            PatchServiceImpl.checkRequirements(version, descriptor);
            fail("Patch should not have passed requirements check - required patch is missing");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().toLowerCase().contains("required patch 'prereq4b' is missing"));
        }
    }

    @Test
    public void testCheckRequirementsMultiplePatches() throws IOException {
        Collection<PatchServiceImpl.PatchDescriptor> patches = new LinkedList<PatchServiceImpl.PatchDescriptor>();
        patches.add(getPatchDescriptor("test3.patch"));
        patches.add(getPatchDescriptor("test4.patch"));

        Version version =
                buildVersion("1.1").withProfiles("karaf", "default", "patch-prereq4a", "patch-prereq3").done();

        try {
            PatchServiceImpl.checkRequirements(version, patches);
            fail("Patch should not have passed requirements check - required patch is missing");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().toLowerCase().contains("required patch 'prereq4b' is missing"));
        }
    }

    @Test
    public void testGetPatchProfile() throws IOException {
        PatchServiceImpl.PatchDescriptor test1 = getPatchDescriptor("test1.patch");
        PatchServiceImpl.PatchDescriptor test3 = getPatchDescriptor("test3.patch");
        Version version =
                buildVersion("1.1").withProfiles("karaf", "default", "patch-test3").done();

        Profile profile = PatchServiceImpl.getPatchProfile(version, test3);
        assertNotNull("test3 patch profile should be found", profile);
        assertEquals("patch-test3", profile.getId());

        assertNull("test1 patch profile should not be found", PatchServiceImpl.getPatchProfile(version, test1));
    }

    /*
     * Load patch descriptor from a classpath resource
     */
    private PatchServiceImpl.PatchDescriptor getPatchDescriptor(String resource) throws IOException {
        Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream(resource));
        return new PatchServiceImpl.PatchDescriptor(properties);
    }

    /*
     * Build a new mock Version instances
     */
    private VersionBuilder buildVersion(String version) {
        return new VersionBuilder(version);
    }

    /*
     * Small builder helper to create mock Version instances
     */
    private static final class VersionBuilder {

        final Version mock = createNiceMock(Version.class);
        final List<Profile> profiles = new LinkedList<Profile>();

        private VersionBuilder(String version) {
            super();
            expect(mock.getId()).andReturn(version).anyTimes();
            expect(mock.getProfiles()).andReturn(profiles).anyTimes();
        }

        private VersionBuilder withProfiles(String... names) {
            for (String name : names) {
                Profile profile = createNiceMock(Profile.class);
                expect(profile.getId()).andReturn(name).anyTimes();
                replay(profile);

                expect(mock.getProfile(name)).andReturn(profile).anyTimes();
                profiles.add(profile);
            }
            return this;
        }

        private Version done() {
            replay(mock);
            return mock;
        }

    }
}
