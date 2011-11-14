/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.example.dosgi.impl;


import org.fusesource.fabric.example.dosgi.Service;

public class ServiceImpl implements Service {

    @Override
    public String messageFrom(String input) {
        return "Message from distributed service to : " + input;
    }
}
