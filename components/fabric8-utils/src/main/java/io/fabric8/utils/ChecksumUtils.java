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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.CRC32;

public class ChecksumUtils {
    public static final String FOLDER_CHECKSUM_CACHE = ".fabric8.checksums.properties";

    private static final transient Logger LOG = LoggerFactory.getLogger(ChecksumUtils.class);

    private ChecksumUtils() {
    }

    /**
     * Compute a checksum for the file or directory that consists of the name, length and the last modified date
     * for a file and its children in case of a directory
     *
     * @param is the input stream
     * @return a checksum identifying any change
     */
    public static long checksum(InputStream is) throws IOException {
        try {
            CRC32 crc = new CRC32();
            byte[] buffer = new byte[8192];
            int l;
            while ((l = is.read(buffer)) > 0) {
                crc.update(buffer, 0, l);
            }
            return crc.getValue();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }


    public static long checksumFile(File file) throws IOException {
        return checksum(new FileInputStream(file));
    }

    /**
     * If there is a file called {@link #FOLDER_CHECKSUM_CACHE} load it where the key is the file name and the value is the checksum
     */
    public static Map<File, Long> loadInstalledChecksumCache(File dir) {
        Map<File, Long> answer = new HashMap<File, Long>();
        File cacheFile = new File(dir, FOLDER_CHECKSUM_CACHE);
        if (cacheFile.exists() && cacheFile.isFile()) {
            Properties properties = new Properties();
            try {
                properties.load(new FileReader(cacheFile));
            } catch (IOException e) {
                LOG.warn("Failed to load checksum cache file " + cacheFile + ". " + e, e);
            }
            Set<Map.Entry<Object, Object>> entries = properties.entrySet();
            for (Map.Entry<Object, Object> entry : entries) {
                Object key = entry.getKey();
                if (key != null) {
                    String keyText = key.toString();
                    Object value = entry.getValue();
                    if (value != null) {
                        String valueText = value.toString();
                        try {
                            long number = Long.parseLong(valueText);
                            answer.put(new File(dir, keyText), number);
                        } catch (NumberFormatException e) {
                            LOG.warn("Failed to parse checksum '" + valueText + "' in " + cacheFile + ". " + e, e);
                        }
                    }

                }
            }
        }
        return answer;
    }

    /**
     * Saves the given checksums into the folder in the {@link #FOLDER_CHECKSUM_CACHE} file
     */
    public static void saveInstalledChecksumCache(File dir, Map<File, Long> checksums) throws IOException {
        File cacheFile = new File(dir, FOLDER_CHECKSUM_CACHE);
        Properties properties = new Properties();
        Set<Map.Entry<File, Long>> entries = checksums.entrySet();
        for (Map.Entry<File, Long> entry : entries) {
            properties.put(entry.getKey().getName(), "" + entry.getValue());
        }
        properties.store(new FileWriter(cacheFile), "Updated on " + new Date());
    }
}
