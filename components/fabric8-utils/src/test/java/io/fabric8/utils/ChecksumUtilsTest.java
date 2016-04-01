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

import org.junit.Assert;
import org.junit.Test;

public class ChecksumUtilsTest {

    @Test
    public void checksumTest() throws IOException {
        
        //well known CRC32 checksums
        InputStream stream = new ByteArrayInputStream("The quick brown fox jumps over the lazy dog".getBytes("UTF-8"));
        
        Assert.assertEquals(0x414fa339, ChecksumUtils.checksum(stream));

        stream = new ByteArrayInputStream("".getBytes("UTF-8"));
        
        Assert.assertEquals(0x0, ChecksumUtils.checksum(stream));
        
        try {
            ChecksumUtils.checksum(null);
            Assert.fail("Expected NullPointerException but nothing happened");
        } catch(NullPointerException e) {
            
        }

    }
    
}
