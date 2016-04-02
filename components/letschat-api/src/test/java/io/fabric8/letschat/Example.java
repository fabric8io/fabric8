/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.letschat;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.List;

/**
 */
public class Example {
    public static void main(String[] args) {
        String roomName = "fabric8_default";
        if (args.length > 0) {
            roomName = args[0];
        }

        try {
            KubernetesClient kubernetes = new DefaultKubernetesClient();
            LetsChatClient letschat = LetsChatKubernetes.createLetsChat(kubernetes);

            System.out.println("Connecting to letschat on: " + letschat.getAddress());

            List<RoomDTO> rooms = letschat.getRooms();
            for (RoomDTO room : rooms) {
                System.out.println("Room " + room.getId() + " has slug: " + room.getSlug() + " name " + room.getName());
            }

            // looking up a room
            RoomDTO myRoom = letschat.getRoom(roomName);
            System.out.println("Found room: " + myRoom + " by slug: " + roomName);

            RoomDTO notExist = letschat.getRoom("does-not-exist");
            System.out.println("Found non existing room: " + notExist);

            // lets try lazily create a room if it doesn't exist
            RoomDTO newRoom = letschat.getOrCreateRoom("my_new_room_slug");
            System.out.println("Found/created room: " + newRoom);

        } catch (Exception e) {
            System.out.println("Caught: " + e);
            e.printStackTrace();
        }
    }
}
