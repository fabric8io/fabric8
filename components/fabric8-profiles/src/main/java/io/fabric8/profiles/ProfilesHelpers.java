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
package io.fabric8.profiles;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

public class ProfilesHelpers {
    public static String DELETED = "#deleted#";

    public static Properties readPropertiesFile(Path path) throws IOException {
        Properties properties = new Properties();
        try (InputStream is = Files.newInputStream(path)) {
            properties.load(is);
        }
        return properties;
    }

    public static byte[] toBytes(Properties properties) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            properties.store(os, null);
            return os.toByteArray();
        }
    }

    public static void recusivelyCollectFileListing(ArrayList<String> rc, Path base, Path directory) throws IOException {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
            for (Path path : directoryStream) {
                if (Files.isDirectory(path)) {
                    recusivelyCollectFileListing(rc, base, path);
                } else {
                    rc.add(base.relativize(path).toString());
                }
            }
        }
    }

    public static void merge(Properties target, Properties source) {
        if( source.contains(DELETED) ) {
            target.clear();
        } else {
            for (Map.Entry<Object, Object> entry : source.entrySet()) {
                if (DELETED.equals(entry.getValue())) {
                    target.remove(entry.getKey());
                } else {
                    target.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

}
