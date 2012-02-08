/**
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

package org.fusesource.eca.util;

import java.util.Map;

import junit.framework.TestCase;
import org.fusesource.eca.TestStat;

public class PropertyUtilTest extends TestCase {

    public void testGetValues() throws Exception {
        TestStat testStat = new TestStat();

        Map<String, Number> map = PropertyUtil.getValues(Number.class, testStat);
        assertEquals(3, map.size());
    }
}
