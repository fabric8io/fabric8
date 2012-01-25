/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.amqp.example.simple;

import org.fusesource.fabric.apollo.amqp.api.AmqpConnectionFactory;
import org.fusesource.fabric.apollo.amqp.api.Connection;
import org.fusesource.fabric.apollo.amqp.codec.AmqpDefinitions;

import java.util.concurrent.CountDownLatch;

/**
 *
 */
public abstract class Client {

    protected String transport = "tcp";
    protected int port = AmqpDefinitions.PORT;
    protected String hostname = "localhost";

    protected int count = 1;
    protected boolean settled = true;

    protected long batch_size = 10;

    protected String address = "queue:test";
    protected String message_prefix = "message number ";

    protected CountDownLatch exit_latch = new CountDownLatch(1);

    public Client(String ... args) {
        parseArgs(args);
    }

    public void parseArgs(String ... args) {
        for(String arg : args) {
            if (arg.startsWith("--port")) {
                port = Integer.parseInt(arg.split("=")[1]);
            } else if (arg.startsWith("--hostname")) {
                hostname = arg.split("=")[1];
            } else if (arg.startsWith("--count")) {
                count = Integer.parseInt(arg.split("=")[1]);
            } else if (arg.startsWith("--settled")) {
                settled = Boolean.parseBoolean(arg.split("=")[1]);
            } else if (arg.startsWith("--address")) {
                address = arg.split("=")[1];
            } else if (arg.startsWith("--message_prefix")) {
                message_prefix = arg.split("=")[1];
            } else if (arg.startsWith("--batch_size")) {
                batch_size = Long.parseLong(arg.split("=")[1]);
            } else if (arg.startsWith("--help")) {
                usage();
            }
        }
    }

    public String getConnectionURI() {
        return String.format("%s://%s:%s", transport, hostname, port);
    }

    public void go() {
        final Connection connection = AmqpConnectionFactory.create();
        connection.setOnClose(new Runnable() {
            public void run() {
                exit_latch.countDown();
            }
        });
        connection.connect(getConnectionURI(), new Runnable() {
            public void run() {
                onConnect(connection);
            }
        });
        try {
            exit_latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void println(String formatStr, Object ... args) {
        System.out.println(String.format(formatStr, args));
    }

    public abstract void printHelp();

    public void usage() {
        printHelp();
        System.exit(0);
    }

    public abstract void onConnect(Connection connection);
}
