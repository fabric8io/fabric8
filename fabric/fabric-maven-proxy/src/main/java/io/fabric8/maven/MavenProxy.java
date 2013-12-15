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
package io.fabric8.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import io.fabric8.maven.impl.InvalidMavenArtifactRequest;

public interface MavenProxy {

    final String UPLOAD_TYPE = "upload";
    final String DOWNLOAD_TYPE = "download";

    void start() throws IOException;

    void stop();

    /**
     * Downloads a {@link File} from the {@link MavenProxy}.
     * @param path The path from which to download the {@link File}.
     * @return
     */
    File download(String path) throws InvalidMavenArtifactRequest;

    /**
     * Upload a {@link File} to the {@link MavenProxy}.
     * @param is The {@link InputStream} to upload.
     * @param path The upload path.
     * @return true/false based on the outcome of the upload.
     */
    boolean upload(InputStream is, String path) throws InvalidMavenArtifactRequest;
}
