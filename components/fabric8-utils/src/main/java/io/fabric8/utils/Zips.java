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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static io.fabric8.utils.Closeables.closeQuietly;

/**
 */
public class Zips {

    /**
     * Creates a zip fie from the given source directory and output zip file name
     */
    public static void createZipFile(Logger log, File sourceDir, File outputZipFile) throws IOException {
        FileFilter filter = null;
        createZipFile(log, sourceDir, outputZipFile, filter);
    }

    public static void createZipFile(Logger log, File sourceDir, File outputZipFile, FileFilter filter) throws IOException {
        outputZipFile.getParentFile().mkdirs();
        OutputStream os = new FileOutputStream(outputZipFile);
        ZipOutputStream zos = new ZipOutputStream(os);
        try {
            //zos.setLevel(Deflater.DEFAULT_COMPRESSION);
            //zos.setLevel(Deflater.NO_COMPRESSION);
            String path = "";
            zipDirectory(log, sourceDir, zos, path, filter);
        } finally {
            Closeables.closeQuietly(zos);
        }
    }

    /**
     * Zips the directory recursively into the ZIP stream given the starting path and optional filter
     */
    public static void zipDirectory(Logger log, File directory, ZipOutputStream zos, String path, FileFilter filter) throws IOException {
        // get a listing of the directory content
        File[] dirList = directory.listFiles();
        byte[] readBuffer = new byte[8192];
        int bytesIn = 0;
        // loop through dirList, and zip the files
        if (dirList != null) {
            for (File f : dirList) {
                if (f.isDirectory()) {
                    String prefix = path + f.getName() + "/";
                    if (matches(filter, f)) {
                        zos.putNextEntry(new ZipEntry(prefix));
                        zipDirectory(log, f, zos, prefix, filter);
                    }
                } else {
                    String entry = path + f.getName();
                    if (matches(filter, f)) {
                        FileInputStream fis = new FileInputStream(f);
                        try {
                            ZipEntry anEntry = new ZipEntry(entry);
                            zos.putNextEntry(anEntry);
                            bytesIn = fis.read(readBuffer);
                            while (bytesIn != -1) {
                                zos.write(readBuffer, 0, bytesIn);
                                bytesIn = fis.read(readBuffer);
                            }
                        } finally {
                            fis.close();
                        }
                        if (log.isDebugEnabled()) {
                            log.debug("zipping file " + entry);
                        }
                    }
                }
                zos.closeEntry();
            }
        }
    }

    protected static boolean matches(FileFilter filter, File f) {
        return filter == null || filter.accept(f);
    }

    /**
     * Unzips the given input stream of a ZIP to the given directory
     */
    public static void unzip(InputStream in, File toDir) throws IOException {
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(in));
        try {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    String entryName = entry.getName();
                    File toFile = new File(toDir, entryName);
                    toFile.getParentFile().mkdirs();
                    OutputStream os = new FileOutputStream(toFile);
                    try {
                        try {
                            copy(zis, os);
                        } finally {
                            zis.closeEntry();
                        }
                    } finally {
                        Closeables.closeQuietly(os);
                    }
                }
                entry = zis.getNextEntry();
            }
        } finally {
            closeQuietly(zis);
        }
    }

    static void copy(InputStream is, OutputStream os) throws IOException {
        try {
            byte[] b = new byte[4096];
            int l = is.read(b);
            while (l >= 0) {
                os.write(b, 0, l);
                l = is.read(b);
            }
        } finally {
            Closeables.closeQuietly(os);
        }
    }

}
