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
package org.fusesource.insight.camel.trace;

import org.fusesource.insight.camel.base.SwitchableContainerStrategyMBean;
import org.fusesource.insight.camel.trace.TracerEventMessage;

import java.util.List;

/**
 *
 */
public interface TracerMBean extends SwitchableContainerStrategyMBean {

    void setQueueSize(int size);

    int getQueueSize();

    long getTraceCounter();

    void resetTraceCounter();

    List<TracerEventMessage> dumpTracedMessages(String nodeId);

    List<TracerEventMessage> dumpAllTracedMessages();

    String dumpAllTracedMessagesAsXml();

}
