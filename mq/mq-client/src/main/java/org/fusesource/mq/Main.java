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
    String destination = "queue://TEST";
    String brokerUrl = ActiveMQConnectionFactory.DEFAULT_BROKER_URL;
    int count = 100;

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
                if ("--count".equals(arg)) {
                    main.count = Integer.parseInt(shift(arg1));
                } else if ("--destination".equals(arg)) {
                    main.destination = shift(arg1);
                } else if ("--brokerUrl".equals(arg)) {
                    main.brokerUrl = shift(arg1);
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
        ActiveMQService activeMQService = new ActiveMQService(brokerUrl);
        System.out.println("Using destination: " + destination + ", on broker: " + brokerUrl);
        try {

            if ("producer".equals(action)) {

                activeMQService.start();

                ProducerThread producerThread = new ProducerThread(activeMQService, destination);
                producerThread.setMessageCount(count);
                producerThread.run();
                System.out.println("Produced: " + producerThread.getSentCount());

            } else if ("consumer".equals(action)) {

                activeMQService.start();

                ConsumerThread consumerThread = new ConsumerThread(activeMQService, destination);
                consumerThread.setMessageCount(count);
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

    private static String shift(LinkedList<String> argl) {
        if (argl.isEmpty()) {
            System.err.println("Invalid usage: Missing argument");
            displayHelpAndExit(1);
        }
        return argl.removeFirst();
    }

    private static void displayHelpAndExit(int exitCode) {
        System.out.println(" usage   : (producer|consumer) [OPTIONS]");
        System.out.println(" options : [--destination (queue://..|topic://..)");
        System.out.println("           [--count N]");
        System.out.println("           [--brokerUrl URL]\n");

        System.exit(exitCode);
    }

}
