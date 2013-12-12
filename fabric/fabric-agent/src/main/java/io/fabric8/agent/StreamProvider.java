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
package io.fabric8.agent;

import io.fabric8.fab.osgi.FabBundleInfo;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 */
public interface StreamProvider {

    InputStream open() throws IOException;

    public static class File implements StreamProvider {
        private final java.io.File file;

        public File(java.io.File file) {
            this.file = file;
        }

        @Override
        public InputStream open() throws IOException {
            return new FileInputStream(file);
        }
    }

    public static class Fab implements StreamProvider {
        private final FabBundleInfo fab;

        public Fab(FabBundleInfo fab) {
            this.fab = fab;
        }

        @Override
        public InputStream open() throws IOException {
            try {
                return fab.getInputStream();
            } catch (Exception e) {
                throw new IOException("Unable to create input stream for fab", e);
            }
        }
    }
}
