/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.fabric8.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class PasswordEncoderTest {

    @Test
    public void testEncode() {      
        assertEquals(PasswordEncoder.PREFIX + "UXdlcnR5", PasswordEncoder.encode("Qwerty"));
        assertEquals(PasswordEncoder.PREFIX + "Qwerty", PasswordEncoder.encode(PasswordEncoder.PREFIX + "Qwerty"));       
    }
    
    @Test
    public void testDecode() {      
        assertEquals("UXdlcnR5", PasswordEncoder.decode("UXdlcnR5"));
        assertEquals("Qwerty", PasswordEncoder.decode(PasswordEncoder.PREFIX + "UXdlcnR5"));       
    }
    
    @Test
    public void testDisablePwEncoder() {
        System.setProperty("zookeeper.password.encode", "false");
        assertEquals("UXdlcnR5", PasswordEncoder.decode("UXdlcnR5"));
        assertEquals(PasswordEncoder.PREFIX + "UXdlcnR5", PasswordEncoder.decode(PasswordEncoder.PREFIX + "UXdlcnR5"));       
        assertEquals("Qwerty", PasswordEncoder.encode("Qwerty"));
        assertEquals(PasswordEncoder.PREFIX + "Qwerty", PasswordEncoder.encode(PasswordEncoder.PREFIX + "Qwerty"));       
    }
}
