/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.quickstarts.eip;

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

import org.junit.Test;

import static io.fabric8.quickstarts.eip.RegionSupport.AMER;
import static io.fabric8.quickstarts.eip.RegionSupport.APAC;
import static io.fabric8.quickstarts.eip.RegionSupport.EMEA;
import static org.junit.Assert.assertEquals;

/**
 * Just a simple JUnit test class for {@link RegionSupport}
 */
public class RegionSupportTest {

    private final RegionSupport support = new RegionSupport();

    @Test
    public void testGetRegion() {
        assertEquals(APAC, support.getRegion("AU"));
        assertEquals(EMEA, support.getRegion("BE"));
        assertEquals(EMEA, support.getRegion("UK"));
        assertEquals(AMER, support.getRegion("US"));
    }

}
