/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 */
package io.fabric8.stream.log;

import org.iq80.snappy.Snappy;

import java.io.*;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class Support {

    public static void displayResourceFile(String name) {
        InputStream is = Support.class.getResourceAsStream(name);
        try {
            int c;
            while((c=is.read())>=0) {
                System.out.write(c);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
            }
        }
    }

    public static void writeText(File file, String value) throws IOException {
        FileOutputStream os = new FileOutputStream(file);
        try {
            os.write(value.getBytes());
        } finally {
            os.close();
        }
    }

    public static String readText(File file) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileInputStream is = new FileInputStream(file);
        try {
            int c;
            while((c=is.read()) >= 0 ) {
                baos.write(c);
            }
        } finally {
            is.close();
        }
        return new String(baos.toByteArray());
    }

    public static int lastnlposition(byte[] data, int len) {
        // have we received an entire log line yet?
        int at = -1;
        for(int i=len-1; i >= 0; i--) {
            if(data[i] == '\n') {
                at = i;
                break;
            }
        }
        return at;
    }

    static public byte[] compress(byte[] data) {
        byte[] compressed = new byte[Snappy.maxCompressedLength(data.length)];
        int len = Snappy.compress(data, 0, data.length, compressed, 0);
        byte[] result = new byte[len];
        System.arraycopy(compressed, 0, result, 0, len);
        return result;
    }

}

