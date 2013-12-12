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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;

public class DirectoryContent implements Content {

    final File directory;
    final URI root;
    List<String> entries;

    public DirectoryContent(String directory) {
        this.directory = new File(directory);
        this.root = this.directory.toURI();
    }

    public Iterable<String> getEntries() {
        return new Iterable<String>() {
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    LinkedList<File> files = new LinkedList<File>(Collections.singletonList(directory));
                    public boolean hasNext() {
                        return !files.isEmpty();
                    }
                    public String next() {
                        File file = files.removeFirst();
                        if (file.isDirectory()) {
                            File[] children = file.listFiles();
                            if (children != null) {
                                for (File f : children) {
                                    files.add(f);
                                }
                            }
                        }
                        return root.relativize(file.toURI()).toString();
                    }
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    public URL getURL(String entry) throws MalformedURLException {
        File f = new File(directory, entry);
        if (f.exists()) {
            return f.toURI().toURL();
        } else {
            return null;
        }
    }

    public void close() {
    }

    @Override
    public String toString() {
        return "DirectoryContent[" + directory + ']';
    }
}
