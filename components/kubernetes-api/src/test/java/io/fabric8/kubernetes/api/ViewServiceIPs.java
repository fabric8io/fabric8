/**
 * Copyright 2005-2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.kubernetes.api;

import java.util.Set;

/**
 * Views the endpoints for all services or the given service id and namespace
 */
public class ViewServiceIPs {
    public static void main(String... args) {
        System.out.println("Usage: [serviceName]");

        try {
            String service = null;
            if (args.length > 0) {
                service = args[0];
            }

            Set<String> addresses = KubernetesHelper.lookupServiceInDns(service);
            if (addresses != null) {
                System.out.println("addresses: " + addresses);
            } else {
                System.out.println("null addresses");
            }
        } catch (Exception e) {
            System.out.println("FAILED: " + e);
            e.printStackTrace();
        }
    }

}
