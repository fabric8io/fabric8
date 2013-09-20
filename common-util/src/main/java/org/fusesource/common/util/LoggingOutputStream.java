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
package org.fusesource.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * A {@link java.io.OutputStream} which logs to a {@link Logger}
 */
public class LoggingOutputStream extends OutputStream {

    private static final transient Logger LOG = LoggerFactory.getLogger(LoggingOutputStream.class);

    private final Logger logger;
    private int bufferSize = 4 * 1024;
    private ByteArrayOutputStream buffer = createBuffer();

    private boolean closed = false;

    public LoggingOutputStream() {
        this(LOG);
    }

    public LoggingOutputStream(Logger logger) {
        this.logger = logger;
    }

    public void close() {
        flush();
        closed = true;
    }

    public void flush() {
        byte[] bytes = buffer.toByteArray();
        if (bytes.length > 0) {
            String text = new String(bytes);
            logMessage(text);
            buffer = createBuffer();
        }
    }

    public void write(final int b) throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }
        char ch = (char) b;
        if (ch == '\n') {
            flush();
        } else {
            buffer.write(b);
        }
    }

    protected void logMessage(String text) {
        if (text.length() > 0) {
            logger.info(text);
        }
    }

    protected ByteArrayOutputStream createBuffer() {
        return new ByteArrayOutputStream(bufferSize);
    }
}