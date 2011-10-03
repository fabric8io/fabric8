/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 FuseSource Corporation, a Progress Software company. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (the "License").
 * You may not use this file except in compliance with the License. You can obtain
 * a copy of the License at http://www.opensource.org/licenses/CDDL-1.0.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at resources/META-INF/LICENSE.txt.
 *
 */
package org.fusesource.fabric.eca.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParsingUtil {

    /**
     * Read a string - and convert to a millisecond value
     */
    public static long getTimeAsMilliseconds(String text) {
        if (text != null && !text.isEmpty()) {
            Pattern p = Pattern.compile("^\\s*(\\d+)\\s*(b)?\\s*$", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(text);
            if (m.matches()) {
                return Long.parseLong(m.group(1));
            }
            p = Pattern.compile("^\\s*(\\d+)\\s*m(s|illiseconds)?\\s*$", Pattern.CASE_INSENSITIVE);
            m = p.matcher(text);
            if (m.matches()) {
                return Long.parseLong(m.group(1));
            }

            p = Pattern.compile("^\\s*(\\d+)\\s*s(ec|ecs|econds)?\\s*$", Pattern.CASE_INSENSITIVE);
            m = p.matcher(text);
            if (m.matches()) {
                return Long.parseLong(m.group(1)) * 1000;
            }

            p = Pattern.compile("^\\s*(\\d+)\\s*min(s|utes)?\\s*$", Pattern.CASE_INSENSITIVE);
            m = p.matcher(text);
            if (m.matches()) {
                return Long.parseLong(m.group(1)) * 1000 * 60;
            }

            p = Pattern.compile("^\\s*(\\d+)\\s*hour(s)?\\s*$", Pattern.CASE_INSENSITIVE);
            m = p.matcher(text);
            if (m.matches()) {
                return Long.parseLong(m.group(1)) * 1000 * 3600;
            }

            p = Pattern.compile("^\\s*(\\d+)\\s*hr(s)?\\s*$", Pattern.CASE_INSENSITIVE);
            m = p.matcher(text);
            if (m.matches()) {
                return Long.parseLong(m.group(1)) * 1000 * 3600;
            }
        }
        return 0l;
    }
}
