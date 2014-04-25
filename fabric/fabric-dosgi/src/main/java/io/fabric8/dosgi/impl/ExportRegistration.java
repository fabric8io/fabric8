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
package io.fabric8.dosgi.impl;

import org.osgi.framework.ServiceReference;

public class ExportRegistration {

    final ServiceReference exportedService;
    final EndpointDescription exportedEndpoint;
    final String zooKeeperNode;
    boolean closed;

    public ExportRegistration(ServiceReference exportedService, EndpointDescription exportedEndpoint, String zooKeeperNode) {
        this.exportedService = exportedService;
        this.exportedEndpoint = exportedEndpoint;
        this.zooKeeperNode = zooKeeperNode;
    }

    public EndpointDescription getExportedEndpoint() {
        return closed ? null : exportedEndpoint;
    }

    public ServiceReference getExportedService() {
        return closed ? null : exportedService;
    }

    public String getZooKeeperNode() {
        return closed ? null : zooKeeperNode;
    }

    public void close() {
        closed = true;
    }

}
