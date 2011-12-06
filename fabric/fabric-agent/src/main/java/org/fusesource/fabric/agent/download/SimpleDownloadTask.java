/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.agent.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.concurrent.ScheduledExecutorService;

public class SimpleDownloadTask extends AbstractDownloadTask {

    public SimpleDownloadTask(String url, ScheduledExecutorService executor) {
        super(url, executor);
    }

    @Override
    protected File download() throws Exception {
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
