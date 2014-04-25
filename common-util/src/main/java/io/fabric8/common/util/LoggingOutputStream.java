package io.fabric8.common.util;

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
