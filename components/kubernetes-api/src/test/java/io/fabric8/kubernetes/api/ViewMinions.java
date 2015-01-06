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
package io.fabric8.kubernetes.api;

import io.fabric8.kubernetes.api.model.Minion;
import io.fabric8.kubernetes.api.model.MinionList;

import java.util.List;

import static io.fabric8.utils.Lists.notNullList;

/**
 * Views the minions
 */
public class ViewMinions {
    public static void main(String... args) {
        KubernetesClient client = new KubernetesClient();

        System.out.println("Connecting to kubernetes on: " + client.getAddress());

        try {
            listMinions(client);
        } catch (Exception e) {
            System.out.println("FAILED: " + e);
            e.printStackTrace();
        }
    }

    protected static void listMinions(KubernetesClient client)  throws Exception {
        MinionList minionList = client.getMinions();
            if (minionList != null) {
                List<Minion> items = notNullList(minionList.getItems());
                for (Minion item : items) {
                    display(item);
                }
        }
    }

    protected static void display(Minion minion) {
        if (minion != null) {
            String id = minion.getId();
            System.out.println("Minion: " + id + " resources: " + minion.getResources());
        } else {
            System.out.println("null minion");
        }
    }

}
