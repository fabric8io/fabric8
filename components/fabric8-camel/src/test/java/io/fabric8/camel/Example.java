/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.camel;

import org.apache.camel.api.management.mbean.ManagedCamelContextMBean;

/**
 */
public class Example {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Arguments: replicationContainerName namespace camelContextId");
            return;
        }
        String replicationContainerName = args[0];
        String namespace = args[1];
        String camelContextId = args[2];

        String contextLabel = "RC " + namespace + "/" + replicationContainerName + " camelContext: " + camelContextId;
        System.out.println("About to use: " + contextLabel);

        try {
            CamelClients camelClients = new CamelClients();
            CamelClient camelClient = camelClients.clientForReplicationController(replicationContainerName, namespace);
            ManagedCamelContextMBean camelContextMBean = camelClient.getCamelContextMBean(camelContextId);
            String routesXml = camelContextMBean.dumpRoutesAsXml();

            System.out.println(contextLabel + " has routes: " + routesXml);

        } catch (Exception e) {
            System.out.println("Caught: " + e);
            e.printStackTrace();
            Throwable t = e;
            for (int i = 0; i < 10; i++) {
                Throwable cause = t.getCause();
                if (cause != null && cause != t) {
                    t = cause;
                    System.out.println("Caused by: " + t);
                    t.printStackTrace();
                } else {
                    break;
                }
            }
        }
    }
}
