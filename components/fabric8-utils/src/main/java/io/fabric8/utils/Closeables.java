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

import java.io.Closeable;
import java.io.InputStream;
import java.io.Reader;

public final class Closeables {

    private Closeables() {
        //Utility Class
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ex) {
                //ignore
            }
        }
    }

    public static void closeQuietly(InputStream closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ex) {
                //ignore
            }
        }
    }

    public static void closeQuietly(Reader closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ex) {
                //ignore
            }
        }
    }
}
