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
package io.fabric8.agent.download;

import java.io.File;
import java.io.IOException;

public interface DownloadFuture extends Future<DownloadFuture> {

    /**
     * Returns the original url
     *
     * @return the original url
     */
    String getUrl();

    /**
     * Returns the file that has been downloaded on the local file system
     *
     * @return the downloaded file or <code>null</code> is the operation has
     *         not completed yet or failed
     */
    File getFile() throws IOException;

    /**
     * Returns {@code true} if the download operation has been canceled by
     * {@link #cancel()} method.
     */
    boolean isCanceled();

    /**
     * Cancels the authentication attempt and notifies all threads waiting for
     * this future.
     */
    void cancel();

}
