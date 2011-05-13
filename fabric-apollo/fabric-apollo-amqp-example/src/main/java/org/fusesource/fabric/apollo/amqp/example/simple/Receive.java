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

/**
 *
 */
public class Receive extends Client {

    public static void main(String ... args) {
        new Receive(args).go();
    }

    public Receive(String ... args) {
        super(args);
    }

    @Override
    public void go() {

    }

    @Override
    public void printHelp() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
