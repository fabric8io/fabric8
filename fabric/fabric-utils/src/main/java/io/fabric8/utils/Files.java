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

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

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
        byte[] bytes = readBytes(file);
        if (charset != null) {
            return new String(bytes, charset);
        } else {
            return new String(bytes);
        }
    }


    /**
     * Reads an {@link InputStream} and returns a {@String}.
     * @throws IOException
     */
    public static String toString(InputStream inputStream) throws IOException {
        return toString(inputStream, null);
    }



    /**
     * Reads an {@link InputStream} and returns a {@String}.
     * @throws IOException
     */
    public static String toString(InputStream inputStream, Charset charset) throws IOException {
        byte[] bytes = readBytes(inputStream);
        if (charset != null) {
            return new String(bytes, charset);
        } else {
            return new String(bytes);
        }
    }


    /**
     * Reads a {@link File} and returns the list of lines
     */
    public static List<String> readLines(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        List<String> answer = new ArrayList<String>();
        try {
            while (true) {
                String line = reader.readLine();
                if (line != null) {
                    answer.add(line);
                } else {
                    break;
                }
            }
        } finally {
            reader.close();
        }
        return answer;
    }

    /**
     * Writes the given lines to the {@link File}
     */
    public static void writeLines(File file, List<String> lines) throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(file));
        try {
            for (String line : lines) {
                writer.println(line);
            }
        } finally {
            writer.close();
        }
    }

    /**
     * Reads a {@link File} and returns the data as a byte array
     *
     * @throws IOException
     */
    public static byte[] readBytes(File file) throws IOException {
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
            return bos.toByteArray();
        } finally {
            Closeables.closeQuitely(fis);
            Closeables.closeQuitely(bos);
        }
    }

    /**
     * Reads an {@link InputStream} and returns the data as a byte array
     *
     * @throws IOException
     */
    public static byte[] readBytes(InputStream in) throws IOException {
        ByteArrayOutputStream bos = null;
        if (in == null) {
            throw new FileNotFoundException("No InputStream specified");
        }
        try {
            bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int remaining;
            while ((remaining = in.read(buffer)) > 0) {
                bos.write(buffer, 0, remaining);
            }
            return bos.toByteArray();
        } finally {
            Closeables.closeQuitely(in);
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
     * Writes {@link String} content to {@link File}.
     *
     * @param file
     * @param content
     * @throws IOException
     */
    public static void writeToFile(File file, byte[] content) throws IOException {
        FileOutputStream fos = null;
        try {
            if (file == null) {
                throw new FileNotFoundException("No file specified.");
            } else if (!file.exists() && !file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                throw new FileNotFoundException("Could not find or create file:" + file.getName());
            }
            fos = new FileOutputStream(file);
            fos.write(content);
        } finally {
            Closeables.closeQuitely(fos);
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


    public static String getRelativePath(File rootDir, File file) throws IOException {
        String rootPath = rootDir.getCanonicalPath();
        String fullPath = file.getCanonicalPath();
        if (fullPath.startsWith(rootPath)) {
            return fullPath.substring(rootPath.length());
        } else {
            return fullPath;
        }
    }

    /**
     * Throws an exception if the given file or directory does not exist
     */
    public static void assertExists(File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException(file + " does not exist");
        }
    }

    /**
     * Throws an exception if the given file does not exist
     */
    public static void assertFileExists(File file) {
        assertExists(file);
        if (!file.isFile()) {
            throw new IllegalArgumentException(file + " is not a file!");
        }
    }


    /**
     * Throws an exception if the given file does not exist
     */
    public static void assertDirectoryExists(File file) {
        assertExists(file);
        if (!file.isDirectory()) {
            throw new IllegalArgumentException(file + " is not a directory!");
        }
    }

}
