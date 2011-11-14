/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.dosgi.util;

import java.util.Arrays;

/**
 * Helper class to hold common text/string manipulation methods.
 *  
 * @author chirino
 */
public class StringSupport {

    public static String indent(String value, int spaces) {
        if( value == null ) {
            return null;
        }
        String indent = fillString(spaces, ' ');
        return value.replaceAll("(\\r?\\n)", "$1"+indent);
    }

    public static String fillString(int count, char character) {
        char t[] = new char[count];
        Arrays.fill(t, character);
        return new String(t);
    }
    
}
