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
package io.fabric8.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * File utilities
 */
public final class Files {

    private static final ThreadLocal<LinkedHashSet<URL>> ACTIVE_DOWNLOADS = new ThreadLocal<LinkedHashSet<URL>>();
    private static final AtomicLong lastTmpFileId = new AtomicLong(System.currentTimeMillis());

    private static final int BUFFER_SIZE = 8192;
    private static boolean windowsOs = initWindowsOs();

    private Files() {
        // Utils method
    }

    private static boolean initWindowsOs() {
        // initialize once as System.getProperty is not fast
        String osName = System.getProperty("os.name").toLowerCase(Locale.US);
        return osName.contains("windows");
    }

    /**
     * Returns true if the given file is of type file and exists
     */
    public static boolean isFile(File file) {
        return file != null && file.exists() && file.isFile();
    }

    /**
     * Returns true if the given file is of type directory and exists
     */
    public static boolean isDirectory(File file) {
        return file != null && file.exists() && file.isDirectory();
    }


    /**
     * Normalizes the path to cater for Windows and other platforms
     */
    public static String normalizePath(String path) {
        if (path == null) {
            return null;
        }

        if (isWindows()) {
            // special handling for Windows where we need to convert / to \\
            return normalizePath(path, '/', '\\');
        } else {
            // for other systems make sure we use / as separators
            return normalizePath(path, '\\', '/');
        }
    }

    public static String normalizePath(String path, char from, char to) {
        return path.replace(from, to);
    }

    /**
     * Returns true, if the OS is windows
     */
    public static boolean isWindows() {
        return windowsOs;
    }

    /**
     * Strip any leading separators
     */
    public static String stripLeadingSeparator(String name) {
        if (name == null) {
            return null;
        }
        while (name.startsWith("/") || name.startsWith(File.separator)) {
            name = name.substring(1);
        }
        return name;
    }

    /**
     * Returns the file name part of the path
     */
    public static String getFileName(String path) {
        if (path != null) {
            return new File(path).getName();
        }
        return null;
    }

    /**
     * Returns the file extension of the file name of the path or null
     */
    public static String getFileExtension(String path) {
        String fileName = getFileName(path);
        if (fileName != null) {
            int idx = fileName.lastIndexOf('.');
            if (idx > 1) {
                String answer = fileName.substring(idx + 1);
                if (answer.length() > 0) {
                    return answer;
                }
            }
        }
        return null;
    }

    public static String getFileExtension(File file) {
        String fileName = file.getName();
        if (fileName != null) {
            int idx = fileName.lastIndexOf('.');
            if (idx > 1) {
                String answer = fileName.substring(idx + 1);
                if (answer.length() > 0) {
                    return answer;
                }
            }
        }
        return null;
    }

    /**
     * Creates a temporary file.
     */
    public static File createTempFile(String path) throws IOException {
        File dataDir = new File(path);
        File tmpDir = new File(dataDir, "tmp");
        if (!tmpDir.exists() && !tmpDir.mkdirs()) {
            throw new IOException("Failed to create tmp dir:" + tmpDir.getAbsolutePath());
        }
        return File.createTempFile(String.valueOf(lastTmpFileId.incrementAndGet()), ".tmp", tmpDir);
    }

    /**
     * Reads a {@link File} and returns a {@String}.
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
     */
    public static String toString(InputStream inputStream) throws IOException {
        return toString(inputStream, null);
    }


    /**
     * Reads an {@link InputStream} and returns a {@String}.
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
            Closeables.closeQuietly(fis);
            Closeables.closeQuietly(bos);
        }
    }

    /**
     * Reads an {@link InputStream} and returns the data as a byte array
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
            Closeables.closeQuietly(in);
            Closeables.closeQuietly(bos);
        }
    }

    /**
     * Reads a {@link File} and returns a {@String}.
     */
    public static String toString(File file) throws IOException {
        return toString(file, null);
    }

    /**
     * Writes {@link String} content to {@link File}.
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
            Closeables.closeQuietly(fos);
            Closeables.closeQuietly(writer);
        }
    }


    /**
     * Writes {@link String} content to {@link File}.
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
            Closeables.closeQuietly(fos);
        }
    }

    /**
     * Copy the source {@link File} to the target {@link File}.
     */
    public static void copy(File source, File target) throws IOException {
        if (!source.exists()) {
            throw new FileNotFoundException("Source file not found:" + source.getAbsolutePath());
        }

        if (!target.exists() && !target.getParentFile().exists() && !target.getParentFile().mkdirs()) {
            throw new IOException("Can't create target directory:" + target.getParentFile().getAbsolutePath());
        }
        if (source.isDirectory()) {
            target.mkdirs();
            File[] files = source.listFiles();
            if (files != null) {
                for (File child : files) {
                    copy(child, new File(target, child.getName()));
                }
            }
        } else {
            FileInputStream is = new FileInputStream(source);
            FileOutputStream os = new FileOutputStream(target);
            copy(is, os);
        }
    }

