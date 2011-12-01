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
package org.fusesource.eca.util;

import java.util.Map;

import junit.framework.TestCase;
import org.fusesource.eca.TestStat;

public class PropertyUtilTest extends TestCase {

    public void testGetValues() throws Exception {
        TestStat testStat = new TestStat();

        Map<String, Number> map = PropertyUtil.getValues(Number.class, testStat);
        assertEquals(3, map.size());
    }
}
