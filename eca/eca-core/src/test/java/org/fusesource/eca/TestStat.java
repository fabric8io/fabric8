/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.eca;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.util.IntrospectionSupport;

public class TestStat {

    private long enqueueTime;
    private int queueDepth;
    private boolean testForFail;
    private Long testTime = new Long(System.currentTimeMillis());
    private String testString = "testString";
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String toString() {
        Map map = new HashMap();
        IntrospectionSupport.getProperties(this, map, "");
        return "TestStat " + map.toString();
    }

}
