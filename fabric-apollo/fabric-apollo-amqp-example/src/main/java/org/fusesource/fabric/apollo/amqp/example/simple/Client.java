/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.example.simple;

import org.fusesource.fabric.apollo.amqp.codec.AmqpDefinitions;

import java.util.concurrent.CountDownLatch;

/**
 *
 */
public abstract class Client {

    protected int port = AmqpDefinitions.PORT;
    protected String hostname = "localhost";

    protected int count = 1;
    protected boolean settled = true;

    protected String address = "queue:test";
    protected String message_prefix = "message number ";

    protected CountDownLatch exit_latch = new CountDownLatch(1);

    public Client(String ... args) {
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
            } else if (arg.startsWith("--help")) {
                usage();
            }
        }
    }

    public abstract void go();
    public abstract void printHelp();

    public void usage() {
        printHelp();
        System.exit(0);
    }
}
