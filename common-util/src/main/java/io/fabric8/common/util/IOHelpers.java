package io.fabric8.common.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
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


    /**
     * Writes the given string as data to the given file
     */
    public static void writeTo(File newFile, String text) throws IOException {
        writeTo(newFile, new ByteArrayInputStream(text.getBytes()));
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
