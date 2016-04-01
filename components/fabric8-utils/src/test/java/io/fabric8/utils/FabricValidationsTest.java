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

import org.junit.Test;

import static io.fabric8.utils.FabricValidations.isValidContainerName;
import static io.fabric8.utils.FabricValidations.isValidProfileName;
import static io.fabric8.utils.FabricValidations.validateProfileName;
import static org.junit.Assert.assertTrue;
import static io.fabric8.utils.FabricValidations.validateContainerName;

public class FabricValidationsTest {

    @Test(expected = IllegalArgumentException.class)
    public void testContainerWithInvalidPrefix() {
        validateContainerName("--container");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testContainerWithInvalidPrefix2() {
        validateContainerName("_container");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testContainerWithUpperCase() {
        validateContainerName("MyContainer");
    }

    @Test
    public void testValidContainerNames() {
        assertTrue(isValidContainerName("c"));
        assertTrue(isValidContainerName("c1"));
        assertTrue(isValidContainerName("c-1"));
        assertTrue(isValidContainerName("c_1"));
        assertTrue(isValidContainerName("1container"));
        assertTrue(isValidContainerName("container1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProfileWithInvalidPrefix() {
        validateProfileName("--profile");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProfileWithInvalidPrefix2() {
        validateProfileName("_profile");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProfileWithUpperCase() {
        validateProfileName("MyProfile");
    }

    @Test
    public void testValidProfileNames() {
        assertTrue(isValidProfileName("c"));
        assertTrue(isValidProfileName("c1"));
        assertTrue(isValidProfileName("c-1"));
        assertTrue(isValidProfileName("c_1"));
        assertTrue(isValidProfileName("1container"));
        assertTrue(isValidProfileName("container1"));

        assertTrue(isValidProfileName("c"));
        assertTrue(isValidProfileName("c1"));
        assertTrue(isValidProfileName("c-1"));
        assertTrue(isValidProfileName("c_1"));
        assertTrue(isValidProfileName("1container"));
        assertTrue(isValidProfileName("container1"));

        // we also allow dots
        assertTrue(isValidProfileName("my.container.name"));
        assertTrue(isValidProfileName("my.container123.name"));
    }
}
