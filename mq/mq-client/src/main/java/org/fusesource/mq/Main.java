/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.mq;

import java.util.Arrays;
import java.util.LinkedList;
import javax.jms.JMSException;
import org.apache.activemq.ActiveMQConnectionFactory;

public class Main {
    final static String loggingLevelProperty = "org.ops4j.pax.logging.DefaultServiceLog.level";
    String action;
    String destination;
    String brokerUrl = ActiveMQConnectionFactory.DEFAULT_BROKER_URL;
    boolean persistent = true;
    int count = 100;
    int sleep = 0;
    int size = 0;
    String clientId;
    String password;
    String user;
    int batchSize;

    public static void main(String[] args) throws Exception {

        if (System.getProperty(loggingLevelProperty) == null) {
            System.setProperty(loggingLevelProperty, "INFO");
        }

        Main main = new Main();

        // Process the arguments
        LinkedList<String> arg1 = new LinkedList<String>(Arrays.asList(args));
        main.action = shift(arg1);
        while (!arg1.isEmpty()) {
            try {
                String arg = arg1.removeFirst();
                if ("--size".equals(arg)) {
                    main.size = Integer.parseInt(shift(arg1));
                } else if ("--count".equals(arg)) {
                    main.count = Integer.parseInt(shift(arg1));
                } else if ("--sleep".equals(arg)) {
                    main.sleep = Integer.parseInt(shift(arg1));
                } else if ("--destination".equals(arg)) {
                    main.destination = shift(arg1);
                } else if ("--brokerUrl".equals(arg)) {
                    main.brokerUrl = shift(arg1);
                } else if ("--user".equals(arg)) {
                    main.user = shift(arg1);
                } else if ("--password".equals(arg)) {
                    main.password = shift(arg1);
                } else if ("--clientId".equals(arg)) {
                    main.clientId = shift(arg1);
                } else if ("--batchSize".equals(arg)) {
                    main.batchSize = Integer.parseInt(shift(arg1));
                } else if ("--persistent".equals(arg)) {
                    main.persistent = Boolean.valueOf(shift(arg1)).booleanValue();
                } else {
                    System.err.println("Invalid usage: unknown option: " + arg);
                    displayHelpAndExit(1);
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid usage: argument not a number");
                displayHelpAndExit(1);
            }
        }

        main.execute();
        System.exit(0);
    }

    private void execute() {
        initDestination();
        System.out.println("Using destination: " + destination + ", on broker: " + brokerUrl);

        ActiveMQService activeMQService = new ActiveMQService(user, password, brokerUrl);
        activeMQService.setTransacted(batchSize > 0);
        try {

            if ("producer".equals(action)) {

                activeMQService.start();

                ProducerThread producerThread = new ProducerThread(activeMQService, destination);
                producerThread.setMessageCount(count);
                producerThread.setMessageSize(size);
                producerThread.setSleep(sleep);
                producerThread.setPersistent(persistent);
                producerThread.setTransactionBatchSize(batchSize);
                producerThread.run();
                System.out.println("Produced: " + producerThread.getSentCount());

            } else if ("consumer".equals(action)) {

                activeMQService.setClientId(clientId);
                activeMQService.start();

                ConsumerThread consumerThread = new ConsumerThread(activeMQService, destination);
                consumerThread.setMessageCount(count);
                consumerThread.setSleep(sleep);
                consumerThread.setTransactionBatchSize(batchSize);

                System.out.println("Waiting for: " + count + " messages");
                consumerThread.run();
                System.out.println("Consumed: " + consumerThread.getReceived() + " messages");

            } else {
                displayHelpAndExit(1);
            }

        } catch (JMSException error) {
            System.err.println("Execution failed with: " + error);
            error.printStackTrace(System.err);
            System.exit(2);
        } finally {
            activeMQService.stop();
        }
    }

    private void initDestination() {
        if (destination == null) {
            if (clientId != null) {
                destination = "topic://TEST";
            } else {
                destination = "queue://TEST";
            }
        }
    }

    private static String shift(LinkedList<String> argl) {
        if (argl.isEmpty()) {
            System.out.println("Invalid usage: Missing argument");
            displayHelpAndExit(1);
        }
        return argl.removeFirst();
    }

    private static void displayHelpAndExit(int exitCode) {
        System.out.println(" usage   : (producer|consumer) [OPTIONS]");
        System.out.println(" options : [--destination (queue://..|topic://..) - ; default TEST");
        System.out.println("           [--persistent  true|false] - use persistent or non persistent messages; default true");
        System.out.println("           [--count       N] - number of messages to send or receive; default 100");
        System.out.println("           [--size        N] - size in bytes of a BytesMessage; default 0, a simple TextMessage is used");
        System.out.println("           [--sleep       N] - millisecond sleep period between sends or receives; default 0");
        System.out.println("           [--batchSize   N] - use send and receive transaction batches of size N; default 0, no jms transactions");
        System.out.println("           [--clientId   id] - use a durable topic consumer with the supplied id; default null, non durable consumer");
        System.out.println("           [--brokerUrl URL] - connection factory url; default " + ActiveMQConnectionFactory.DEFAULT_BROKER_URL);
        System.out.println("           [--user      .. ] - connection user name");
        System.out.println("           [--password  .. ] - connection password");

        System.out.println("");

        System.exit(exitCode);
    }

}
