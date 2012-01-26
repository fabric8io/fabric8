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
package org.fusesource.fabric.fab.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;

/**
 */
public class Files {

    
    private static class DownloadKey {
        private final URL url;
        private final String tempFilePrefix;
        private final String tempFilePostfix;

        DownloadKey(URL url, String tempFilePrefix, String tempFilePostfix) {
            this.url = url;
            this.tempFilePrefix = tempFilePrefix;
            this.tempFilePostfix = tempFilePostfix;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DownloadKey that = (DownloadKey) o;

            if (tempFilePostfix != null ? !tempFilePostfix.equals(that.tempFilePostfix) : that.tempFilePostfix != null)
                return false;
            if (tempFilePrefix != null ? !tempFilePrefix.equals(that.tempFilePrefix) : that.tempFilePrefix != null)
                return false;
            if (url != null ? !url.equals(that.url) : that.url != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = url != null ? url.hashCode() : 0;
            result = 31 * result + (tempFilePrefix != null ? tempFilePrefix.hashCode() : 0);
            result = 31 * result + (tempFilePostfix != null ? tempFilePostfix.hashCode() : 0);
            return result;
        }
    }
    
    private static final ThreadLocal<HashSet<DownloadKey>> ACTIVE_DOWNLOADS = new ThreadLocal<HashSet<DownloadKey>>();



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
        HashSet<DownloadKey> original = ACTIVE_DOWNLOADS.get();
        HashSet<DownloadKey> downloads = original;
        if( downloads ==null ) {
            downloads = new HashSet<DownloadKey>();
            ACTIVE_DOWNLOADS.set(downloads);
        }
        try {
            DownloadKey key = new DownloadKey(url, tempFilePrefix, tempFilePostfix);
            if(downloads.contains(key)) {
                throw new IOException("The URL is being recursively downloaded: "+url);
            }
            downloads.add(key);
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
                downloads.remove(key);
            }
        } finally {
            if(original==null) {
                ACTIVE_DOWNLOADS.remove();
            }
        }

    }
}
