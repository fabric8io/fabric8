/*
 * Copyright 2005-2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.arquillian.utils;

import org.junit.Test;

import java.util.regex.Matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SecretsTest {

    @Test
    public void testRegexParsing() {
        String str = "testname";
        Matcher matcher = Secrets.FOLDER_PATTERN.matcher(str);
        assertTrue(matcher.matches());
        String folder = matcher.group(Secrets.FOLDER_GROUP);
        assertEquals(folder, "testname");

        str = "testfolder[one]";
        matcher = Secrets.FOLDER_PATTERN.matcher(str);
        assertTrue(matcher.matches());
        folder = matcher.group(Secrets.FOLDER_GROUP);
        assertEquals("testfolder", folder);

        String secrets = matcher.group(3);
        assertEquals("one", secrets);

        str = "testfolder[one,two]";
        matcher = Secrets.FOLDER_PATTERN.matcher(str);
        assertTrue(matcher.matches());
        folder = matcher.group(Secrets.FOLDER_GROUP);
        assertEquals("testfolder", folder);

        secrets = matcher.group(Secrets.CONTENT_GROUP);
        assertEquals("one,two", secrets);
    }
}