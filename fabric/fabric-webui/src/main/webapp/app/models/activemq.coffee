###
 Copyright 2010 Red Hat, Inc.

 Red Hat licenses this file to you under the Apache License, version
 2.0 (the "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 implied.  See the License for the specific language governing
 permissions and limitations under the License.
###

define [
  "frameworks",
], -> 

  class ActiveMQ extends FON.Model
    defaults:
      id: null
      name: null
      version: null
    
    connectors: FON.nested_collection("connectors")
    connections: FON.nested_collection("connections")
    network_connectors: FON.nested_collection("network_connectors")
    network_bridges: FON.nested_collection("network_bridges")
    topics: FON.nested_collection("topics")
    queues: FON.nested_collection("queues")
    durable_topic_subscribers: FON.nested_collection("durable_topic_subscribers")
    inactive_durable_topic_subscribers: FON.nested_collection("inactive_durable_topic_subscribers")

  ActiveMQ