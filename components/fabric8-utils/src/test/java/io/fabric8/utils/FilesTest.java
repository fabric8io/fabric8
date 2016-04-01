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



import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.junit.Assert;
import org.junit.Test;

public class FilesTest {

    @Test
    public void normalizePathTest() {

        Assert.assertEquals("/some/path", Files.normalizePath("\\some\\path", '\\', '/'));
        Assert.assertEquals("/some/path", Files.normalizePath("/some/path", '\\', '/'));
        Assert.assertEquals("\\some\\path", Files.normalizePath("/some/path", '/', '\\'));
        Assert.assertEquals("\\some\\path", Files.normalizePath("\\some\\path", '/', '\\'));
        
    }
    
    @Test
    public void toStringTest() throws IOException {
        
        final String testString = "This is a test string";
        
        final InputStream stream = new ByteArrayInputStream(testString.getBytes("UTF-8"));
        
        Assert.assertEquals(testString, Files.toString(stream));
        stream.reset();
        Assert.assertEquals(testString, Files.toString(stream, Charset.forName("UTF-8")));
        
    }
    
}
