package org.fusesource.fabric.zookeeper.commands;

import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

public class RegexSupport {

    public static boolean matches(java.util.List<Pattern> patterns, String value) {
        if ( patterns.isEmpty() ) {
            return true;
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