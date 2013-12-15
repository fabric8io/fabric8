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

import org.osgi.framework.BundleContext;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DataStoreUtils {


    public static byte[] toBytes(Properties source) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        source.store(baos, null);
        return baos.toByteArray();
    }

    public static byte[] toBytes(Map<String, String> source) throws IOException {
        return toBytes(toProperties(source));
    }

    public static Properties toProperties(byte[] source) throws IOException {
        Properties rc = new Properties();
        if (source != null) {
            rc.load(new ByteArrayInputStream(source));
        }
        return rc;
    }

    public static Map<String, String> toMap(Properties source) {
        Map<String, String> rc = new HashMap<String, String>();
        for (Map.Entry<Object, Object> entry : source.entrySet()) {
            rc.put((String) entry.getKey(), (String) entry.getValue());
        }
        return rc;
    }

    public static Map<String, String> toMap(byte[] source) throws IOException {
        return toMap(toProperties(source));
    }

    public static Properties toProperties(Map<String, String> source) {
        Properties rc = new Properties();
        for (Map.Entry<String, String> entry : source.entrySet()) {
            rc.put(entry.getKey(), entry.getValue());
        }
        return rc;
    }

    public static Properties toStringProperties(Map<String, ?> source) {
        Properties rc = new Properties();
        for (Map.Entry<String, ?> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (value != null) {
                rc.put(entry.getKey(), value.toString());
            }
        }
        return rc;
    }

    public static String stripSuffix(String value, String suffix) throws IOException {
        if (value.endsWith(suffix)) {
            return value.substring(0, value.length() - suffix.length());
        } else {
            return value;
        }
    }

    public static String toString(Properties source) throws IOException {
        StringWriter writer = new StringWriter();
        source.store(writer, null);
        return writer.toString();
    }

    public static Properties toProperties(String source) throws IOException {
        Properties rc = new Properties();
        rc.load(new StringReader(source));
        return rc;
    }

    /**
     * Substitutes a placeholder with the checksum:[url] format with the checksum of the urls target.
     * @param key
     * @return  The checksum or 0.
     */
    public static String substituteChecksum(String key) {
        InputStream is = null;
        try {
            URL url = new URL(key.substring("checksum:".length()));
            is = url.openStream();
            return String.valueOf(ChecksumUtils.checksum(is));
        } catch (Exception ex) {
            return "0";
        } finally {
            Closeables.closeQuitely(is);
        }
    }

    /**
     * Substitutes a placeholder with profile:[property file]/[key], with the target value.
     * @param key
     * @param configs
     * @return  The target value or the key as is.
     */
    public static String substituteProfileProperty(String key, Map<String, Map<String, String>> configs) {
        String pid = key.substring("profile:".length(), key.indexOf("/"));
        String propertyKey = key.substring(key.indexOf("/") + 1);
        Map<String, String> targetProps = configs.get(pid);
        if (targetProps != null && targetProps.containsKey(propertyKey)) {
            return targetProps.get(propertyKey);
        } else {
            return key;
        }
    }

    /**
     * Substitutes bundle property.
     * @param key
     * @return  The target value or an empty String.
     */
    public static String substituteBundleProperty(String key, BundleContext bundleContext) {
        String value = null;
        if (bundleContext != null) {
            value = bundleContext.getProperty(key);
        }
        if (value == null) {
            value = System.getProperty(key);
        }
        return value != null ? value : "";
    }

}
