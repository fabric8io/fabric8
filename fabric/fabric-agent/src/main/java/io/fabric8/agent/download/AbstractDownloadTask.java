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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDownloadTask extends DefaultFuture<DownloadFuture> implements DownloadFuture, Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDownloadTask.class);

    protected final String url;
    protected final ExecutorService executor;
    private long scheduleDelay = 250;
    private int scheduleNbRun = 0;

    public AbstractDownloadTask(String url, ExecutorService executor) {
        super(null);
        this.url = url;
        this.executor = executor;
    }

    public String getUrl() {
        return url;
    }

    public File getFile() throws IOException {
        Object v = getValue();
        if (v instanceof File) {
            return (File) v;
        } else if (v instanceof IOException) {
            throw (IOException) v;
        } else {
            return null;
        }
    }

    public void setFile(File file) {
        if (file == null) {
            throw new NullPointerException("file");
        }
        setValue(file);
    }

    public void setException(IOException exception) {
        if (exception == null) {
            throw new NullPointerException("exception");
        }
        setValue(exception);
    }

    public final void run() {
        boolean done = false;
        try {
            while (!done) {
                try {
                    File file = download();
                    setFile(file);
                    done = true;
                    return;
                } catch (IOException e) {
                    if (++scheduleNbRun < 5) {
                        long delay = (long)(scheduleDelay * 3 / 2 + Math.random() * scheduleDelay / 2);
                        LOGGER.debug("Error downloading " + url + ": " + e.getMessage() + ". Retrying in approx " + delay + " ms.");
                        Thread.sleep(delay);
                        scheduleDelay *= 2;
                    } else {
                        setException(initIOException("Error downloading " + url, e));
                        done = true;
                    }
                }
            }
        } catch (Throwable e) {
            setException(initIOException("Error downloading " + url, e));
        }
    }

    protected abstract File download() throws Exception;

    /**
     * Creates an IOException with a message and a cause.
     *
     * @param message exception message
     * @param cause   exception cause
     * @return the created IO Exception
     */
    static IOException initIOException(final String message, final Throwable cause) {
        IOException exception = new IOException(message);
        exception.initCause(cause);
        return exception;
    }

    /**
     * Copy the input stream to the output
     */
    static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        try {
            byte[] buffer = new byte[8192];
            int len;
            for (; ;) {
                len = inputStream.read(buffer);
                if (len > 0) {
                    outputStream.write(buffer, 0, len);
                } else {
                    break;
                }
            }
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
            }
            try {
                outputStream.close();
            } catch (IOException e) {
            }
        }
    }

}
