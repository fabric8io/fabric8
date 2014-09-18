/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.api;

public interface ZkDefs {

    String DEFAULT_VERSION = "1.0";
    String DEFAULT_PROFILE = "default";

    String LOCAL_IP = "localip";
    String LOCAL_HOSTNAME = "localhostname";
    String PUBLIC_IP = "publicip";
    String PUBLIC_HOSTNAME = "publichostname";
    String MANUAL_IP = "manualip";
    String RESOLVER = "resolver";

    String MINIMUM_PORT ="minimum.port";
    String MAXIMUM_PORT ="maximum.port";

    String GLOBAL_RESOLVER_PROPERTY = "global.resolver";
    String LOCAL_RESOLVER_PROPERTY = "local.resolver";
    String BIND_ADDRESS = "bind.address";
    String DEFAULT_RESOLVER = LOCAL_HOSTNAME;
    String[] VALID_RESOLVERS = new String[]{LOCAL_HOSTNAME,LOCAL_IP,PUBLIC_IP,PUBLIC_HOSTNAME, MANUAL_IP};

}
