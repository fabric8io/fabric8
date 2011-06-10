/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.generator;

/**
 *
 */
public class Utilities {

    public static String sanitize(String name) {
        String rc = name.replace('-', '_');

        return rc;
    }

    public static String toStaticName(String name) {
        return sanitize(name.toUpperCase());
    }

    public static String toJavaClassName(String name) {
        String rc = "";
        String strs[] = sanitize(name).split("_");
        for (String str : strs) {
            rc += str.substring(0, 1).toUpperCase();
            rc += str.substring(1);
        }
        return rc;
    }
}
