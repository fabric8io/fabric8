/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.api;

/**
 * A simple enum representing the kinds of instances to be created via jClouds
 */
public enum JCloudsInstanceType {
    Smallest, Biggest, Fastest;

    /**
     * Returns the JCloudsInstanceType value for the given text value if it exists otherwise returns the defaultValue
     */
    public static JCloudsInstanceType get(String text, JCloudsInstanceType defaultValue) {
        JCloudsInstanceType answer = null;
        if (text != null) {
            answer = JCloudsInstanceType.valueOf(text);
        }
        if (answer == null) {
            answer = defaultValue;
        }
        return answer;
    }
}
