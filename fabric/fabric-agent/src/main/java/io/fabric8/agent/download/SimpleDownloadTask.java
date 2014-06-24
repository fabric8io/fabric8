/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.agent.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleDownloadTask extends AbstractDownloadTask {

    private static final String BLUEPRINT_PREFIX = "blueprint:";
    private static final String SPRING_PREFIX = "spring:";

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDownloadTask.class);
    /**
     * 2 spaces indent;
     */
    private static final String Ix2 = "  ";
    /**
     * 4 spaces indent;
     */
    private static final String Ix4 = "    ";

    private File basePath;

    public SimpleDownloadTask(String url, ExecutorService executor) {
        this(url, executor, new File("."));
    }

    public SimpleDownloadTask(String url, ExecutorService executor, File basePath) {
        super(url, executor);
        this.basePath = basePath;
    }

    @Override
    protected File download() throws Exception {
        LOG.trace("Downloading [" + url + "]");

        // we should skip fab: from the url, as we do not want to trigger fab: url handler to kick-in during download
        // which will resolve FAB and attempt to install causing fabric to report issues such as:
        // The container is managed by fabric, please use fabric:profile-edit --features camel-core target-profile instead.
        String s = url;
        if (s.startsWith("fab:")) {
            s = s.substring(4);
        }

        if (s.startsWith(BLUEPRINT_PREFIX) || s.startsWith(SPRING_PREFIX)) {
            return downloadBlueprintOrSpring();
        }

        try {
            URL urlObj = new URL(s);
            File file = new File(basePath, getFileName(urlObj.getFile()));
            if (file.exists()) {
                return file;
            }

            File dir = new File(System.getProperty("karaf.data"), "fabric-agent");
            dir.mkdirs();
            if (!dir.isDirectory()) {
                throw new IOException("Unable to create directory " + dir.toString());
            }

            File tmpFile = File.createTempFile("download-", null, dir);

            InputStream is = urlObj.openStream();
            try {
                OutputStream os = new FileOutputStream(tmpFile);
                try {
                    copy(is, os);
                } finally {
                    os.close();
                }
            } finally {
                is.close();
            }

            if (file.exists() && !file.delete()) {
                throw new IOException("Unable to delete file: " + file.toString());
            }
            // check: this will move the file to CHILD_HOME root directory...
            if (!tmpFile.renameTo(file)) {
                throw new IOException("Unable to rename file " + tmpFile.toString() + " to " + file.toString());
            }
            return file;
        } catch (IOException ignore) {
            // go on with next repository
            LOG.debug(Ix2 + "Could not download [" + url + "]");
            LOG.trace(Ix2 + "Reason [" + ignore.getClass().getName() + ": " + ignore.getMessage() + "]");
        }

        // no artifact found
        throw new IOException("URL [" + url + "] could not be resolved.");
    }

    // we only want the filename itself, not the whole path
    private String getFileName(String url) {
        // ENTESB-1394: we do not want all these decorators from wrap: protocol
        url = DownloadManagerHelper.stripUrl(url);
        int unixPos = url.lastIndexOf('/');
        int windowsPos = url.lastIndexOf('\\');
        return url.substring(Math.max(unixPos, windowsPos) + 1);
    }

    protected File downloadBlueprintOrSpring() throws Exception {
        // when downloading an embedded blueprint or spring xml file, then it must be as a temporary file
        File dir = new File(System.getProperty("karaf.data"), "fabric-agent");
        dir.mkdirs();
        File tmpFile = File.createTempFile("download-", null, dir);
        InputStream is = new URL(url).openStream();
        try {
            OutputStream os = new FileOutputStream(tmpFile);
            try {
                copy(is, os);
            } finally {
                os.close();
            }
        } finally {
            is.close();
        }
        return tmpFile;
    }
}
