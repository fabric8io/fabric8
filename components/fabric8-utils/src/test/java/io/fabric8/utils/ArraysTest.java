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

public class ArraysTest {

    @Test
    public void joinTest() {
        
        Assert.assertEquals("A,B,C,D,E", Arrays.join(",", "A", "B", "C", "D", "E"));
        Assert.assertEquals("1:2:3:4:5", Arrays.join(":", 1, 2, 3, 4, 5));
        Assert.assertEquals("1.1;2;C;4;5.5", Arrays.join(";", 1.1d, 2, "C", 4, 5.5f));

    }
    
}
