/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.agent.download;

import java.io.File;
import java.io.IOException;

public interface DownloadFuture extends Future<DownloadFuture> {

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
