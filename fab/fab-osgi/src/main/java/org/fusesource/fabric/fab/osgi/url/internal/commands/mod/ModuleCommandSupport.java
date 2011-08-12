/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.fab.osgi.url.internal.commands.mod;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.fusesource.fabric.fab.ModuleRegistry;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public abstract class ModuleCommandSupport extends OsgiCommandSupport {

    protected void println(){
        session.getConsole().println();
    }

    protected void println(String msg, Object ...args){
        session.getConsole().println(String.format(msg, args));
    }

    static class Table {
        final String format;
        private final int[] col;
        final ArrayList<ArrayList<Object>> table = new ArrayList<ArrayList<Object>>();

        Table(String format, int...col) {
            this.format = format;
            this.col = col;
        }

        public void add(Object ... values) {
            if( values.length!= col.length) {
                throw new IllegalArgumentException("Expected "+col.length+" arguments");
            }
            table.add(new ArrayList<Object>(Arrays.asList(values)));
        }

        public void print(PrintStream out) {
            String fmt = format;
            for (int i = 0; i < col.length; i++) {
                String token = "{" + (i + 1) + "}";
                if(fmt.contains(token)) {
                    if( col[i]!=0 ) {
                        int len = Math.abs(col[i]);
                        for (ArrayList<Object> row : table) {
                            Object o = row.get(i);
                            if( o == null ) {
                                o = "";
                            }
                            String s = o.toString();
                            row.set(i, s);
                            len = Math.max(s.length(), len);
                        }
                        if( col[i] < 0 ) {
                            len *= -1;
                        }
                        fmt = fmt.replaceAll(Pattern.quote(token), "%"+len+"s");
                    } else {
                        fmt = fmt.replaceAll(Pattern.quote(token), "%s");
                    }
                }
            }
            for (ArrayList<Object> row : table) {
                out.println(String.format(fmt, row.toArray()));
            }
        }
    }
}