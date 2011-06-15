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

import java.util.HashMap;

/**
 *
 */
public class Utilities {

    private static HashMap<String, String> substitutions = new HashMap<String, String>();

    static {
        substitutions.put("Ubyte", "UByte");
        substitutions.put("Ushort", "UShort");
        substitutions.put("Uint", "UInt");
        substitutions.put("Ulong", "ULong");
        substitutions.put("Uuid", "UUID");
        substitutions.put("Sasl", "SASL");
        substitutions.put("Id", "ID");
        substitutions.put("Ieee", "IEEE");
        substitutions.put("Vbin", "VBIN");
        substitutions.put("Utf", "UTF");
        substitutions.put("ListList", "List");
        substitutions.put("MapMap", "Map");
        substitutions.put("ArrayArray", "Array");
        substitutions.put("Smalllong", "SmallLong");
        substitutions.put("Smallint", "SmallInt");
        substitutions.put("Smallulong", "SmallULong");
        substitutions.put("Smalluint", "SmallUInt");
        substitutions.put("TimestampMs", "TimestampMS");

    }

    public static String filterClassNames(String name) {
        for (String needle : substitutions.keySet()) {
            String substitute = substitutions.get(needle);
            name = name.replaceAll(needle, substitute);
        }
        return name;
    }

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
        return filterClassNames(rc);
    }
}
