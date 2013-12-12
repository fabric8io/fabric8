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
package io.fabric8.stream.log;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQMessageProducer;
import org.apache.activemq.AsyncCallback;
import org.apache.activemq.command.ActiveMQDestination;

import javax.jms.*;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

import static io.fabric8.stream.log.Support.compress;
import static io.fabric8.stream.log.Support.displayResourceFile;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class Producer {
    
    private String logFilePattern = null;
    private File positionFile = null;
    private String broker;
    private String destination;
    private int batchSize = 1024*64;
    private long batchTimeout = 1000*5;
    private boolean compress = true;
    private InputStream is = null;

    public static void main(String[] args) throws Exception {
        Producer producer = new Producer();
        
        // Process the arguments
        LinkedList<String> argl = new LinkedList<String>(Arrays.asList(args));
        while(!argl.isEmpty()) {
            try {
                String arg = argl.removeFirst();
                if( "--help".equals(arg) ) {
                    displayHelpAndExit(0);
                } else if( "--broker".equals(arg) ) {
                    producer.broker = shift(argl);
                } else if( "--destination".equals(arg) ) {
                    producer.destination = shift(argl);
                } else if( "--batch-size".equals(arg) ) {
                    producer.batchSize = Integer.parseInt(shift(argl));
                } else if( "--batch-timeout".equals(arg) ) {
                    producer.batchTimeout =  Long.parseLong(shift(argl));
                } else if( "--compress".equals(arg) ) {
                    producer.compress = Boolean.parseBoolean(shift(argl));
                } else if( "--log-file".equals(arg) ) {
                    producer.logFilePattern = shift(argl);
                } else if( "--position-file".equals(arg) ) {
                    producer.positionFile = new File(shift(argl));
                } else {
                    System.err.println("Invalid usage: unknown option: "+arg);
                    displayHelpAndExit(1);
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid usage: argument not a number");
                displayHelpAndExit(1);
            }
        }

        if( producer.logFilePattern!=null ^ producer.positionFile!=null ) {
            System.err.println("Invalid usage: --log-file and --position-file but both be set.");
            displayHelpAndExit(1);
        }
        if( producer.broker==null ) {
            System.err.println("Invalid usage: --broker option not specified.");
            displayHelpAndExit(1);
        }
        if( producer.destination==null ) {
            System.err.println("Invalid usage: --destination option not specified.");
            displayHelpAndExit(1);
        }

        producer.execute();
        System.exit(0);
    }

    private static String shift(LinkedList<String> argl) {
        if(argl.isEmpty()) {
            System.err.println("Invalid usage: Missing argument");
            displayHelpAndExit(1);
        }
        return argl.removeFirst();
    }

    private static void displayHelpAndExit(int exitCode) {
        displayResourceFile("producer-usage.txt");
        System.exit(exitCode);
    }

    private void execute() throws Exception {
        LogStreamer source = configure();
        source.start();

        // block until the process is killed.
        synchronized (this) {
            while(true) {
                this.wait();
            }
        }
    }

    public LogStreamer configure() throws Exception {

        Processor processor = new Processor() {
            Connection connection;
            Session session;
            ActiveMQMessageProducer producer;

            @Override
            public void start() throws JMSException {
                ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(broker);
                connection = factory.createConnection();
                connection.start();
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                producer = (ActiveMQMessageProducer) session.createProducer(ActiveMQDestination.createDestination(destination, ActiveMQDestination.QUEUE_TYPE));
                if(positionFile!=null) {
                    producer.setDeliveryMode(DeliveryMode.PERSISTENT);
                } else {
                    producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
                }
            }

            @Override
            public void stop() {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            connection.stop();
                        } catch (JMSException e) {
                        }
                    }
                }.start();
            }

            @Override
            public void send(HashMap<String, String> headers, byte[] data, final Callback onComplete) {
                try {
                    BytesMessage msg = session.createBytesMessage();
                    producer.send(msg, new AsyncCallback(){
                        public void onSuccess() {
                            onComplete.onSuccess();
                        }
                        public void onException(JMSException exception) {
                            onComplete.onFailure(exception);
                        }
                    });
                } catch (JMSException e) {
                    onComplete.onFailure(e);
                }
            }
        };

        if(compress) {
            final Processor next = processor;
            processor = new Processor() {
                @Override
                public void start() throws Exception {
                    next.start();
                }

                @Override
                public void stop() {
                    next.stop();
                }

                @Override
                public void send(HashMap<String, String> headers, byte[] data, Callback onComplete) {
                    next.send(headers, compress(data), onComplete);
                }
            };
        }

        LogStreamer streamer = new LogStreamer();
        streamer.setBatchSize(batchSize);
        streamer.setBatchTimeout(batchTimeout);
        streamer.setIs(is);
        streamer.setLogFilePattern(logFilePattern);
        streamer.setPositionFile(positionFile);
        if( positionFile==null ) {
            streamer.setExitOnEOF(true);
        }
        streamer.setProcessor(processor);
        return streamer;
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

    public String getBroker() {
        return broker;
    }

    public void setBroker(String broker) {
        this.broker = broker;
    }

    public boolean isCompress() {
        return compress;
    }

    public void setCompress(boolean compress) {
        this.compress = compress;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public InputStream getIs() {
        return is;
    }

    public void setIs(InputStream is) {
        this.is = is;
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
}
