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
package io.fabric8.utils;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static io.fabric8.utils.FabricValidations.isValidName;
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

    @Test
    public void testValidNames() {
        assertTrue(isValidName("c"));
        assertTrue(isValidName("c1"));
        assertTrue(isValidName("c-1"));
        assertTrue(isValidName("c_1"));
        assertTrue(isValidName("1container"));
        assertTrue(isValidName("Container1"));
    }
}
