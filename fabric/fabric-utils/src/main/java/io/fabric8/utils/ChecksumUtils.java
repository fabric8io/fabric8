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
package io.fabric8.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;

public class ChecksumUtils {

    private ChecksumUtils() {
    }

    /**
     * Compute a cheksum for the file or directory that consists of the name, length and the last modified date
     * for a file and its children in case of a directory
     *
     * @param is the input stream
     * @return a checksum identifying any change
     */
    public static long checksum(InputStream is) throws IOException
    {
        try {
            CRC32 crc = new CRC32();
            byte[] buffer = new byte[8192];
            int l;
            while ((l = is.read(buffer)) > 0) {
                crc.update(buffer, 0, l);
            }
            return crc.getValue();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

}
