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
package io.fabric8.utils;

public class PasswordEncoder {  

    public static final String PREFIX = "ZKENC=";
    
    private PasswordEncoder() {
    }
    
    /**
     * Encodes a String into a base 64 String. The resulting encoding is chunked at 76 bytes.
     * <p/>
     *
     * @param s String to encode.
     * @return encoded string.
     */
    public static String encode(String s) {        
        return shouldEncodePassword(s) ? PREFIX + Base64Encoder.encode(s) : s;
    }

    private static boolean shouldEncodePassword(String s) {
        if (Boolean.parseBoolean(System.getProperty("zookeeper.password.encode", "true"))) {
            // don't want to encode password that is already encoded
            return !s.startsWith(PREFIX);
        } else {
            return false;
        }
    }    
    
    /**
     * Decodes a base 64 String into a String.
     * <p/>
     *
     * @param s String to decode.
     * @return encoded string.
     * @throws java.lang.IllegalArgumentException
     *          thrown if the given byte array was not valid com.sun.syndication.io.impl.Base64 encoding.
     */
    public static String decode(String s)
            throws IllegalArgumentException {
        return shouldDecodePassword(s) ? Base64Encoder.decode(s.substring(PREFIX.length())) : s;
    }
    
    private static boolean shouldDecodePassword(String s) {
        if (Boolean.parseBoolean(System.getProperty("zookeeper.password.encode", "true"))) {
            // don't want to decode password that is not encoded
            return s.startsWith(PREFIX);
        } else {
            return false;
        }
    }    
    
}