    /**
     * Copy the {@link InputStream} to the {@link OutputStream}.
     */
    public static void copy(InputStream is, OutputStream os) throws IOException {
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            for (; ; ) {
                len = is.read(buffer);
                if (len > 0) {
                    os.write(buffer, 0, len);
                } else {
                    break;
                }
            }
        } finally {
            Closeables.closeQuietly(is);
            Closeables.closeQuietly(os);
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

    public static Set<File> recursiveList(File root, FilenameFilter filter) {
        Set<File> result = new HashSet<>();
        if (root != null) {
            result.add(root);
            if (root.isDirectory()) {
                for (File child : root.listFiles(filter)) {
                    result.addAll(recursiveList(child, filter));
                }
            }
        }
        return result;
    }


    /**
     * Recursively deletes the given file whether its a file or directory returning the number
     * of files deleted
     */
    public static int recursiveDelete(File file) {
        int answer = 0;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    answer += recursiveDelete(child);
                }
            }
        }
        if (file.delete()) {
            answer += 1;
        }
        return answer;
    }

    /**
     * Recursively finds all files matching the given filter and adds them to the collection
     */
    public static void findRecursive(File file, Filter<File> filter, Collection<File> collection) {
        if (filter.matches(file)) {
            collection.add(file);
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    findRecursive(child, filter, collection);
                }
            }
        }
    }

    /**
     * Returns the files found for the given file and directories which match the given filter
     */
    public static Set<File> findRecursive(File file, Filter<File> filter) {
        Set<File> files = new HashSet<>();
        findRecursive(file, filter, files);
        return files;
    }


    /**
     * Recursively deletes the file and any children files if its a directory
     */
    public static void recursiveDelete(File file, FileFilter filter) {
        if (filter == null || filter.accept(file)) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) {
                    for (File child : files) {
                        recursiveDelete(child, filter);
                    }
                }
                files = file.listFiles();
                // lets not delete if we didn't delete a child file
                if (files == null || files.length == 0) {
                    file.delete();
                }
            } else {
                file.delete();
            }
        }
    }

    public static File urlToFile(String url, String tempFilePrefix, String tempFilePostfix) throws IOException {
        File file = new File(url);
        if (file.exists()) {
            return file;
        } else {
            return urlToFile(new URL(url), tempFilePrefix, tempFilePostfix);
        }
    }

    @SuppressWarnings("serial")
    public static class DownloadCycleException extends IOException {
        public DownloadCycleException(String s) {
            super(s);
        }
    }

    /**
     * Attempts to convert a URL to a file or copies the URL to a temporary file if it can't be easily converted
     */
    public static File urlToFile(URL url, String tempFilePrefix, String tempFilePostfix) throws IOException {
        LinkedHashSet<URL> original = ACTIVE_DOWNLOADS.get();
        LinkedHashSet<URL> downloads = original;
        if (downloads == null) {
            downloads = new LinkedHashSet<URL>();
            ACTIVE_DOWNLOADS.set(downloads);
        }
        try {
            if (downloads.contains(url)) {
                throw new DownloadCycleException("Download cycle detected: " + downloads);
            }
            downloads.add(url);
            try {
                String fileName = url.getFile();
                File file = new File(fileName);
                if (!file.exists()) {
                    // we need to copy the URL to a new temp file for now...
                    file = File.createTempFile(tempFilePrefix, tempFilePostfix);
                    InputStream in = url.openStream();
                    IOHelpers.writeTo(file, in);
                }
                return file;
            } finally {
                downloads.remove(url);
            }
        } finally {
            if (original == null) {
                ACTIVE_DOWNLOADS.remove();
            }
        }

    }

    public static String getExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int index = filename.lastIndexOf(".");
        if (index == -1) {
            return "";
        } else {
            return filename.substring(index + 1);
        }
    }

    /**
     * Returns the MIME type of the given file
     */
    public static String guessMediaType(File fileName) {
        return guessMediaType(fileName.getName());
    }

    /**
     * Returns the MIME type of the given file name
     */
    public static String guessMediaType(String fileName) {
        if (fileName.endsWith(".xml")) {
            return "application/xml";
        }
        if (fileName.endsWith(".wadl")) {
            return "application/wadl+xml";
        }
        if (fileName.endsWith(".wsdl")) {
            return "application/wsdl+xml";
        }
        if (fileName.endsWith(".xsd")) {
            return "application/xsd+xml";
        }
        if (fileName.endsWith(".json")) {
            return "application/json";
        }
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "application/html";
        }
        if (fileName.endsWith(".properties")) {
            return "text/x-java-properties";
        }
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (fileName.endsWith(".png")) {
            return "image/png";
        }
        if (fileName.endsWith(".gif")) {
            return "image/gif";
        }
        if (fileName.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "text/plain";
    }
}
