/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 */
package io.fabric8.stream.log;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static io.fabric8.stream.log.Support.*;

/**
 * <p>
 *     This class is used to simulate the logging load
 *     that an HTTP server would feed into a LogStreamProducer.
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class HttpSimulator {

    // To get '31/Jan/2012:16:21:03 +0000'
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("dd/MMM/yyyy:HH:mm::ss Z");
    private static final char[] SESSION_DATA_CHARS = new char[] {
        'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
        'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
        '0','1','2','3','4','5','6','7','8','9',
        ' ','=','-',';','.','{','}','[',']'
    };

    private static void displayHelpAndExit(int exitCode) {
        displayResourceFile("http-simulator-usage.txt");
        System.exit(exitCode);
    }


    private static String shift(LinkedList<String> argl) {
        if(argl.isEmpty()) {
            System.err.println("Invalid usage: Missing argument");
            displayHelpAndExit(1);
        }
        return argl.removeFirst();
    }

    public static void main(String[] args) {

        HttpSimulator simulator = new HttpSimulator();

        // Process the arguments
        LinkedList<String> argl = new LinkedList<String>(Arrays.asList(args));
        while(!argl.isEmpty()) {
            try {
                String arg = argl.removeFirst();
                if( "--help".equals(arg) ) {
                    displayHelpAndExit(0);
                } else if( "--producers".equals(arg) ) {
                    simulator.producers = Integer.parseInt(shift(argl));
                } else if( "--broker".equals(arg) ) {
                    simulator.brokers.add(shift(argl));
                } else if( "--destination".equals(arg) ) {
                    simulator.destinations.add(shift(argl));
                } else if( "--batch-size".equals(arg) ) {
                    simulator.batchSize = Integer.parseInt(shift(argl));
                } else if( "--batch-timeout".equals(arg) ) {
                    simulator.batchTimeout =  Long.parseLong(shift(argl));
                } else if( "--compress".equals(arg) ) {
                    simulator.compress = Boolean.parseBoolean(shift(argl));
                } else if( "--entries-per-sec".equals(arg) ) {
                    simulator.entriesPerSec = Double.parseDouble(shift(argl));
                } else if( "--entries-per-sec-sd".equals(arg) ) {
                    simulator.entriesPerSecSD = Double.parseDouble(shift(argl));
                } else if( "--session-size".equals(arg) ) {
                    simulator.sessionSize = Long.parseLong(shift(argl));
                } else if( "--session-size-sd".equals(arg) ) {
                    simulator.sessionSizeSD = Long.parseLong(shift(argl));
                } else if( "--sample-time".equals(arg) ) {
                    simulator.sampleTime = Integer.parseInt(shift(argl));
                } else if( "--warmup-time".equals(arg) ) {
                    simulator.warmupTime = Integer.parseInt(shift(argl));
                } else {
                    System.err.println("Invalid usage: unknown option: "+arg);
                    displayHelpAndExit(1);
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid usage: argument not a number");
                displayHelpAndExit(1);
            }
        }
        if( simulator.brokers.isEmpty() ) {
            System.err.println("At least one --broker option is required.");
            displayHelpAndExit(1);
        }
        if( simulator.destinations.isEmpty() ) {
            System.err.println("At least one --destination option is required.");
            displayHelpAndExit(1);
        }
        try {
            simulator.execute();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    
    int producers = 1;
    private ArrayList<String> brokers = new ArrayList<String>();
    private ArrayList<String> destinations = new ArrayList<String>();
    private int batchSize = 1024*64;
    private long batchTimeout = 1000*5;
    private boolean compress = true;
    private double entriesPerSec = 10;
    private double entriesPerSecSD = 0;
    private long sessionSize = 512;
    private long sessionSizeSD = 0;
    int warmupTime = 5;
    int sampleTime = 60;

    private AtomicBoolean done = new AtomicBoolean(false);

    private class ProducerContext implements Runnable {
        
        private final int id;
        private final CountDownLatch startedLatch;
        private Thread thread;
        private PrintStream out;
        private Random random;
        AtomicLong counter = new AtomicLong();

        volatile

        double randomEntriesPerSec;
        double feedingsPerMs;
        double msPerFeedings;

        public ProducerContext(int id, CountDownLatch started) {
            this.id = id;
            startedLatch = started;
            this.random = new Random(id);

            // Assign a random feeding rate (within the allowed SD) to this producer
            changeRate();
        }

        synchronized private void changeRate() {
            randomEntriesPerSec = entriesPerSecSD ==0 ? entriesPerSec : Math.max(0, (random.nextGaussian() * entriesPerSecSD) + entriesPerSec);
            feedingsPerMs = randomEntriesPerSec / SECONDS.toMillis(1);
            msPerFeedings = 1.0 / feedingsPerMs;
        }

        long zeroDelays = 0;
        synchronized private long nextFeedDelay() {
            if( zeroDelays > 0 ) {
                // This is the case where we are doing multiple feedings per ms. We want to not delay
                // until the all the feedings in this 1 ms interval are complete.
                zeroDelays -= 1;
                if( zeroDelays == 0 ) {
                    return 1; // We are now done so delay 1 ms
                } else {
                    return 0; // not yet done with the feedings
                }
            }
            if( feedingsPerMs > 1 ) {
                zeroDelays = (long) feedingsPerMs;
                return nextFeedDelay();
            } else {
                return (long) msPerFeedings;
            }
        }

        public void start() throws Exception {
            // do all the work async so we can run lots of these concurrently.
            thread = new Thread(this, "Simulator #: "+id);
            thread.start();
        }

        public void run() {
            LogStreamer streamer=null;
            try {

                // we will be feeding the camel route via this pipe..
                PipedInputStream in = new PipedInputStream();
                out = new PrintStream(new PipedOutputStream(in));

                // Configure the camel route..
                Producer p = new Producer();
                p.setBatchSize(batchSize);
                p.setBatchTimeout(batchTimeout);
                p.setCompress(compress);
                String broker = brokers.get(id % brokers.size());
                p.setBroker(broker);
                String destination = destinations.get(id % destinations.size());
                p.setDestination(destination);
                p.setIs(in);

                streamer = p.configure();
                streamer.start();

                System.out.println(format("Started HTTP log event simulator #"+id+" generating %,.2f events/sec", randomEntriesPerSec));
                startedLatch.countDown();

                // Lets feed the camel based on schedule.
                long nextMealTime = System.currentTimeMillis() + nextFeedDelay();
                while(!done.get()) {

                    long now = System.currentTimeMillis(); 
                    if( now >= nextMealTime ) {
                        feedTheCamel(now);
                        nextMealTime = now + nextFeedDelay();
                    }
                    
                    long remaining = nextMealTime - now;
                    sleep(remaining); 
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    streamer.stop();
                } catch (Throwable e) {
                }
            }
        }


        private void feedTheCamel(long now) {

            String ip = "79.132.121.18";
            String date = DATE_FORMATTER.format(now);
            String resource = "/index.html";
            String referer = "http://fusesource.com/index.html";
            String userAgent = "Mozilla/5.0 (X11; Linux i686; rv:7.0.1) Gecko/20100101 Firefox/7.0.1";

            double randomSize = sessionSizeSD==0 ? sessionSize : Math.floor((random.nextGaussian() * sessionSizeSD) + sessionSize);
            // yeah have a 4 meg max session size so we don't blow up the client in an extreme random case.
            int size = Math.min((int) randomSize, 1024 * 1024 * 4);
            char data[] = new char[size];
            for( int i=0 ;i< size; i++) {
                data[i] = SESSION_DATA_CHARS[random.nextInt(SESSION_DATA_CHARS.length)];
            }
            String session = new String(data);

            out.println(String.format(
                    "%s - - [%s] \"GET %s HTTP/1.1\" 200 1070 \"%s\" \"%s\" \"session=%s\"",
                    ip, date, resource, referer, userAgent, session));
            out.flush();
            counter.incrementAndGet();
        }
    }

    private void sleep(long value) throws InterruptedException {
        long now = System.currentTimeMillis();
        long end = now + value;
        long remaining = end - now;

        while( remaining > 0 && !done.get()) {
            // we don't want to sleep for more than 1 second so we can check the done flag.
            Thread.sleep(Math.min(remaining, 1000));
            now = System.currentTimeMillis();
            remaining = end - now;
        }
    }

    private void execute() throws Exception {

        // To help us wake up on time to take the samples.
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        ArrayList<ProducerContext> contexts = new ArrayList<ProducerContext>(producers);
        CountDownLatch started = new CountDownLatch(producers);
        for (int i=0; i < producers; i++) {
            ProducerContext ctx = new ProducerContext(i, started);
            contexts.add(ctx);
            ctx.start();
        }

        started.await();
        // Give producers a chance to establish a steady state..
        System.out.println(format("Warming up for %,d seconds before sampling", warmupTime));
        sleep(warmupTime * 1000);

        for (int i=0; i < producers; i++) {
            contexts.get(i).counter.set(0);
        }

        // Now start sampling..
        while(!done.get()) {
            // Wait for work to be done..
            System.out.println(format("Sampling for %,d seconds...", sampleTime));
            
            sleep(sampleTime * 1000);

            // Gather the stats..
            DescriptiveStatistics samples = new DescriptiveStatistics();
            samples.setWindowSize(producers);
            for (int i=0; i < producers; i++) {
                ProducerContext producer = contexts.get(i);
                samples.addValue(producer.counter.getAndSet(0));
                producer.changeRate(); // Lets Assign a new random rate..
            }

            // Report 'em
            System.out.println(Arrays.toString(samples.getValues()));
            double scale = 1.0 / sampleTime;
            System.out.println(format("avg producer rate=%,.2f events/sec, sd=%,.2f, total messages produced in the last sample: %.0f",
                    samples.getMean() * scale,
                    samples.getStandardDeviation() * scale,
                    samples.getSum()));
        }

    }


    public void stop() {
        done.set(true);
    }

    public void start() {
        new Thread("Monitor thread") {
            @Override
            public void run() {
                try {
                    execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    //////////////////////////////////////////////////////////////////
    // These setter methods are used to configure this instance by the 
    // HttpSimulatorFactory
    //////////////////////////////////////////////////////////////////
    
    public void setBrokers(String brokers) {
        this.brokers = new ArrayList<String>(Arrays.asList(brokers.split("\\|")));
    }

    public void setDestinations(String destinations) {
        this.destinations = new ArrayList<String>(Arrays.asList(destinations.split("\\|")));
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setBatchTimeout(long batchTimeout) {
        this.batchTimeout = batchTimeout;
    }

    public void setCompress(boolean compress) {
        this.compress = compress;
    }

    public void setEntriesPerSec(double entriesPerSec) {
        this.entriesPerSec = entriesPerSec;
    }

    public void setEntriesPerSecSD(double entriesPerSecSD) {
        this.entriesPerSecSD = entriesPerSecSD;
    }

    public void setProducers(int producers) {
        this.producers = producers;
    }

    public void setSampleTime(int sampleTime) {
        this.sampleTime = sampleTime;
    }

    public void setSessionSize(long sessionSize) {
        this.sessionSize = sessionSize;
    }

    public void setSessionSizeSD(long sessionSizeSD) {
        this.sessionSizeSD = sessionSizeSD;
    }

    public void setWarmupTime(int warmupTime) {
        this.warmupTime = warmupTime;
    }
}
