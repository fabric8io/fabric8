/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 */
public class IOHelpers {

    protected static final int BUFFER_SIZE = 16 * 1024;

    public static void writeTo(File file, InputStream in) throws IOException {
        writeTo(file, in, BUFFER_SIZE);
    }

    public static void writeTo(File file, InputStream in, int bufferSize) throws IOException {
        writeTo(new FileOutputStream(file), in, bufferSize, true);
    }

    public static void writeTo(OutputStream outputStream, InputStream in, boolean close) throws IOException {
        writeTo(outputStream, in, BUFFER_SIZE, close);
    }

    public static void writeTo(OutputStream outputStream, InputStream in, int bufferSize, boolean close) throws IOException {
        BufferedOutputStream out = new BufferedOutputStream(outputStream, bufferSize);
        BufferedInputStream bufferedIn = new BufferedInputStream(in, bufferSize);
        while (true) {
            int b = bufferedIn.read();
            if (b >= 0) {
                out.write(b);
            } else {
                in.close();
                if (close) {
                    out.close();
                } else {
                    out.flush();
                }
                return;
            }
        }
    }
}
