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
package io.fabric8.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.felix.utils.properties.Properties;

public class ProfileUtils {

    public static byte[] toBytes(Properties source) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            source.store(baos, null);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Cannot store properties", ex);
        }
        return baos.toByteArray();
    }

    public static byte[] toBytes(Map<String, String> source) {
        return toBytes(toProperties(source));
    }

    public static Properties toProperties(byte[] source)  {
        try {
            Properties rc = new Properties(false);
            if (source != null) {
                rc.load(new ByteArrayInputStream(source));
            }
            return rc;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Cannot load properties", ex);
        }
    }

    public static Properties toProperties(Map<String, String> source) {
        try {
            if (source instanceof Properties) {
                return (Properties) source;
            }
            Properties rc = new Properties(false);
            rc.putAll(source);
            return rc;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Cannot load properties", ex);
        }
    }

}
