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
package org.jboss.fuse.rhaccess.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileUtil {
    final static int BUFFER = 2048;
    private static Logger log = LoggerFactory.getLogger(FileUtil.class);

    public static void zipSingleFile(File contentPath, File destination) throws IOException {
        // input file
        FileInputStream in = new FileInputStream(contentPath);

        // out put file
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(destination));

        // name the file inside the zip  file
        out.putNextEntry(new ZipEntry(contentPath.getName()));

        // buffer size
        byte[] b = new byte[1024];
        int count;

        while ((count = in.read(b)) > 0) {
            System.out.println();
            out.write(b, 0, count);
        }
        out.close();
        in.close();
    }

    public static boolean createZipArchive(String srcFolder, File destination) {

        try {
            BufferedInputStream origin = null;

            FileOutputStream dest = new FileOutputStream(destination);

            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
            byte data[] = new byte[BUFFER];

            File subDir = new File(srcFolder);
            String subdirList[] = subDir.list();
            for (String sd : subdirList) {
                // get a list of files from current directory
                File f = new File(srcFolder + "/" + sd);
                if (f.isDirectory()) {
                    String files[] = f.list();

                    for (int i = 0; i < files.length; i++) {
                        log.debug("Adding: " + files[i]);
                        FileInputStream fi = new FileInputStream(srcFolder + "/" + sd + "/" + files[i]);
                        origin = new BufferedInputStream(fi, BUFFER);
                        ZipEntry entry = new ZipEntry(sd + "/" + files[i]);
                        out.putNextEntry(entry);
                        int count;
                        while ((count = origin.read(data, 0, BUFFER)) != -1) {
                            out.write(data, 0, count);
                            out.flush();
                        }

                    }
                } else //it is just a file
                {
                    FileInputStream fi = new FileInputStream(f);
                    origin = new BufferedInputStream(fi, BUFFER);
                    ZipEntry entry = new ZipEntry(sd);
                    out.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(data, 0, BUFFER)) != -1) {
                        out.write(data, 0, count);
                        out.flush();
                    }

                }
            }
            origin.close();
            out.flush();
            out.close();
        } catch (Exception e) {
            log.info("createZipArchive threw exception: " + e.getMessage());
            return false;

        }
        return true;
    }
}