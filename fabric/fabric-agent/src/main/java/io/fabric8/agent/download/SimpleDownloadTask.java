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
    
    public SimpleDownloadTask(String url, ExecutorService executor) {
        super(url, executor);
    }

    @Override
    protected File download() throws Exception {
        try {
            LOG.trace("Downloading [" + url + "]");
            
            URL urlObj = new URL(url);
            File file = new File(urlObj.getFile());
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
}
