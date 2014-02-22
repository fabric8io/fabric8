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
package org.fusesource.patch.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

public class Utils {

    public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        try {
            byte[] buffer = new byte[8192];
            int len;
            for (; ;) {
                len = inputStream.read(buffer);
                if (len > 0) {
                    outputStream.write(buffer, 0, len);
                } else {
                    outputStream.flush();
                    break;
                }
            }
        } finally {
            close(inputStream, outputStream);
        }
    }

    public static void copy(Reader reader, Writer writer) throws IOException {
        try {
            char[] buffer = new char[8192];
            int len;
            for (; ;) {
                len = reader.read(buffer);
                if (len > 0) {
                    writer.write(buffer, 0, len);
                } else {
                    writer.flush();
                    break;
                }
            }
        } finally {
            close(reader, writer);
        }
    }

    public static void close(ZipFile... closeables) {
        for (ZipFile c : closeables) {
            try {
                if (c != null) {
                    c.close();
                }
            } catch (IOException e) {
            }
        }
    }

    public static void close(Closeable... closeables) {
        for (Closeable c : closeables) {
            try {
                if (c != null) {
                    c.close();
                }
            } catch (IOException e) {
            }
        }
    }

    public static String readFully(File file) throws IOException {
        if (!file.isFile()) {
            return null;
        }
        Reader r = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        StringWriter w = new StringWriter();
        try {
            copy(r, w);
            return w.toString();
        } finally {
            close(r, w);
        }
    }

    public static void writeFully(File file, String content) throws IOException {
        if (content == null) {
            file.delete();
        } else {
            Reader r = new StringReader(content);
            Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            try {
                copy(r, w);
            } finally {
                close(r, w);
            }
        }
    }

    public static List<String> readLines(File file) throws IOException {
        List<String> overrides = new ArrayList<String>();
        if (file.exists()) {
            InputStream is = new FileInputStream(file);
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    overrides.add(line);
                }
            } finally {
                is.close();
            }
        }
        return overrides;
    }

    public static void writeLines(File file, List<String> lines) throws IOException {
        OutputStream os = new FileOutputStream(file);
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
            for (String over : lines) {
                writer.write(over);
                writer.newLine();
            }
            writer.close();
        } finally {
            os.close();
        }
    }

}
