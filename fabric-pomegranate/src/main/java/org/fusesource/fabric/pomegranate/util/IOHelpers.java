/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.pomegranate.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 */
public class IOHelpers {

    public static void writeTo(File file, InputStream in) throws IOException {
        writeTo(file, in, 16 * 1024);
    }

    public static void writeTo(File file, InputStream in, int bufferSize) throws IOException {
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        BufferedInputStream bufferedIn = new BufferedInputStream(in, bufferSize);
        while (true) {
            int b = bufferedIn.read();
            if (b >= 0) {
                out.write(b);
            } else {
                out.close();
                return;
            }
        }
    }
}
