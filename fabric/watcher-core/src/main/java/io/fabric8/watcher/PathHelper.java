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
package io.fabric8.watcher;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

/**
 * A set of helper methods for working with {@link Path) instances
 */
public class PathHelper {
    /**
     * Converts the given array of files into an array of paths
     */
    public static Path[] toPathArray(File... files) {
        Path[] paths = new Path[files.length];
        int idx = 0;
        for (File file : files) {
            paths[idx++] = file.toPath();
        }
        return paths;
    }

    /**
     * Converts the given Path to a URL string
     */
    public static String toUrlString(Path path) throws MalformedURLException {
        return toURL(path).toString();
    }

    /**
     * Converts the given Path to a URL
     */
    public static URL toURL(Path path) throws MalformedURLException {
        return path.toUri().toURL();
    }
}
