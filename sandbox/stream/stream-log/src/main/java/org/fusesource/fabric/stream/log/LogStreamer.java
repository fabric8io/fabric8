/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 */
package io.fabric8.stream.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static io.fabric8.stream.log.Support.*;

/**
* <p>
* </p>
*
* @author <a href="http://hiramchirino.com">Hiram Chirino</a>
*/
class LogStreamer {

    private static final transient Logger LOG = LoggerFactory.getLogger(LogStreamer.class);

    static final Object EOF = new Object();
    static class QueueEntry {
        private final byte[] data;
        private final long file;
        private final long offset;
        private final int size;

        QueueEntry(byte data[], long file, long offset, int size) {
            this.data = data;
            this.file = file;
            this.offset = offset;
            this.size = size;
        }
    }

    final ExecutorService inputReader = Executors.newSingleThreadExecutor();
    final ExecutorService batchReader = Executors.newSingleThreadExecutor();
    final private AtomicBoolean runAllowed = new AtomicBoolean(false);
    final ArrayBlockingQueue<Object> queue = new ArrayBlockingQueue<Object>(1024);


    public String logFilePattern = null;
    public File positionFile = null;
    public int batchSize = 1024*256;
    public long batchTimeout = 1000;
    public long tailRetry = 500;
    public InputStream is;
    public boolean exitOnEOF;
    public Processor processor;

    private void updateLogPosition(long file, long offset) {
        if( positionFile !=null ) {
            try {
                writeText(positionFile, String.format("%d:%d\n", file, offset));
            } catch (IOException e) {
                new RuntimeException(e);
            }
        }
    }

    private boolean logFileExists(long file) {
        return new File(String.format(logFilePattern, file)).exists();
    }

    private boolean isRunAllowed() {
        return runAllowed.get();
    }

    public void start() {
        if(runAllowed.compareAndSet(false, true)) {
            try {
                processor.start();
            } catch (Exception e) {
                runAllowed.set(false);
                return;
            }

            //
            // Start a thread which reads stdin and passes the data in big byte[] chunks
            // aligned at \n boundaries to an ArrayBlockingQueue
            //
            inputReader.execute(new Runnable() {
                @Override
                public void run() {
                    readInput();
                }
            });

            // In another thread
            batchReader.execute(new Runnable() {
                @Override
                public void run() {
                    drainBatchQueue();
                }
            });
        }
    }

    public void stop() {
        if(runAllowed.compareAndSet(true, false)) {
            inputReader.shutdown();
            batchReader.shutdown();
            processor.stop();
        }
        runAllowed.set(false);
    }

