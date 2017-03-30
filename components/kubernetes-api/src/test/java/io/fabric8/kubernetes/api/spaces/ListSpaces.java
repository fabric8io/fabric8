/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.api.spaces;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.SortedSet;

/**
 */
public class ListSpaces {
    public static void main(String[] args) {
        String namespace = null;
        if (args.length > 0) {
            namespace = args[0];
        }

        KubernetesClient kubernetesClient = new DefaultKubernetesClient();
        Spaces spaces = Spaces.load(kubernetesClient, namespace);
        SortedSet<Space> set = spaces.getSpaceSet();
        for (Space space : set) {
            System.out.println("Space " + space.getName() + " = " + space);
        }
    }
}
