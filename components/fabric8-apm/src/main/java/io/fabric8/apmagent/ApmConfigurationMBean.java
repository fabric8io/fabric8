/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.apmagent;

public interface ApmConfigurationMBean {

    String getWhiteList();

    void setWhiteList(String whiteList);

    String getBlackList();

    void setBlackList(String blackList);

    void addToBlackList(String className);

    void addToWhiteList(String className);

    boolean isTrace();

    void setTrace(boolean trace);

    boolean isDebug();

    void setDebug(boolean debug);

    boolean isAsyncTransformation();

    void setAsyncTransformation(boolean asyncTransformation);

    int getThreadMetricDepth();

    void setThreadMetricDepth(int threadMetricDepth);

    int getMethodMetricDepth();

    void setMethodMetricDepth(int methodMetricDepth);

    String getStrategy();

    void setStrategy(String strategy);

}
