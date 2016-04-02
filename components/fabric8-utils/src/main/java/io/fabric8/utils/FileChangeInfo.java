/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.utils;

import java.io.File;
import java.io.IOException;

/**
 * A helper class used to help detect if a file changes in contents between a copy/write
 * operation.
 */
public class FileChangeInfo {
    private final long length;
    private final long checksum;

    public FileChangeInfo(long length, long checksum) {
        this.length = length;
        this.checksum = checksum;
    }

    /**
     * Returns a file info object if the file is not null and exists otherwise returns null
     */
    public static FileChangeInfo newInstance(File file) throws IOException {
        if (file != null && file.isFile() && file.exists()) {
            long checksum = ChecksumUtils.checksumFile(file);
            return new FileChangeInfo(file.length(), checksum);
        } else {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileChangeInfo that = (FileChangeInfo) o;

        if (checksum != that.checksum) return false;
        if (length != that.length) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (length ^ (length >>> 32));
        result = 31 * result + (int) (checksum ^ (checksum >>> 32));
        return result;
    }

    public long getLength() {
        return length;
    }

    public long getChecksum() {
        return checksum;
    }
}
