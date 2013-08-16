/*
 * Copyright 2010 Red Hat, Inc.
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

package org.fusesource.fabric.camel.facade.mbean;

import java.util.List;
import java.util.Map;

/*
TODO disabled for now until we figure out how to work nicely with the updated camel 2.12 API

import org.apache.camel.fabric.FabricTracerEventMessage;
*/


/**
 *
 */
public interface CamelFabricTracerMBean {

    String getId();

    void setEnabled(boolean enabled);

    boolean isEnabled();

    void setQueueSize(int size);

    int getQueueSize();

    long getTraceCounter();

    void resetTraceCounter();

/*
TODO disabled for now until we figure out how to work nicely with the updated camel 2.12 API

    List<FabricTracerEventMessage> dumpTracedMessages(String nodeId);

    List<FabricTracerEventMessage> dumpAllTracedMessages();
*/

    String dumpAllTracedMessagesAsXml();

}
