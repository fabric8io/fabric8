/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.util;

import java.util.Map;

/**
 * Some helper methods for working with maps
 */
public class Maps {

    /**
     * Copies the entries for the given keys form the input map to the output map
     */
    public static <K,V> void putAll(Map<K,V> output, Map<K,V> input, K... keys) {
        for (K key : keys) {
            V value = input.get(key);
            if (value != null) {
                output.put(key, value);
            }
        }
    }
}
