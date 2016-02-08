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
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestHelpers {

    static final public Path PROJECT_BASE_DIR;

    static {
        try {
            URL location = TestHelpers.class.getProtectionDomain().getCodeSource().getLocation();
            Path path = Paths.get(location.toURI());
            PROJECT_BASE_DIR = path.resolve("../..").normalize();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    static void recusiveDeleteIfExists(Path p) throws IOException {
        if( Files.isDirectory(p) ) {
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(p)) {
                for (Path path : directoryStream) {
                    recusiveDeleteIfExists(path);
                }
            }
        }
        Files.deleteIfExists(p);
    }

    public static String readTextFile(Path p) throws IOException {
        try(InputStream is = Files.newInputStream(p) ) {
            try(ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                copy(is, os);
                return new String(os.toByteArray(), StandardCharsets.UTF_8);
            }
        }
    }

    public static void copy(InputStream is, OutputStream os) throws IOException {
        byte data[] = new byte[1024*4];
        int c;
        while( (c=is.read(data)) >= 0 ) {
            os.write(data, 0, c);
        }
    }

}