    private void readInput() {
        if( logFilePattern!=null ) {
            long currentFile = -1;
            long currentOffset = -1;

            try {
                if( !positionFile.exists() ) {
                    writeText(positionFile, "0:0");
                }
                String data = readText(positionFile).trim();
                String[] split = data.split(":");
                currentFile = Long.parseLong(split[0]);
                currentOffset = Long.parseLong(split[1]);

                while(runAllowed.get()) {
                    File current = new File(String.format(logFilePattern, currentFile));
                    FileInputStream is = new FileInputStream(current);
                    try {
                        process(is, currentFile, currentOffset);
                        currentFile ++;
                        currentOffset = 0;
                    } finally {
                        is.close();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

        } else {
            if(is==null) {
                is = System.in;
            }
            try {
                process(is, 0, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean process(InputStream is, long file, long offset) throws IOException, InterruptedException {

        // Skip to the offset.
        if( offset > 0 ) {
            is.skip(offset);
        }

        int pos = 0;
        byte batch[] = new byte[4*1024];
        boolean eof_possible = false;

        while(isRunAllowed()) {
            int count = is.read(batch, pos, batch.length - pos);
            if( count < 0  ) {
                if( logFilePattern==null || eof_possible ) {
                    if( pos > 0 ) {
                        queue.put(new QueueEntry(Arrays.copyOf(batch, pos), file, offset, pos));
                    }
                    queue.put(EOF);
                    return true;
                } else {
                    // We won't move off the current log file until the next one is created.
                    if( logFileExists(file+1) ) {
                        // To to read 1 more time.. and then switch..
                        eof_possible = true;
                        continue;
                    } else {
                        eof_possible = false;
                        Thread.sleep(tailRetry);
                    }
                }
            } else {
                eof_possible = false;
                pos += count;
                int at = lastnlposition(batch, pos);
                if( at >= 0 ) {
                    int len = at+1;
                    byte[] data = Arrays.copyOf(batch, len);
                    int remaining = pos-len;
                    System.arraycopy(batch, len, batch, 0,  remaining);
                    pos = remaining;
                    queue.put(new QueueEntry(data, file, offset, len));
                }
                if (pos == batch.length) {
                    queue.put(new QueueEntry(batch, file, offset, pos));
                    batch = new byte[batch.length];
                    pos = 0;
                }
            }
            offset += count;
        }
        return false;
    }

    private void drainBatchQueue() {
        while(isRunAllowed()) {
            boolean atEOF = false;
            // loop while we are allowed, or if we are stopping loop until the queue is empty
            while (isRunAllowed() && !atEOF) {
                QueueEntry firstEntry = null;
                QueueEntry lastEntry;
                ByteArrayOutputStream batch = new ByteArrayOutputStream((int) (batchSize*1.5));
                try {
                    Object obj = queue.poll(1000, TimeUnit.MILLISECONDS);
                    if (obj == null) {
                        continue;
                    }

                    // we are done.
                    if(obj == EOF) {
                        atEOF = true;
                        continue;
                    }

                    //starting a new batch..
                    long start = System.currentTimeMillis();
                    long timeout = start + batchTimeout;

                    lastEntry = (QueueEntry)obj;
                    if( firstEntry == null ) {
                        firstEntry = lastEntry;
                    }

                    batch.write(lastEntry.data);

                    // Fill in the rest of the batch up to the batch size or the batch timeout.
                    while(batch.size() < batchSize && !atEOF) {
                        obj = queue.poll();
                        if( obj!=null ) {
                            if(obj == EOF) {
                                atEOF = true;
                            } else {
                                lastEntry = (QueueEntry)obj;
                                batch.write(lastEntry.data);
                            }
                        } else {
                            // gonna have to poll with a timeout..
                            long remaining = timeout - System.currentTimeMillis();
                            if( remaining > 0 ) {
                                obj = queue.poll(remaining, TimeUnit.MILLISECONDS);
                                if( obj!=null ) {
                                    if(obj == EOF) {
                                        atEOF = true;
                                    } else {
                                        lastEntry = (QueueEntry)obj;
                                        batch.write(lastEntry.data);
                                    }
                                    continue;
                                }
                            }
                            // timeout.
                            break;
                        }
                    }

                    if(batch.size() > 0 ) {

                        byte[] body = batch.toByteArray();
                        batch.reset();

                        assert firstEntry.file == lastEntry.file;

                        HashMap<String, String> headers = new HashMap<String, String>();
                        headers.put("at", String.format("%d:%d", firstEntry.file, firstEntry.offset));

                        final QueueEntry entry = lastEntry;
                        send(headers, lastEntry, body, new Runnable() {
                            @Override
                            public void run() {
                                updateLogPosition(entry.file, entry.offset + entry.size);
                            }
                        });

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    stop();
                }
            }

            if( atEOF && isRunAllowed() ) {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("EOF", "true");
                send(headers, null, new byte[0], new Runnable() {
                    @Override
                    public void run() {
                        if( exitOnEOF ) {
                            System.exit(0);
                        }
                    }
                });
            }
        }
    }

    public Semaphore sendSemaphore = new Semaphore(10);
    private void send(HashMap<String, String> headers, final QueueEntry lastEntry, byte[] body, final Runnable onComplete) {

        // We send up to 10 batches before this semaphore blocks.
        // blocks waiting for a response.
        try {
            sendSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        processor.send(headers, body, new Callback(){
            @Override
            public void onSuccess() {
                try {
                    sendSemaphore.release();
                    if( onComplete!=null ) {
                        onComplete.run();
                    }
                } catch (Throwable e) {
                    onFailure(e);
                }
            }

            @Override
            public void onFailure(Throwable e) {
                e.printStackTrace();
                stop();
            }
        });
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getBatchTimeout() {
        return batchTimeout;
    }

    public void setBatchTimeout(long batchTimeout) {
        this.batchTimeout = batchTimeout;
    }

    public InputStream getIs() {
        return is;
    }

    public void setIs(InputStream is) {
        this.is = is;
    }

    public boolean isExitOnEOF() {
        return exitOnEOF;
    }

    public void setExitOnEOF(boolean exitOnEOF) {
        this.exitOnEOF = exitOnEOF;
    }

    public String getLogFilePattern() {
        return logFilePattern;
    }

    public void setLogFilePattern(String logFilePattern) {
        this.logFilePattern = logFilePattern;
    }

    public File getPositionFile() {
        return positionFile;
    }

    public void setPositionFile(File positionFile) {
        this.positionFile = positionFile;
    }

    public Processor getProcessor() {
        return processor;
    }

    public void setProcessor(Processor processor) {
        this.processor = processor;
    }

    public long getTailRetry() {
        return tailRetry;
    }

    public void setTailRetry(long tailRetry) {
        this.tailRetry = tailRetry;
    }
}
