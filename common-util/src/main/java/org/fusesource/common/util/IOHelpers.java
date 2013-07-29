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
package org.fusesource.common.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;

/**
 */
public class IOHelpers {

    protected static final int BUFFER_SIZE = 16 * 1024;

    public static void writeTo(File file, InputStream in) throws IOException {
        writeTo(file, in, BUFFER_SIZE);
    }


    /**
     * Writes the given string as data to the given file
     */
    public static void writeTo(File newFile, String text) throws IOException {
        writeTo(newFile, new ByteArrayInputStream(text.getBytes()));
    }

    public static void writeTo(File file, InputStream in, int bufferSize) throws IOException {
        writeTo(new FileOutputStream(file), in, bufferSize, true);
    }

    public static void writeTo(OutputStream outputStream, InputStream in, boolean close) throws IOException {
        writeTo(outputStream, in, BUFFER_SIZE, close);
    }

    public static void writeTo(OutputStream outputStream, InputStream in, int bufferSize, boolean close) throws IOException {
        BufferedOutputStream out = new BufferedOutputStream(outputStream, bufferSize);
        BufferedInputStream bufferedIn = new BufferedInputStream(in, bufferSize);
        while (true) {
            int b = bufferedIn.read();
            if (b >= 0) {
                out.write(b);
            } else {
                in.close();
                if (close) {
                    out.close();
                } else {
                    out.flush();
                }
                return;
            }
        }
    }

}
