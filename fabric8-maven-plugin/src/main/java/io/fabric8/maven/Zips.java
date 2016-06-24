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
package io.fabric8.maven;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.maven.plugin.logging.Log;

import static com.google.common.io.Closeables.close;

public final class Zips {

    /**
     * Creates a zip fie from the given source directory and output zip file name
     */
    public static void createZipFile(Log log, File sourceDir, File outputZipFile, File legalDir) throws IOException {
        outputZipFile.getParentFile().mkdirs();
        OutputStream os = new FileOutputStream(outputZipFile);
        ZipOutputStream zos = new ZipOutputStream(os);
        try {
            //zos.setLevel(Deflater.DEFAULT_COMPRESSION);
            //zos.setLevel(Deflater.NO_COMPRESSION);
            String path = "";
            FileFilter filter = null;
            zipDirectory(log, sourceDir, zos, path, filter);
            if (legalDir != null && legalDir.exists() && legalDir.isDirectory()) {
                zipDirectory(log, legalDir, zos, "META-INF/", new LegalFilter());
            }
        } finally {
            try {
                zos.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    /**
     * Zips the directory recursively into the ZIP stream given the starting path and optional filter
     */
    public static void zipDirectory(Log log, File directory, ZipOutputStream zos, String path, FileFilter filter) throws IOException {
        // get a listing of the directory content
        File[] dirList = directory.listFiles();
        byte[] readBuffer = new byte[8192];
        int bytesIn = 0;
        // loop through dirList, and zip the files
        if (dirList != null) {
            for (File f : dirList) {
                if (f.isDirectory()) {
                    String prefix = path + f.getName() + "/";
                    zos.putNextEntry(new ZipEntry(prefix));
                    zipDirectory(log, f, zos, prefix, filter);
                } else {
                    String entry = path + f.getName();
                    if (filter == null || filter.accept(f)) {
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
                        log.info("zipping file " + entry);
                    }
                }
                zos.closeEntry();
            }
        }
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
                        close(os, true);
                    }
                }
                entry = zis.getNextEntry();
            }
        } finally {
            close(zis, true);
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
            close(os, true);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<String> notNullList(List<String> list) {
        if (list == null) {
            return Collections.EMPTY_LIST;
        } else {
            return list;
        }
    }

    private static class LegalFilter implements FileFilter {

        @Override
        public boolean accept(File pathname) {
            return pathname.isFile() && (pathname.getName().startsWith("LICENSE") || pathname.getName().startsWith("NOTICE"));
        }
    }
}
