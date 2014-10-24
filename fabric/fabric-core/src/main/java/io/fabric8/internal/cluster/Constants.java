/*
 * Copyright 2005-2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.internal.cluster;

import java.util.regex.Pattern;

public class Constants {

    static final String ZOOKEEPER_SERVER_PID = "io.fabric8.zookeeper.server";
    static final String ZOOKEEPER_SERVER_PROPERTIES = ZOOKEEPER_SERVER_PID + ".properties";
    static final String ZOOKEEPER_CLIENT_PORT_KEY = "clientPort";
    static final String ENSEMBLE_ID_FORMAT = "%04d";
    static final String CLUSTER_PROFILE_PREFIX = "fabric-ensemble-";
    static final String CLUSTER_PROFILE_FORMAT = CLUSTER_PROFILE_PREFIX + ENSEMBLE_ID_FORMAT;
    static final String MEMBER_PROFILE_FORMAT = CLUSTER_PROFILE_PREFIX + ENSEMBLE_ID_FORMAT + "-%d";
    static final String SERVER_ID_PREFIX = "server.";

    static final Pattern SERVER_ID_PATTERN = Pattern.compile(SERVER_ID_PREFIX + "([0-9]+)");
    static final Pattern SERVER_PORTS_PATTERN = Pattern.compile("[\\w.:{}$-_]+:(?<" + ZooKeeperPortType.PEER.name() + ">[0-9]+):(?<" + ZooKeeperPortType.ELECTION + ">[0-9]+)");

    static final String CLIENT_PORT = "clientPort";
    static final String CLIENT_PORT_ADDRESS = "clientPortAddress";
    static final String SERVER_ADDRESS_FORMAT = "%s:%d:%d";
    static final String SERVER_ADDRESS_FORMAT_PL= "${zk:%s/ip}:%d:%d";
    static final String CLIENT_ADDRESS_FORMAT = "%s:%d";
    static final String CLIENT_ADDRESS_FORMAT_PL = "${zk:%s/ip}:%d";
    static final String SERVER_BIND_ADDRESS_FORMAT = "${zk:%s/bindaddress}";
}
