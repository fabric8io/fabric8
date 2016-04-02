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

public class Base64EncoderTest {

    @Test
    public void encodeTest() {
        
        //RFC4648 test vectors
        Assert.assertEquals("", Base64Encoder.encode(""));
        Assert.assertEquals("Zg==", Base64Encoder.encode("f"));
        Assert.assertEquals("Zm8=", Base64Encoder.encode("fo"));
        Assert.assertEquals("Zm9v", Base64Encoder.encode("foo"));
        Assert.assertEquals("Zm9vYg==", Base64Encoder.encode("foob"));
        Assert.assertEquals("Zm9vYmE=", Base64Encoder.encode("fooba"));
        Assert.assertEquals("Zm9vYmFy", Base64Encoder.encode("foobar"));
        
    }

    @Test
    public void decodeTest() {
        
        //RFC4648 test vectors
        Assert.assertEquals("", Base64Encoder.decode(""));
        Assert.assertEquals("f", Base64Encoder.decode("Zg=="));
        Assert.assertEquals("fo", Base64Encoder.decode("Zm8="));
        Assert.assertEquals("foo", Base64Encoder.decode("Zm9v"));
        Assert.assertEquals("foob", Base64Encoder.decode("Zm9vYg=="));
        Assert.assertEquals("fooba", Base64Encoder.decode("Zm9vYmE="));
        Assert.assertEquals("foobar", Base64Encoder.decode("Zm9vYmFy"));
        
    }

}
