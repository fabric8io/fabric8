/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.stream.log;

import org.apache.camel.*;
import org.apache.camel.impl.DefaultComponent;
import org.apache.camel.impl.DefaultConsumer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.impl.DefaultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class InputBatcher extends DefaultComponent {

    private static final transient Logger LOG = LoggerFactory.getLogger(InputBatcherConsumer.class);

    public int batchSize = 1024*256;
    public long batchTimeout = 1000;

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        return new InputBatcherEndpoint(uri, this);
    }

    class InputBatcherEndpoint extends DefaultEndpoint {

        InputBatcherEndpoint(String endpointUri, Component component) {
            super(endpointUri, component);
        }

        @Override
        public boolean isSingleton() {
            return true;
        }

        @Override
        public Producer createProducer() throws Exception {
            throw new UnsupportedOperationException("Producer not supported!");
        }

        @Override
        public Consumer createConsumer(Processor processor) throws Exception {
            return new InputBatcherConsumer(this, processor);
        }

    }
    
    static final Object EOF = new Object();
    
    class InputBatcherConsumer extends DefaultConsumer {

        final ArrayBlockingQueue<Object> queue = new ArrayBlockingQueue<Object>(1024);

        final ExecutorService inputReader = Executors.newSingleThreadExecutor();
        final ExecutorService batchReader = Executors.newSingleThreadExecutor();
        private AsyncProcessor processor;


        InputBatcherConsumer(Endpoint endpoint, Processor processor) {
            super(endpoint, processor);
        }
        
        @Override
        protected void doStart() throws Exception {
            super.doStart();

            //
            // Start a thread which reads stdin and passes the data in big byte[] chunks
            // aligned at \n boundaries to an ArrayBlockingQueue
            //
            inputReader.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        byte batch[] = new byte[4*1024];
                        int pos = 0;
                        while(isRunAllowed()) {
                            // do not poll if we are suspended
                            if (isSuspending() || isSuspended()) {
                                LOG.trace("Consumer is suspended so skip polling");
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    LOG.debug("Sleep interrupted, are we stopping? {}", isStopping() || isStopped());
                                }
                                continue;
                            }

                            int count = System.in.read(batch, pos, batch.length - pos);
                            if( count < 0  ) {
                                if( pos > 0 ) {
                                    byte[] data = new byte[pos];
                                    System.arraycopy(batch, 0, data, 0, pos);
                                    queue.put(data);
                                }
                                queue.put(EOF);
                                return;
                            } else {
                                pos += count;
                                int at = lastnlposition(batch, pos);
                                if( at >= 0 ) {
                                    int len = at+1;
                                    byte[] data = new byte[len];
                                    System.arraycopy(batch, 0, data, 0, len);
                                    int remaining = pos-len;
                                    System.arraycopy(batch, len, batch, 0,  remaining);
                                    pos = remaining;
//                                    System.err.println("Queued "+len+" bytes");
                                    queue.put(data);
                                } else if (pos == batch.length) {
                                    // no nl found in the 4k read so far.. pass it along.
                                    // only other alternative is to drop it and that's not so good.
//                                    System.err.println("queuing "+pos+" data, but no nl was in it");
                                    queue.put(batch);
                                    batch = new byte[4*1024];
                                    pos = 0;
                                } else {
//                                    System.err.println("at "+pos+" but no nl yet");
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // In another thread
            batchReader.execute(new Runnable() {
                @Override
                public void run() {
                    boolean atEOF = false;
                    // loop while we are allowed, or if we are stopping loop until the queue is empty
                    while (isRunAllowed() && !atEOF) {
                        // do not poll if we are suspended
                        if (isSuspending() || isSuspended()) {
                            LOG.trace("Consumer is suspended so skip polling");
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                LOG.debug("Sleep interrupted, are we stopping? {}", isStopping() || isStopped());
                            }
                            continue;
                        }

                        ByteArrayOutputStream batch = new ByteArrayOutputStream((int) (batchSize*1.5));
                        try {
                            Object obj = queue.poll(1000, TimeUnit.MILLISECONDS);
                            if (obj != null) {
                                
                                // we are done. 
                                if(obj == EOF) {
                                    atEOF = true;
                                    continue;
                                }
                                
                                //starting a new batch..
                                long start = System.currentTimeMillis();
                                long timeout = start + batchTimeout;
                                try {

//                                    System.err.println("batch start, size: "+batch.size());
                                    batch.write((byte[]) obj);

                                    // Fill in the rest of the batch up to the batch size or the batch timeout.
                                    while(batch.size() < batchSize && !atEOF) {
                                        obj = queue.poll();
                                        if( obj!=null ) {
                                            if(obj == EOF) {
                                                atEOF = true;
                                            } else {
//                                                System.err.println("batch add, size: "+batch.size());
                                                batch.write((byte[]) obj);
                                            }
                                        } else {
                                            // gonna have to poll with a timeout..
                                            long remaining = timeout - System.currentTimeMillis();
                                            if( remaining > 0 ) {
//                                                System.err.println("batch waiting "+remaining);
                                                obj = queue.poll(remaining, TimeUnit.MILLISECONDS);
                                                if( obj!=null ) {
                                                    if(obj == EOF) {
                                                        atEOF = true;
                                                    } else {
                                                        batch.write((byte[]) obj);
                                                    }
                                                    continue;
                                                }
                                                // else timeout..
                                            }
                                            // timeout.
//                                            System.err.println("batch poll timeout");
                                            break;
                                        }
                                    }

                                    if(batch.size() > 0 ) {

                                        byte[] body = batch.toByteArray();
                                        batch.reset();

                                        Exchange exchange = getEndpoint().createExchange();
                                        Message msg = new DefaultMessage();
                                        msg.setBody(body);
                                        exchange.setIn(msg);

                                        try {
//                                            System.err.println("sending: "+body.length);
                                            getProcessor().process(exchange);
//                                            System.err.println("sent");

                                            if (exchange.getException() != null) {
                                                getExceptionHandler().handleException("Error processing exchange", exchange, exchange.getException());
                                            }
                                        } catch (Exception e) {
                                            getExceptionHandler().handleException("Error processing exchange", exchange, e);
                                        }
                                    }

                                } catch (Exception e) {
                                    LOG.info("Error processing exchange.", e);
                                }
                            }
                        } catch (InterruptedException e) {
                            LOG.debug("Sleep interrupted, are we stopping? {}", isStopping() || isStopped());
                            continue;
                        }
                    }
                    
                    if( atEOF && isRunAllowed() ) {

                        // Send an exchange to signal the end of the stream.
                        Exchange exchange = getEndpoint().createExchange();
                        Message msg = new DefaultMessage();
                        msg.setHeader("EOF", "true");
                        msg.setBody(new byte[0]);
                        exchange.setIn(msg);

                        try {
//                            System.err.println("Sending EOF signal");
                            getProcessor().process(exchange);
                            if (exchange.getException() != null) {
                                getExceptionHandler().handleException("Error processing exchange", exchange, exchange.getException());
                            }
                        } catch (Exception e) {
                            getExceptionHandler().handleException("Error processing exchange", exchange, e);
                        }

                        // we are done processing... lets exit cleanly.
//                        System.err.println("EOF reached shutting down.");
                        System.exit(0);
                    }
                }
            });
        }

        @Override
        protected void doStop() throws Exception {
            inputReader.shutdown();
            batchReader.shutdown();
            super.doStop();
        }

    }

    private static int lastnlposition(byte[] data, int len) {
        // have we received an entire log line yet?
        int at = -1;
        for(int i=len-1; i >= 0; i--) {
            if(data[i] == '\n') {
                at = i;
                break;
            }
        }
        return at;
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
}
