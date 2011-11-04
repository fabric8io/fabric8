/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.zookeeper.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RegexSupport {

    public static String[] merge(File ignore, String[] regex) throws Exception {
        ArrayList<String> list = new ArrayList<String>();
        if (regex != null) {
            for(String r : regex) {
                list.add(r);
            }
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(ignore));
            String s = reader.readLine();
            while (s != null) {
                list.add(s);
                s = reader.readLine();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading from " + ignore + " : " + e);
        }

        String rc[] = new String[list.size()];
        list.toArray(rc);
        return rc;
    }

    public static boolean matches(List<Pattern> patterns, String value, boolean defaultOnEmpty) {
        if ( patterns.isEmpty() ) {
            return defaultOnEmpty;
        }
        boolean rc = false;
        for ( Pattern pattern : patterns ) {
            if ( pattern.matcher(value).matches() ) {
                rc = true;
            }
        }
        return rc;
    }

    public static List<Pattern> getPatterns(String[] regex) {
        List<Pattern> patterns = new ArrayList<Pattern>();
        if ( regex != null ) {
            for ( String p : regex ) {
                patterns.add(Pattern.compile(p));
            }
        }
        return patterns;
    }
}