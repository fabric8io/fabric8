/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 */
public class Files {

    public static File urlToFile(String url, String tempFilePrefix, String tempFilePostfix) throws IOException {
        File file = new File(url);
        if (file.exists()) {
            return file;
        } else {
            return urlToFile(new URL(url), tempFilePrefix, tempFilePostfix);
        }
    }

    /**
     * Attempts to convert a URL to a file or copies the URL to a temporary file if it can't be easily converted
     */
    public static File urlToFile(URL url, String tempFilePrefix, String tempFilePostfix) throws IOException {
        String fileName = url.getFile();
        File file = new File(fileName);
        if (!file.exists()) {
            // we need to copy the URL to a new temp file for now...
            file = File.createTempFile(tempFilePrefix, tempFilePostfix);
            InputStream in = url.openStream();
            IOHelpers.writeTo(file, in);
        }
        return file;
    }
}
