/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.pomegranate.util;

/**
 */
public class Filters {

    public static <T> Filter<T> trueFilter() {
        return new Filter<T>() {
            public boolean matches(T t) {
                return true;
            }
        };
    }

    public static <T> Filter<T> falseFilter() {
        return new Filter<T>() {
            public boolean matches(T t) {
                return false;
            }
        };
    }
}
