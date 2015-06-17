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
package io.fabric8.kubernetes.mbeans;

import io.fabric8.kubernetes.api.Kubernetes;
import io.fabric8.kubernetes.api.KubernetesFactory;
import io.fabric8.kubernetes.mbeans.KubernetesManager;

/**
 * Simple test program for looking up the port of a pod
 */
public class FindPort {
    public static void main(String... args) {
        KubernetesFactory kubeFactory = new KubernetesFactory();
        System.out.println("Connecting to kubernetes on: " + kubeFactory.getAddress());

        try {
            Kubernetes kube = kubeFactory.createKubernetes();

            if (args.length < 2) {
                System.out.println("Arguments: podId portNumberOrName");
                return;
            }
            String podName = args[0];
            String portNumberOrName = args[1];

            KubernetesManager manager = new KubernetesManager();
            String url = manager.getPodUrl(podName, portNumberOrName);
            if (url == null) {
                System.out.println("Could not find URL for pod: " + podName + " and port: " + portNumberOrName);
            } else {
                System.out.println("Found: " + url + " for pod: " + podName + " port: " + portNumberOrName);
            }
/*
            Pod pod = kube.getPod(podName);
            if (pod == null) {
                System.out.println("No such pod: " + podName);
            } else {
                Port port = KubernetesHelper.findContainerPortByNumberOrName(pod, portNumberOrName);
                System.out.println("Found pod: " + podName + " port: " + portNumberOrName + " found: " + port);
            }
*/
        } catch (Exception e) {
            System.out.println("FAILED: " + e);
            e.printStackTrace();
        }
    }
}
