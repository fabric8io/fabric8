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
package io.fabric8.kubernetes.api;

import io.fabric8.kubernetes.api.model.EditablePod;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import org.junit.Test;

import static org.junit.Assert.*;

public class UserConfigurationCompareTest {

    @Test
    public void testCommonDenominator() {
        assertEquals(Pod.class, UserConfigurationCompare.getCommonDenominator(Pod.class, EditablePod.class));
        assertEquals(EditablePod.class, UserConfigurationCompare.getCommonDenominator(EditablePod.class, EditablePod.class));
        assertEquals(Pod.class, UserConfigurationCompare.getCommonDenominator(EditablePod.class, Pod.class));

        assertEquals(null, UserConfigurationCompare.getCommonDenominator(ReplicationController.class, Pod.class));
    }
}