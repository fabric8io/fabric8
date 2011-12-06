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
package org.fusesource.eca;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.util.IntrospectionSupport;

public class TestBlob {

    private long enqueueTime;
    private int queueDepth;
    private boolean testForFail;
    private Long testTime = new Long(System.currentTimeMillis());
    private String testString = "testString";
    private String correlationID;

    public int getQueueDepth() {
        return queueDepth;
    }

    public void setQueueDepth(int queueDepth) {
        this.queueDepth = queueDepth;
    }

    public long getEnqueueTime() {
        return enqueueTime;
    }

    public void setEnqueueTime(long enqueueTime) {
        this.enqueueTime = enqueueTime;
    }

    public boolean isTestForFail() {
        return testForFail;
    }

    public void setTestForFail(boolean testForFail) {
        this.testForFail = testForFail;
    }

    public Long getTestTime() {
        return testTime;
    }

    public void setTestTime(Long testTime) {
        this.testTime = testTime;
    }

    public String getTestString() {
        return testString;
    }

    public void setTestString(String testString) {
        this.testString = testString;
    }

    public String getCorrelationID() {
        return correlationID;
    }

    public void setCorrelationID(String correlationID) {
        this.correlationID = correlationID;
    }

    public String toString() {
        Map map = new HashMap();
        IntrospectionSupport.getProperties(this, map, "");
        return "TestBlob " + map.toString();
    }

}
