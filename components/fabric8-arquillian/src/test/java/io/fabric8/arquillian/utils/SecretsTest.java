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
package io.fabric8.arquillian.utils;

import org.junit.Test;

import java.util.List;
import java.util.regex.Matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SecretsTest {

    @Test
    public void testGetNames() {
        List<String> result = Secrets.getNames("one,two");
        assertTrue(result.contains("one"));
        assertTrue(result.contains("two"));

        result = Secrets.getNames("one, two");
        assertTrue(result.contains("one"));
        assertTrue(result.contains("two"));

        result = Secrets.getNames("one[in1,in2]");
        assertTrue(result.contains("one"));

        List<String> contents = Secrets.getContents("one[in1,in2]", "one");
        assertTrue(contents.contains("in1"));
        assertTrue(contents.contains("in2"));

        contents = Secrets.getContents("one[in11,in12], two[in21,in22]", "one");
        assertTrue(contents.contains("in11"));
        assertTrue(contents.contains("in12"));

        contents = Secrets.getContents("one[in11,in12], two[in21,in22]", "two");
        assertTrue(contents.contains("in21"));
        assertTrue(contents.contains("in22"));
    }

}