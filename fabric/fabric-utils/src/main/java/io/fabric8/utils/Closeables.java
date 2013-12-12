package io.fabric8.utils;


import java.io.Closeable;

public final class Closeables {

    private Closeables() {
        //Utility Class
    }

    public static void closeQuitely(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ex) {
                //ignore
            }
        }
    }
}
