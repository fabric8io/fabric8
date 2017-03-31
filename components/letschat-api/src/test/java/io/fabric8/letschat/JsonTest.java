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

import io.fabric8.utils.jaxrs.JsonHelper;
import org.junit.Test;

/**
 */
public class JsonTest {
    @Test
    public void testRoom() throws Exception {
        RoomDTO dto = new RoomDTO();
        dto.setName("foo");
        dto.setDescription("some description");
        dto.setId("abcd");
        dto.setOwner("someUUID");

        String json = JsonHelper.toJson(dto);
        System.out.println("DTO " + dto + " is json: " + json);
    }

}
