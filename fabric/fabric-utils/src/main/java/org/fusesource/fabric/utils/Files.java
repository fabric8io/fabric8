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
package org.fusesource.fabric.utils;

import java.io.*;
import java.nio.charset.Charset;

public class Files {

    private static final int BUFFER_SIZE = 8192;

    /**
     * Creates a temporary file.
     * @return
     * @throws IOException
     */
    public static File createTempFile() throws IOException {
        File dataDir = new File(System.getProperty(SystemProperties.KARAF_DATA));
        File tmpDir = new File(dataDir, "tmp");
        if (!tmpDir.exists() && !tmpDir.mkdirs()) {
            throw new IOException("Failed to create tmp dir:" + tmpDir.getAbsolutePath());
        }
       return File.createTempFile(String.valueOf(System.currentTimeMillis()),".tmp", tmpDir);
    }

    /**
     * Reads a {@link File} and returns a {@String}.
     *
     * @param file
     * @param charset
     * @return
     * @throws IOException
     */
    public static String toString(File file, Charset charset) throws IOException {
        FileInputStream fis = null;
        ByteArrayOutputStream bos = null;
        if (file == null) {
            throw new FileNotFoundException("No file specified");
        }
        try {
            fis = new FileInputStream(file);
            bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int remaining;
            while ((remaining = fis.read(buffer)) > 0) {
                bos.write(buffer, 0, remaining);
            }
            if (charset != null) {
                return new String(bos.toByteArray(), charset);
            } else {
                return new String(bos.toByteArray());
            }

        } finally {
            Closeables.closeQuitely(fis);
            Closeables.closeQuitely(bos);
        }
    }

    /**
     * Reads a {@link File} and returns a {@String}.
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static String toString(File file) throws IOException {
        return toString(file, null);
    }

    /**
     * Writes {@link String} content to {@link File}.
     *
     * @param file
     * @param content
     * @param charset
     * @throws IOException
     */
    public static void writeToFile(File file, String content, Charset charset) throws IOException {
        FileOutputStream fos = null;
        OutputStreamWriter writer = null;
        try {
            if (file == null) {
                throw new FileNotFoundException("No file specified.");
            } else if (!file.exists() && !file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                throw new FileNotFoundException("Could not find or create file:" + file.getName());
            }
            fos = new FileOutputStream(file);
            writer = new OutputStreamWriter(fos, charset);
            writer.write(content, 0, content.length());
            writer.flush();
        } finally {
            Closeables.closeQuitely(fos);
            Closeables.closeQuitely(writer);
        }
    }

    /**
     * Copy the source {@link File} to the target {@link File}.
     * @param source
     * @param target
     * @throws IOException
     */
    public static void copy(File source,File target) throws IOException {
        if (!source.exists()) {
             throw new FileNotFoundException("Source file not found:"+source.getAbsolutePath());
        }

        if (!target.exists() && !target.getParentFile().exists() && !target.getParentFile().mkdirs()) {
            throw new IOException("Can't create target directory:"+target.getParentFile().getAbsolutePath());
        }
        FileInputStream is = new FileInputStream(source);
        FileOutputStream os = new FileOutputStream(target);
        copy(is, os);
    }

    /**
     * Copy the {@link InputStream} to the {@link OutputStream}.
     * @param is
     * @param os
     * @throws IOException
     */
    public static void copy(InputStream is, OutputStream os) throws IOException {
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            for (; ;) {
                len = is.read(buffer);
                if (len > 0) {
                    os.write(buffer, 0, len);
                } else {
                    break;
                }
            }
        } finally {
            Closeables.closeQuitely(is);
            Closeables.closeQuitely(os);
        }
    }
}
