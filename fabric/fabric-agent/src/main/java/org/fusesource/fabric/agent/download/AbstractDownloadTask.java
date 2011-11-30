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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDownloadTask extends DefaultFuture<DownloadFuture> implements DownloadFuture, Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDownloadTask.class);

    protected final String url;

    protected final ScheduledExecutorService executor;

    private long scheduleDelay = 250;
    private int scheduleNbRun = 0;

    public AbstractDownloadTask(String url, ScheduledExecutorService executor) {
        super(null);
        this.url = url;
        this.executor = executor;
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
        try {
            try {
                File file = download();
                setFile(file);
                return;
            } catch (IOException e) {
                if (++scheduleNbRun < 5) {
                    LOGGER.debug("Error downloading " + url + ": " + e.getMessage() + ". Retrying in approx " + scheduleDelay * 2 + " ms.");
                    long delay = (long)(scheduleDelay * 3 / 2 + Math.random() * scheduleDelay / 2);
                    executor.schedule(this, delay, TimeUnit.MILLISECONDS);
                    scheduleDelay *= 2;
                } else {
                    setException(initIOException("Error downloading " + url, e));
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
     *
     * @param inputStream
     * @param outputStream
     * @throws IOException
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
