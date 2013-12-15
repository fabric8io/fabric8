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
package io.fabric8.fab;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class JarContent implements Content {

    final JarFile jar;

    public JarContent(JarFile jar) {
        this.jar = jar;
    }

    public Iterable<String> getEntries() {
        return new Iterable<String>() {
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    final Enumeration<JarEntry> enumeration = jar.entries();
                    public boolean hasNext() {
                        return enumeration.hasMoreElements();
                    }

                    public String next() {
                        return enumeration.nextElement().getName();
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    public URL getURL(String entry) throws MalformedURLException {
        ZipEntry je = jar.getEntry(entry);
        if (je != null) {
            return new URL("jar:file:" + jar.getName() + "!/" + entry);
        } else {
            return null;
        }
    }

    public void close() {
        try {
            jar.close();
        } catch (IOException e) {
            // Ignore
        }
    }

    @Override
    public String toString() {
        return "JarContent[" + jar.getName() + ']';
    }
}
