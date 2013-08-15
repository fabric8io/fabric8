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
package org.fusesource.common.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashSet;

/**
 */
public class Files {


    private static final ThreadLocal<LinkedHashSet<URL>> ACTIVE_DOWNLOADS = new ThreadLocal<LinkedHashSet<URL>>();

    /**
     * Recursively deletes the given file whether its a file or directory returning the number
     * of files deleted
     */
    public static int recursiveDelete(File file) {
        int answer = 0;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    answer += recursiveDelete(child);
                }
            }
        }
        if (file.delete()) {
            answer += 1;
        }
        return answer;
    }

    public static File urlToFile(String url, String tempFilePrefix, String tempFilePostfix) throws IOException {
        File file = new File(url);
        if (file.exists()) {
            return file;
        } else {
            return urlToFile(new URL(url), tempFilePrefix, tempFilePostfix);
        }
    }

    public static class DownloadCycle extends IOException {
        public DownloadCycle(String s) {
            super(s);
        }
    }

    /**
     * Attempts to convert a URL to a file or copies the URL to a temporary file if it can't be easily converted
     */
    public static File urlToFile(URL url, String tempFilePrefix, String tempFilePostfix) throws IOException {
        LinkedHashSet<URL> original = ACTIVE_DOWNLOADS.get();
        LinkedHashSet<URL> downloads = original;
        if( downloads ==null ) {
            downloads = new LinkedHashSet<URL>();
            ACTIVE_DOWNLOADS.set(downloads);
        }
        try {
            if(downloads.contains(url)) {
                throw new DownloadCycle("Download cycle detected: "+downloads);
            }
            downloads.add(url);
            try {
                String fileName = url.getFile();
                File file = new File(fileName);
                if (!file.exists()) {
                    // we need to copy the URL to a new temp file for now...
                    file = File.createTempFile(tempFilePrefix, tempFilePostfix);
                    InputStream in = url.openStream();
                    IOHelpers.writeTo(file, in);
                }
                return file;
            } finally {
                downloads.remove(url);
            }
        } finally {
            if(original==null) {
                ACTIVE_DOWNLOADS.remove();
            }
        }

    }
}
