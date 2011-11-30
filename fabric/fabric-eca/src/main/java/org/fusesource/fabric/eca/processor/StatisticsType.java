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
package org.fusesource.fabric.eca.processor;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

enum StatisticsType {
    ALL, MEAN, GEOMETRIC_MEAN, MIN, MAX, VARIANCE, STDDEV, SKEWNESS, KUTOSIS, RATE, COUNT;

    private static final Map<String, StatisticsType> lookup = new HashMap<String, StatisticsType>();

    static {
        for (StatisticsType s : EnumSet.allOf(StatisticsType.class))
            lookup.put(s.name().toUpperCase(), s);
    }

    public static StatisticsType getType(String name) {
        StatisticsType result = null;
        if (name != null) {
            String key = name.trim().toUpperCase();
            result = lookup.get(key);
        }
        return result;
    }

}
